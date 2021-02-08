package io.sniffy.socket;

import java.io.ByteArrayOutputStream;

/**
 * @since 3.1.10
 */
public class NetworkPacket implements Comparable<NetworkPacket> {

    private final boolean sent;
    private final long timestamp;
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public NetworkPacket(boolean sent, long timestamp, byte[] traffic, int off, int len) {
        this.sent = sent;
        this.timestamp = timestamp;
        this.baos.write(traffic, off, len);
    }

    public boolean combine(boolean sent, long timestamp, byte[] traffic, int off, int len, long maxDelay) {
        if (this.sent != sent) return false;
        if (timestamp - this.timestamp > maxDelay) return false;
        this.baos.write(traffic, off, len);
        return true;
    }

    public boolean combine(NetworkPacket that, long maxDelay) {
        if (this.sent != that.sent) return false;
        if (that.timestamp - this.timestamp > maxDelay) return false;
        byte[] bytes = that.getBytes();
        this.baos.write(bytes, 0, bytes.length);
        return true;
    }

    public boolean isSent() {
        return sent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getBytes() {
        return baos.toByteArray();
    }

    @Override
    public int compareTo(NetworkPacket that) {
        //noinspection UseCompareMethod
        return (timestamp < that.timestamp) ? -1 : ((timestamp == that.timestamp) ? 0 : 1);
    }

}
