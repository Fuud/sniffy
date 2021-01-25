package io.sniffy.socket;

import io.sniffy.ThreadMetaData;

import java.net.InetSocketAddress;

/**
 * @since 3.1
 */
public class SocketMetaData {

    @Deprecated
    public final InetSocketAddress address;
    @Deprecated
    public final int connectionId;
    @Deprecated
    public final String stackTrace;
    @Deprecated
    public final long ownerThreadId;

    private final ThreadMetaData threadMetaData;

    private final int hashCode;

    public SocketMetaData(InetSocketAddress address, int connectionId, String stackTrace, Thread ownerThread) {
        this(address, connectionId, stackTrace, new ThreadMetaData(ownerThread));
    }

    public SocketMetaData(InetSocketAddress address, int connectionId, String stackTrace, ThreadMetaData threadMetaData) {
        this.address = address;
        this.connectionId = connectionId;
        this.stackTrace = null == stackTrace ? null : stackTrace.intern();
        this.threadMetaData = threadMetaData;
        this.ownerThreadId = threadMetaData.getThreadId();
        hashCode = computeHashCode();
    }

    private int computeHashCode() {
        int result = address.hashCode();
        result = 31 * result + connectionId;
        result = 31 * result + System.identityHashCode(stackTrace);
        result = 31 * result + threadMetaData.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SocketMetaData that = (SocketMetaData) o;

        if (connectionId != that.connectionId) return false;
        if (threadMetaData.getThreadId() != that.threadMetaData.getThreadId()) return false;
        if (!address.equals(that.address)) return false;
        //noinspection StringEquality
        return stackTrace == that.stackTrace;

    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public ThreadMetaData getThreadMetaData() {
        return threadMetaData;
    }
}
