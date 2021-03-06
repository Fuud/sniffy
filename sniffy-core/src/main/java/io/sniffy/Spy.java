package io.sniffy;

import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.socket.*;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.StringUtil;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.Callable;

import static io.sniffy.util.ExceptionUtil.throwException;

/**
 * Spy holds a number of queries which were executed at some point of time and uses it as a base for further assertions
 *
 * @see Sniffy#spy()
 * @see Sniffy#expect(Expectation)
 * @since 2.0
 */
public class Spy<C extends Spy<C>> extends LegacySpy<C> implements Closeable {

    private final WeakReference<Spy> selfReference;

    private boolean closed = false;
    private StackTraceElement[] closeStackTrace;

    private List<Expectation> expectations = new ArrayList<Expectation>();

    /**
     * @since 3.1
     */
    @Override
    public Map<StatementMetaData, SqlStats> getExecutedStatements(ThreadMatcher threadMatcher, boolean removeStackTraces) {

        Map<StatementMetaData, SqlStats> executedStatements = new LinkedHashMap<StatementMetaData, SqlStats>();
        for (Map.Entry<StatementMetaData, SqlStats> entry : this.executedStatements.ascendingMap().entrySet()) {

            StatementMetaData statementMetaData = entry.getKey();

            if (removeStackTraces) statementMetaData = new StatementMetaData(
                    statementMetaData.sql, statementMetaData.query, null, statementMetaData.getThreadMetaData()
            );

            if (threadMatcher.matches(statementMetaData.getThreadMetaData())) {
                SqlStats existingSocketStats = executedStatements.get(statementMetaData);
                if (null == existingSocketStats) {
                    executedStatements.put(statementMetaData, new SqlStats(entry.getValue()));
                } else {
                    existingSocketStats.accumulate(entry.getValue());
                }
            }
        }

        return Collections.unmodifiableMap(executedStatements);
    }

    Spy() {
        this(SpyConfiguration.builder().build());
    }

    Spy(SpyConfiguration spyConfiguration) {
        super(spyConfiguration);
        selfReference = Sniffy.registerSpy(this);
    }

    /**
     * Wrapper for {@link Sniffy#spy()} method; useful for chaining
     *
     * @return a new {@link Spy} instance
     * @since 2.0
     */
    public C reset() {
        checkOpened();
        super.reset();
        expectations.clear();
        return self();
    }

    /**
     * @since 3.1
     */
    public Map<SocketMetaData, SocketStats> getSocketOperations(ThreadMatcher threadMatcher, String address, boolean removeStackTraces) {
        return getSocketOperations(threadMatcher, AddressMatchers.exactAddressMatcher(address), removeStackTraces);
    }

    /**
     * @since 3.1.10
     */
    public Map<SocketMetaData, SocketStats> getSocketOperations(ThreadMatcher threadMatcher, boolean removeStackTraces) {
        return getSocketOperations(threadMatcher, AddressMatchers.anyAddressMatcher(), removeStackTraces);
    }

    /**
     * @since 3.1.10
     */
    public Map<SocketMetaData, SocketStats> getSocketOperations(ThreadMatcher threadMatcher, AddressMatcher addressMatcher, boolean removeStackTraces) {

        Map<SocketMetaData, SocketStats> socketOperations = new LinkedHashMap<SocketMetaData, SocketStats>();
        for (Map.Entry<SocketMetaData, SocketStats> entry : this.socketOperations.ascendingMap().entrySet()) {
            SocketMetaData socketMetaData = entry.getKey();
            if (threadMatcher.matches(socketMetaData.getThreadMetaData()) && (null == addressMatcher || addressMatcher.matches(socketMetaData.getAddress()))) {
                if (removeStackTraces) socketMetaData = new SocketMetaData(
                        socketMetaData.getProtocol(), socketMetaData.address, socketMetaData.connectionId, null, socketMetaData.getThreadMetaData()
                );
                SocketStats existingSocketStats = socketOperations.get(socketMetaData);
                if (null == existingSocketStats) {
                    socketOperations.put(socketMetaData, new SocketStats(entry.getValue()));
                } else {
                    existingSocketStats.accumulate(entry.getValue());
                }
            }
        }

        return Collections.unmodifiableMap(socketOperations);

    }

    /**
     * @since 3.1.10
     */
    public Map<SocketMetaData, List<NetworkPacket>> getNetworkTraffic() {
        return getNetworkTraffic(Threads.ANY, AddressMatchers.anyAddressMatcher());
    }

