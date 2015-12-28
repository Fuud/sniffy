package io.sniffy.servlet;

import io.sniffy.Constants;
import io.sniffy.Sniffer;
import io.sniffy.Spy;
import io.sniffy.Threads;
import io.sniffy.sql.StatementMetaData;
import io.sniffy.util.LruCache;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * HTTP Filter will capture the number of executed queries for given HTTP request and return it
 * as a 'X-Sql-Queries' header in response.
 *
 * It also can inject an icon with a number of executed queries to each HTML page
 * This feature is experimental and can be enabled using inject-html filter parameter
 *
 * Example of web.xml:
 * <pre>
 * {@code
 *   <filter>
 *        <filter-name>sniffer</filter-name>
 *        <filter-class>io.sniffy.servlet.SnifferFilter</filter-class>
 *        <init-param>
 *            <param-name>inject-html</param-name>
 *            <param-value>true</param-value>
 *        </init-param>
 *        <init-param>
 *            <param-name>enabled</param-name>
 *            <param-value>true</param-value>
 *        </init-param>
 *        <init-param>
 *            <param-name>exclude-pattern</param-name>
 *            <param-value>^/vets.html$</param-value>
 *        </init-param>
 *    </filter>
 *    <filter-mapping>
 *        <filter-name>sniffer</filter-name>
 *        <url-pattern>/*</url-pattern>
 *    </filter-mapping>
 * }
 * </pre>
 *
 * @since 2.3.0
 */
public class SnifferFilter implements Filter {

    public static final String HEADER_NUMBER_OF_QUERIES = "X-Sql-Queries";
    public static final String HEADER_REQUEST_DETAILS = "X-Request-Details";

    public static final String SNIFFER_URI_PREFIX =
            "/jdbcsniffer/" +
                    Constants.MAJOR_VERSION +
                    "." +
                    Constants.MINOR_VERSION +
                    "." +
                    Constants.PATCH_VERSION;

    public static final String JAVASCRIPT_URI = SNIFFER_URI_PREFIX + "/jdbcsniffer.min.js";
    public static final String REQUEST_URI_PREFIX = SNIFFER_URI_PREFIX + "/request/";

    protected boolean injectHtml = false;
    protected boolean enabled = true;
    protected Pattern excludePattern = null;

    // TODO: consider replacing with some concurrent collection instead
    protected Map<String, List<StatementMetaData>> cache = Collections.synchronizedMap(
            new LruCache<String, List<StatementMetaData>>(10000)
    );

    protected SnifferServlet snifferServlet;
    protected ServletContext servletContext;

    public void init(FilterConfig filterConfig) throws ServletException {
        String injectHtml = filterConfig.getInitParameter("inject-html");
        if (null != injectHtml) {
            this.injectHtml = Boolean.parseBoolean(injectHtml);
        }
        String enabled = filterConfig.getInitParameter("enabled");
        if (null != enabled) {
            this.enabled = Boolean.parseBoolean(enabled);
        }
        String excludePattern = filterConfig.getInitParameter("exclude-pattern");
        if (null != excludePattern) {
            this.excludePattern = Pattern.compile(excludePattern);
        }

        snifferServlet = new SnifferServlet(cache);
        snifferServlet.init(new FilterServletConfigAdapter(filterConfig, "jdbc-sniffer"));

        servletContext = filterConfig.getServletContext();

    }

    public void doFilter(final ServletRequest request, ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        boolean chainCalled = false;

        try {

            if (injectHtml) {
                snifferServlet.service(request, response);
                if (response.isCommitted()) return;
            }

            final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            final HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            final String contextPath = httpServletRequest.getContextPath();
            final String relativeUrl = httpServletRequest.getRequestURI().substring(contextPath.length());

            if (null != excludePattern && excludePattern.matcher(relativeUrl).matches()) {
                chainCalled = true;
                chain.doFilter(request, response);
                return;
            }

            final Spy<? extends Spy> spy = Sniffer.spy();
            final String requestId = UUID.randomUUID().toString();

            BufferedServletResponseWrapper responseWrapper = new BufferedServletResponseWrapper(
                    httpServletResponse,
                    new BufferedServletResponseListener() {

                        /**
                         * Flag indicating that current response looks like HTML and capable of injecting sniffer widget
                         */
                        private boolean isHtmlPage = false;

                        /**
                         * todo return flag indicating that sniffer wont modify the output stream
                         * @param wrapper
                         * @param buffer
                         * @throws IOException
                         */
                        @Override
                        public void onBeforeCommit(BufferedServletResponseWrapper wrapper, Buffer buffer) throws IOException {
                            wrapper.addIntHeader(HEADER_NUMBER_OF_QUERIES, spy.executedStatements(Threads.CURRENT));
                            wrapper.addHeader(HEADER_REQUEST_DETAILS, contextPath + REQUEST_URI_PREFIX + requestId);
                            if (injectHtml) {
                                String contentType = wrapper.getContentType();
                                String contentEncoding = wrapper.getContentEncoding();

                                String mimeTypeMagic = null == buffer ? null :
                                        URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(buffer.leadingBytes(16)));

                                if (null != buffer && null == contentEncoding && null != contentType && contentType.startsWith("text/html")
                                        && !"application/xml".equals(mimeTypeMagic)) {
                                    // adjust content length with the size of injected content
                                    int contentLength = wrapper.getContentLength();
                                    if (contentLength > 0) {
                                        wrapper.setContentLength(contentLength + maximumInjectSize(contextPath));
                                    }
                                    isHtmlPage = true;

                                    String characterEncoding = wrapper.getCharacterEncoding();
                                    if (null == characterEncoding) {
                                        characterEncoding = Charset.defaultCharset().name();
                                    }

                                    String snifferHeader = generateHeaderHtml(contextPath, requestId).toString();

                                    HtmlInjector htmlInjector = new HtmlInjector(buffer, characterEncoding);
                                    htmlInjector.injectAtTheBeginning(snifferHeader);

                                }
                            }
                        }

                        @Override
                        public void beforeClose(BufferedServletResponseWrapper wrapper, Buffer buffer) throws IOException {

                            List<StatementMetaData> executedStatements = spy.getExecutedStatements(Threads.CURRENT);
                            if (null != executedStatements && !executedStatements.isEmpty()) {
                                cache.put(requestId, executedStatements);
                            }

                            if (injectHtml && isHtmlPage) {

                                String characterEncoding = wrapper.getCharacterEncoding();
                                if (null == characterEncoding) {
                                    characterEncoding = Charset.defaultCharset().name();
                                }

                                String snifferWidget = generateAndPadFooterHtml(spy.executedStatements(Threads.CURRENT));

                                HtmlInjector htmlInjector = new HtmlInjector(buffer, characterEncoding);
                                htmlInjector.injectAtTheEnd(snifferWidget);

                            }

                        }

                    }
            );

            chainCalled = true;
            chain.doFilter(request, responseWrapper);

            List<StatementMetaData> executedStatements = spy.getExecutedStatements(Threads.CURRENT);
            if (null != executedStatements && !executedStatements.isEmpty()) {
                cache.put(requestId, executedStatements);
            }

            responseWrapper.close();

        } catch (Exception e) {
            servletContext.log("Exception in SnifferFilter", e);
            if (!chainCalled) {
                chain.doFilter(request, response);
            }
        }

    }

    protected StringBuilder generateHeaderHtml(String contextPath, String requestId) {
        return new StringBuilder().
                append("<script id=\"jdbc-sniffer-header\" type=\"application/javascript\" data-request-id=\"").
                append(requestId).
                append("\" src=\"").
                append(contextPath).
                append(JAVASCRIPT_URI).
                append("\"></script>");
        //return "<script type=\"application/javascript\" src=\"/mock/jdbcsniffer.min.js\"></script>";
    }

    private int maximumInjectSize;

    protected int maximumInjectSize(String contextPath) {
        if (maximumInjectSize == 0) {
            maximumInjectSize = maximumFooterSize() +
                    generateHeaderHtml(contextPath, UUID.randomUUID().toString()).length();
        }
        return maximumInjectSize;
    }

    private int maximumFooterSize() {
        return generateFooterHtml(Integer.MAX_VALUE).length();
    }

    protected String generateAndPadFooterHtml(int executedQueries) {
        StringBuilder sb = generateFooterHtml(executedQueries);
        for (int i = sb.length(); i < maximumFooterSize(); i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Generates following HTML snippet
     * <pre>
     * {@code
     * <div style="display:none!important" id="jdbc-sniffer" data-sql-queries="5" data-request-id="abcd"></div>
     * <script type="application-javascript" src="/petstore/jdbcsniffer.min.js"></script>
     * }
     * </pre>
     * @param executedQueries
     * @return
     */
    protected static StringBuilder generateFooterHtml(int executedQueries) {
        return new StringBuilder().
                append("<data id=\"jdbc-sniffer\" data-sql-queries=\"").append(executedQueries).append("\"/>");
    }

    public void destroy() {

    }

    public boolean isInjectHtml() {
        return injectHtml;
    }

    public void setInjectHtml(boolean injectHtml) {
        this.injectHtml = injectHtml;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
