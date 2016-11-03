package io.sniffy.servlet;

import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.sniffy.registry.ConnectionsRegistry.ConnectionStatus.CLOSED;
import static io.sniffy.registry.ConnectionsRegistry.ConnectionStatus.OPEN;
import static org.junit.Assert.*;

public class SnifferServletTest {

    private MockServletContext servletContext = new MockServletContext();
    private MockFilterConfig filterConfig = new MockFilterConfig(servletContext, "sniffy");
    private ServletConfig servletConfig = new FilterServletConfigAdapter(filterConfig, "sniffy");

    private Map<String, RequestStats> cache;
    private SnifferServlet snifferServlet;

    @Before
    public void setupMocks() throws Exception {
        servletContext.setContextPath("/petclinic");
        cache = new HashMap<>();
        snifferServlet = new SnifferServlet(cache);
        snifferServlet.init(servletConfig);
    }

    @Test
    public void testGetJavascript() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic" + SnifferFilter.JAVASCRIPT_URI).
                buildRequest(servletContext);

        request.setContextPath("/petclinic");

        snifferServlet.service(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("application/javascript", response.getContentType());
        assertTrue(response.getContentLength() > 0);

    }

    @Test
    public void testGetSocketRegistry() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic" + SnifferServlet.CONNECTION_REGISTRY_URI_PREFIX).
                buildRequest(servletContext);

        request.setContextPath("/petclinic");

        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("localhost", 8181, OPEN);
        ConnectionsRegistry.INSTANCE.setDataSourceStatus("jdbc:h2:mem:", "sa", OPEN);

        snifferServlet.service(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("application/javascript", response.getContentType());
        assertTrue(response.getContentAsByteArray().length > 0);

        assertTrue(response.getContentAsString().contains("\"sockets\":"));
        assertTrue(response.getContentAsString().contains("\"dataSources\":"));

        ConnectionsRegistry.INSTANCE.clear();

    }

    @Test
    public void testEditDatasourceRegistry() throws Exception {

        ConnectionsRegistry.INSTANCE.clear();

        URI dataSource1URI = new URI("/petclinic" + SnifferServlet.DATASOURCE_REGISTRY_URI_PREFIX +
                URLEncoder.encode("jdbc:h2:mem:data/base", "UTF-8") + "/sa");

        {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = MockMvcRequestBuilders.post(dataSource1URI).buildRequest(servletContext);
            request.setContextPath("/petclinic");

            snifferServlet.service(request, response);

            assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());

            assertEquals(1, ConnectionsRegistry.INSTANCE.getDiscoveredDataSources().size());
            Map.Entry<String, String> datasource = ConnectionsRegistry.INSTANCE.getDiscoveredDataSources().keySet().iterator().next();
            assertEquals("jdbc:h2:mem:data/base", datasource.getKey());
            assertEquals("sa", datasource.getValue());
            assertEquals(OPEN, ConnectionsRegistry.INSTANCE.getDiscoveredDataSources().get(datasource));
        }

        // delete datasource from registry
        {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = MockMvcRequestBuilders.delete(dataSource1URI).buildRequest(servletContext);
            request.setContextPath("/petclinic");

            snifferServlet.service(request, response);

            assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());