    /**
     * @since 3.1.10
     */
    public Map<SocketMetaData, List<NetworkPacket>> getNetworkTraffic(ThreadMatcher threadMatcher, String address) {
        return getNetworkTraffic(threadMatcher, AddressMatchers.exactAddressMatcher(address));
    }

    /**
     * @since 3.1.10
     */
    public Map<SocketMetaData, List<NetworkPacket>> getNetworkTraffic(ThreadMatcher threadMatcher, String address, GroupingOptions groupingOptions) {
        return getNetworkTraffic(threadMatcher, AddressMatchers.exactAddressMatcher(address), groupingOptions);
    }

    /**
     * @since 3.1.10
     */
    public Map<SocketMetaData, List<NetworkPacket>> getNetworkTraffic(ThreadMatcher threadMatcher, AddressMatcher addressMatcher) {
        return getNetworkTraffic(threadMatcher, addressMatcher, GroupingOptions.builder().build());
    }

    /**
     * @since 3.1.10
     */
    public Map<SocketMetaData, List<NetworkPacket>> getNetworkTraffic(ThreadMatcher threadMatcher, AddressMatcher addressMatcher, GroupingOptions groupingOptions) {

        Map<SocketMetaData, List<NetworkPacket>> networkTraffic = new LinkedHashMap<SocketMetaData, List<NetworkPacket>>();
        for (Map.Entry<SocketMetaData, Deque<NetworkPacket>> entry : this.networkTraffic.ascendingMap().entrySet()) {
            SocketMetaData socketMetaData = entry.getKey();
            if (threadMatcher.matches(socketMetaData.getThreadMetaData()) && addressMatcher.matches(socketMetaData.getAddress())) {

                if (!groupingOptions.isGroupByConnection() || !groupingOptions.isGroupByStackTrace() || !groupingOptions.isGroupByThread()) {
                    SocketMetaData originalSocketMetaData = socketMetaData;
                    socketMetaData = new SocketMetaData(
                            originalSocketMetaData.getProtocol(), originalSocketMetaData.getAddress(),
                            !groupingOptions.isGroupByConnection() ? -1 : originalSocketMetaData.getConnectionId(),
                            !groupingOptions.isGroupByStackTrace() ? null : originalSocketMetaData.getStackTrace(),
                            !groupingOptions.isGroupByThread() ? null : originalSocketMetaData.getThreadMetaData()
                    );

                }

                List<NetworkPacket> networkPackets = networkTraffic.get(socketMetaData);
                //noinspection Java8MapApi
                if (null == networkPackets) {
                    networkPackets = new ArrayList<NetworkPacket>();
                    networkTraffic.put(socketMetaData, networkPackets);
                }
                networkPackets.addAll(entry.getValue());
            }
        }

        for (Map.Entry<SocketMetaData, List<NetworkPacket>> entry : networkTraffic.entrySet()) {
            List<NetworkPacket> networkPackets = entry.getValue();

            Collections.sort(networkPackets);

            List<NetworkPacket> reducedNetworkPackets = new ArrayList<NetworkPacket>();
            NetworkPacket lastNetworkPacket = null;

            for (NetworkPacket networkPacket : networkPackets) {
                if (!groupingOptions.isGroupByStackTrace() && null != networkPacket.getStackTrace()) {
                    byte[] bytes = networkPacket.getBytes();
                    networkPacket = new NetworkPacket(networkPacket.isSent(), networkPacket.getTimestamp(), null, bytes, 0, bytes.length);
                }
                if (null == lastNetworkPacket || !lastNetworkPacket.combine(networkPacket, SniffyConfiguration.INSTANCE.getPacketMergeThreshold())) {
                    reducedNetworkPackets.add(networkPacket);
                    lastNetworkPacket = networkPacket;
                }
            }

            entry.setValue(reducedNetworkPackets);

        }

        return networkTraffic;

    }

    // Expect and verify methods


    /**
     * @param expectation
     * @return
     * @since 3.1
     */
    @Override
    public C expect(Expectation expectation) {
        checkOpened();
        expectations.add(expectation);
        return self();
    }

    /**
     * @param expectation
     * @return
     * @since 3.1
     */
    @Override
    public C verify(Expectation expectation) {
        checkOpened();
        expectation.verify(this);
        return self();
    }