            assertEquals(1, ConnectionsRegistry.INSTANCE.getDiscoveredDataSources().size());
            Map.Entry<String, String> datasource = ConnectionsRegistry.INSTANCE.getDiscoveredDataSources().keySet().iterator().next();
            assertEquals("jdbc:h2:mem:data/base", datasource.getKey());
            assertEquals("sa", datasource.getValue());
            assertEquals(CLOSED, ConnectionsRegistry.INSTANCE.getDiscoveredDataSources().get(datasource));
        }

        ConnectionsRegistry.INSTANCE.clear();

    }

    @Test
    public void testEditSocketRegistry() throws Exception {

        ConnectionsRegistry.INSTANCE.clear();

        URI dataSource1URI = new URI("/petclinic" + SnifferServlet.SOCKET_REGISTRY_URI_PREFIX +
                URLEncoder.encode("2001:0db8:85a3:0000:0000:8a2e:0370:7334", "UTF-8") + "/1234");

        {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = MockMvcRequestBuilders.post(dataSource1URI).buildRequest(servletContext);
            request.setContextPath("/petclinic");

            snifferServlet.service(request, response);

            assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());

            assertEquals(1, ConnectionsRegistry.INSTANCE.getDiscoveredAddresses().size());
            Map.Entry<String, Integer> datasource = ConnectionsRegistry.INSTANCE.getDiscoveredAddresses().keySet().iterator().next();
            assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", datasource.getKey());
            assertEquals(1234, datasource.getValue().intValue());
            assertEquals(OPEN, ConnectionsRegistry.INSTANCE.getDiscoveredAddresses().get(datasource));
        }

        // delete datasource from registry
        {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = MockMvcRequestBuilders.delete(dataSource1URI).buildRequest(servletContext);
            request.setContextPath("/petclinic");

            snifferServlet.service(request, response);

            assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());

            assertEquals(1, ConnectionsRegistry.INSTANCE.getDiscoveredAddresses().size());
            Map.Entry<String, Integer> datasource = ConnectionsRegistry.INSTANCE.getDiscoveredAddresses().keySet().iterator().next();
            assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", datasource.getKey());
            assertEquals(1234, datasource.getValue().intValue());
            assertEquals(CLOSED, ConnectionsRegistry.INSTANCE.getDiscoveredAddresses().get(datasource));
        }

        ConnectionsRegistry.INSTANCE.clear();

    }

    @Test
    public void testGetRequest() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic" + SnifferFilter.REQUEST_URI_PREFIX + "foo").
                buildRequest(servletContext);

        cache.put("foo", new RequestStats(21, 42, Collections.singletonMap(
                new StatementMetaData(
                        "SELECT 1 FROM DUAL",
                        StatementMetaData.guessQueryType("SELECT 1 FROM DUAL"),
                        "",
                        Thread.currentThread().getId()
                ), new SqlStats(300999, 0, 0, 0, 1))
        ));

        request.setContextPath("/petclinic");

        snifferServlet.service(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("application/javascript", response.getContentType());
        assertTrue(response.getContentLength() > 0);
        assertEquals("{\"timeToFirstByte\":21,\"time\":42,\"executedQueries\":[{\"query\":\"SELECT 1 FROM DUAL\",\"stackTrace\":\"\",\"time\":300.999,\"invocations\":1,\"rows\":0,\"type\":\"SELECT\",\"bytesDown\":0,\"bytesUp\":0}]}", response.getContentAsString());

    }

    @Test
    public void testGetRequestWithNetworkConnections() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic" + SnifferFilter.REQUEST_URI_PREFIX + "foo").
                buildRequest(servletContext);

        cache.put("foo", new RequestStats(
                        21,
                        42,
                        Collections.singletonMap(
                                new StatementMetaData(
                                        "SELECT 1 FROM DUAL",
                                        StatementMetaData.guessQueryType("SELECT 1 FROM DUAL"),
                                        "",
                                        Thread.currentThread().getId()
                                ), new SqlStats(300999, 200, 300, 0, 1)),
                        Collections.singletonMap(
                                new SocketMetaData(
                                        new InetSocketAddress(InetAddress.getLocalHost(), 5555),
                                        42,
                                        "stackTrace",
                                        Thread.currentThread().getId()
                                ),
                                new SocketStats(100, 200, 300)
                        )
                )
        );

        request.setContextPath("/petclinic");

        snifferServlet.service(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("application/javascript", response.getContentType());
        assertTrue(response.getContentLength() > 0);
        assertEquals("{\"timeToFirstByte\":21,\"time\":42," +
                "\"executedQueries\":[{\"query\":\"SELECT 1 FROM DUAL\",\"stackTrace\":\"\",\"time\":300.999,\"invocations\":1,\"rows\":0,\"type\":\"SELECT\",\"bytesDown\":200,\"bytesUp\":300}]," +
                "\"networkConnections\":[{\"host\":\"" + InetAddress.getLocalHost().toString() + ":5555\",\"stackTrace\":\"stackTrace\",\"time\":100.000,\"bytesDown\":200,\"bytesUp\":300}]" +
                "}", response.getContentAsString());

    }

    @Test
    public void testGetComplexRequest() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic" + SnifferFilter.REQUEST_URI_PREFIX + "foo").
                buildRequest(servletContext);

        cache.put("foo", new RequestStats(21, 42, Collections.singletonMap(
                new StatementMetaData(
                        "SELECT \r\n\"1\" FROM 'DUAL'",
                        StatementMetaData.guessQueryType("SELECT \r\n\"1\" FROM 'DUAL'"),
                        "io.sniffy.Test.method(Test.java:99)",
                        Thread.currentThread().getId()
                ), new SqlStats(300999, 0, 0, 0, 1))
        ));

        request.setContextPath("/petclinic");

        snifferServlet.service(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("application/javascript", response.getContentType());
        assertTrue(response.getContentLength() > 0);
        assertEquals("{\"timeToFirstByte\":21,\"time\":42,\"executedQueries\":[{\"query\":\"SELECT \\r\\n\\\"1\\\" FROM 'DUAL'\",\"stackTrace\":\"io.sniffy.Test.method(Test.java:99)\",\"time\":300.999,\"invocations\":1,\"rows\":0,\"type\":\"SELECT\",\"bytesDown\":0,\"bytesUp\":0}]}", response.getContentAsString());

    }

    @Test
    public void testGetRequestNotFound() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic" + SnifferFilter.REQUEST_URI_PREFIX + "foo").
                buildRequest(servletContext);

        request.setContextPath("/petclinic");

        snifferServlet.service(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals(0, response.getContentLength());
        assertEquals(0, response.getContentLength());

    }

    @Test
    public void testGetMissingResource() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                get("/petclinic/foobar").
                buildRequest(servletContext);

        snifferServlet.service(request, response);

        assertFalse(response.isCommitted());

    }

    @Test
    public void testPostMissingResource() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = MockMvcRequestBuilders.
                post("/petclinic/foobar").
                buildRequest(servletContext);

        snifferServlet.service(request, response);

        assertFalse(response.isCommitted());

    }

}