    /**
     * Verifies all expectations added previously using {@code expect} methods family
     *
     * @throws SniffyAssertionError if wrong number of queries was executed
     * @since 2.0
     */
    @Override
    public void verify() throws SniffyAssertionError {
        checkOpened();
        SniffyAssertionError assertionError = getSniffyAssertionError();
        if (null != assertionError) {
            throw assertionError;
        }
    }

    /**
     * @return SniffyAssertionError or null if there are no errors
     * @since 3.1
     */
    @Override
    public SniffyAssertionError getSniffyAssertionError() {
        checkOpened();
        SniffyAssertionError assertionError = null;
        Throwable currentException = null;
        for (Expectation expectation : expectations) {
            try {
                expectation.verify(this);
            } catch (SniffyAssertionError e) {
                if (null == assertionError) {
                    currentException = assertionError = e;
                } else {
                    currentException.initCause(e);
                    currentException = e;
                }
            }
        }
        return assertionError;
    }

    /**
     * Alias for {@link #verify()} method; it is useful for try-with-resource API:
     * <pre>
     * <code>
     *     {@literal @}Test
     *     public void testTryWithResourceApi() throws SQLException {
     *         final Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa");
     *         try (@SuppressWarnings("unused") Spy s = Sniffer.expectAtMostOnce();
     *              Statement statement = connection.createStatement()) {
     *             statement.execute("SELECT 1 FROM DUAL");
     *         }
     *     }
     * }
     * </code>
     * </pre>
     *
     * @since 2.0
     */
    public void close() {
        try {
            verify();
        } finally {
            Sniffy.removeSpyReference(selfReference);
            closed = true;
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            closeStackTrace = new StackTraceElement[stackTrace.length - 1];
            System.arraycopy(stackTrace, 1, closeStackTrace, 0, stackTrace.length - 1);
        }
    }

    @Override
    protected void checkOpened() {
        if (closed) {
            throw new SpyClosedException("Spy is closed", closeStackTrace);
        }
    }

    /**
     * Executes the {@link io.sniffy.Executable#execute()} method on provided argument and verifies the expectations
     *
     * @throws SniffyAssertionError if wrong number of queries was executed
     * @since 3.1
     */
    @Override
    public C execute(io.sniffy.Executable executable) throws SniffyAssertionError {
        checkOpened();
        try {
            executable.execute();
        } catch (Throwable e) {
            throw verifyAndAddToException(e);
        }

        verify();
        return self();
    }

    /**
     * Executes the {@link Runnable#run()} method on provided argument and verifies the expectations
     *
     * @throws SniffyAssertionError if wrong number of queries was executed
     * @since 2.0
     */
    public C run(Runnable runnable) throws SniffyAssertionError {
        checkOpened();
        try {
            runnable.run();
        } catch (Throwable e) {
            throw verifyAndAddToException(e);
        }

        verify();
        return self();
    }

    /**
     * Executes the {@link Callable#call()} method on provided argument and verifies the expectations
     *
     * @throws SniffyAssertionError if wrong number of queries was executed
     * @since 2.0
     */
    public <V> SpyWithValue<V> call(Callable<V> callable) throws SniffyAssertionError {
        checkOpened();
        V result;

        try {
            result = callable.call();
        } catch (Throwable e) {
            throw verifyAndAddToException(e);
        }

        verify();
        return new SpyWithValue<V>(result);
    }

    private RuntimeException verifyAndAddToException(Throwable e) {
        try {
            verify();
        } catch (SniffyAssertionError ae) {
            if (!ExceptionUtil.addSuppressed(e, ae)) {
                ae.printStackTrace();
            }
        }
        throwException(e);
        return new RuntimeException(e);
    }

    /**
     * @since 3.1
     */
    public interface Expectation {

        <T extends Spy<T>> Spy<T> verify(Spy<T> spy) throws SniffyAssertionError;

    }

    /**
     * @since 3.1
     */
    public static class SpyClosedException extends IllegalStateException {

        private final StackTraceElement[] closeStackTrace;

        public SpyClosedException(String s, StackTraceElement[] closeStackTrace) {
            super(ExceptionUtil.generateMessage(s + StringUtil.LINE_SEPARATOR + "Close stack trace:", closeStackTrace));
            this.closeStackTrace = closeStackTrace;
        }

        public StackTraceElement[] getCloseStackTrace() {
            return null == closeStackTrace ? null : closeStackTrace.clone();
        }

    }

    public static final class SpyWithValue<V> extends Spy<SpyWithValue<V>> {

        private final V value;

        SpyWithValue(V value) {
            this.value = value;
        }

        public V getValue() {
            return value;
        }

    }
}
