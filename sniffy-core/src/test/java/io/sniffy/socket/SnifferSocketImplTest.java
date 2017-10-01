package io.sniffy.socket;

import io.sniffy.registry.ConnectionsRegistry;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import ru.yandex.qatools.allure.annotations.Features;

import java.io.*;
import java.net.*;

import static java.net.SocketOptions.SO_RCVBUF;
import static java.net.SocketOptions.SO_SNDBUF;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.ignoreStubs;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SocketImpl.class, SnifferSocketImpl.class})
public class SnifferSocketImplTest {

    @Mock
    private SocketImpl delegate;

    private SnifferSocketImpl sniffySocket;

    @BeforeClass
    public static void initSnifferSocketImplStatic() {
        SnifferSocketImpl.defaultReceiveBufferSize = 64;
        SnifferSocketImpl.defaultSendBufferSize = 64;
    }

    @Before
    public void createSniffySocket() throws Exception {
        spy(SnifferSocketImpl.class);
        sniffySocket = spy(new SnifferSocketImpl(delegate));

        ConnectionsRegistry.INSTANCE.clear();
    }

    @Test
    public void testSendUrgentData() throws Exception {

        sniffySocket.sendUrgentData(1);

        // TODO: insert timeout here and to similar methods?

        verifyPrivate(delegate).invoke("sendUrgentData",1);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testSendUrgentDataThrowsIOException() throws Exception {

        IOException expected = new IOException();
        when(delegate, "sendUrgentData", anyInt()).thenThrow(expected);

        try {
            sniffySocket.sendUrgentData(1);
            fail();
        } catch (IOException actual) {
            assertEquals(expected, actual);
        }

        verifyPrivate(delegate).invoke("sendUrgentData",1);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testSendUrgentDataThrowsRuntimeException() throws Exception {

        RuntimeException expected = new RuntimeException();
        when(delegate, "sendUrgentData", anyInt()).thenThrow(expected);

        try {
            sniffySocket.sendUrgentData(1);
            fail();
        } catch (Exception actual) {
            assertEquals(expected, actual);
        }

        verifyPrivate(delegate).invoke("sendUrgentData",1);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testShutdownInput() throws Exception {

        sniffySocket.shutdownInput();

        verifyPrivate(delegate).invoke("shutdownInput");
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testShutdownInputThrowsIOException() throws Exception {

        IOException expected = new IOException();
        when(delegate, "shutdownInput").thenThrow(expected);

        try {
            sniffySocket.shutdownInput();
            fail();
        } catch (IOException actual) {
            assertEquals(expected, actual);
        }

        verifyPrivate(delegate).invoke("shutdownInput");
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testShutdownInputThrowsRuntimeException() throws Exception {

        RuntimeException expected = new RuntimeException();
        when(delegate, "shutdownInput").thenThrow(expected);

        try {
            sniffySocket.shutdownInput();
            fail();
        } catch (Exception actual) {
            assertEquals(expected, actual);
        }

        verifyPrivate(delegate).invoke("shutdownInput");
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testShutdownOutput() throws Exception {

        sniffySocket.shutdownOutput();

        verifyPrivate(delegate).invoke("shutdownOutput");
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testGetFileDescriptor() throws Exception {

        FileDescriptor expected = mock(FileDescriptor.class);

        when(delegate, "getFileDescriptor").thenReturn(expected);

        FileDescriptor actual = sniffySocket.getFileDescriptor();

        verifyPrivate(delegate).invoke("getFileDescriptor");
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testGetInetAddress() throws Exception {

        InetAddress expected = mock(InetAddress.class);

        when(delegate, "getInetAddress").thenReturn(expected);

        InetAddress actual = sniffySocket.getInetAddress();

        verifyPrivate(delegate).invoke("getInetAddress");
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testGetPort() throws Exception {

        int expected = 1;

        when(delegate, "getPort").thenReturn(expected);

        int actual = sniffySocket.getPort();

        verifyPrivate(delegate).invoke("getPort");
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testSupportsUrgentData() throws Exception {

        when(delegate, "supportsUrgentData").thenReturn(true);

        boolean actual = sniffySocket.supportsUrgentData();

        verifyPrivate(delegate).invoke("supportsUrgentData");
        verifyNoMoreInteractions(delegate);

        assertEquals(true, actual);

    }

    @Test
    public void testGetLocalPort() throws Exception {

        int expected = 42;

        when(delegate, "getLocalPort").thenReturn(expected);

        int actual = sniffySocket.getLocalPort();

        verifyPrivate(delegate).invoke("getLocalPort");
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testSetPerformancePreferences() throws Exception {

        sniffySocket.setPerformancePreferences(1, 2, 3);

        verifyPrivate(delegate).invoke("setPerformancePreferences", 1, 2, 3);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testCreate() throws Exception {

        sniffySocket.create(true);

        verifyPrivate(delegate).invoke("create", true);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testConnect() throws Exception {

        sniffySocket.connect("localhost", 123);

        verifyPrivate(delegate).invoke("connect", "localhost", 123);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    @Features({"issues/219"})
    public void testConnectWithDelay() throws Exception {

        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("localhost", 123, 10);

        doNothing().when(SnifferSocketImpl.class, "sleepImpl", anyInt());

        sniffySocket.connect("localhost", 123);

        verifyPrivate(SnifferSocketImpl.class).invoke("sleepImpl", eq(10));

        verifyPrivate(delegate).invoke("connect", "localhost", 123);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    @Features({"issues/219"})
    public void testConnectWithDelayException() throws Exception {

        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("localhost", 123, -10);

        doNothing().when(SnifferSocketImpl.class, "sleepImpl", anyInt());

        try {
            sniffySocket.connect("localhost", 123);
            fail();
        } catch (ConnectException e) {
            assertNotNull(e);
        }

        verifyPrivate(SnifferSocketImpl.class).invoke("sleepImpl", eq(10));

        verifyNoMoreInteractions(delegate);

    }

    @Test
    @Features({"issues/219"})
    public void testConnectWithDelayThreadSleeps() throws Exception {

        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("localhost", 123, 1000);

        Thread thread = new Thread(() -> {
            try {
                sniffySocket.connect("localhost", 123);
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });

        thread.start();
        Thread.sleep(500);

        assertEquals(Thread.State.TIMED_WAITING, thread.getState());

        thread.join(1000);

        verifyPrivate(delegate).invoke("connect", "localhost", 123);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testConnectInetAddress() throws Exception {

        InetAddress inetAddress = InetAddress.getLoopbackAddress();

        sniffySocket.connect(inetAddress, 123);

        verifyPrivate(delegate).invoke("connect", inetAddress, 123);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testConnectSocketAddress() throws Exception {

        SocketAddress socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 123);

        sniffySocket.connect(socketAddress, 123);

        verifyPrivate(delegate).invoke("connect", socketAddress, 123);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testBindInetAddress() throws Exception {

        InetAddress inetAddress = InetAddress.getLoopbackAddress();

        sniffySocket.bind(inetAddress, 123);

        verifyPrivate(delegate).invoke("bind", inetAddress, 123);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testListen() throws Exception {

        sniffySocket.listen(123);

        verifyPrivate(delegate).invoke("listen", 123);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testAccept() throws Exception {

        SocketImpl socketImpl = new SnifferSocketImpl(null);

        sniffySocket.accept(socketImpl);

        verifyPrivate(delegate).invoke("accept", socketImpl);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testGetInputStream() throws Exception {

        InputStream expected = new ByteArrayInputStream(new byte[]{1,2,3});

        when(delegate, "getInputStream").thenReturn(expected);

        InputStream actual = sniffySocket.getInputStream();

        verifyPrivate(delegate).invoke("getInputStream");
        verifyNoMoreInteractions(delegate);

        assertEquals(SnifferInputStream.class, actual.getClass());
        assertEquals(1, actual.read());

    }

    @Test
    public void testEstimateReceiveBufferNoRcvBufOption() throws Exception {

        InputStream expected = new ByteArrayInputStream(new byte[]{1,2,3});
        SnifferSocketImpl.defaultReceiveBufferSize = null;

        when(delegate, "getInputStream").thenReturn(expected);
        when(delegate, "getOption", SO_RCVBUF).thenReturn(null);

        InputStream actual = sniffySocket.getInputStream();

        verifyPrivate(delegate).invoke("getInputStream");
        verifyNoMoreInteractions(ignoreStubs(delegate));

        assertEquals(SnifferInputStream.class, actual.getClass());
        assertEquals(1, actual.read());

        assertEquals((Integer) 0, SnifferSocketImpl.defaultReceiveBufferSize);

    }

    @Test
    public void testEstimateReceiveBufferRcvBufOptionThrowsException() throws Exception {

        InputStream expected = new ByteArrayInputStream(new byte[]{1,2,3});
        SnifferSocketImpl.defaultReceiveBufferSize = null;

        when(delegate, "getInputStream").thenReturn(expected);
        when(delegate, "getOption", SO_RCVBUF).thenThrow(new SocketException());

        InputStream actual = sniffySocket.getInputStream();

        verifyPrivate(delegate).invoke("getInputStream");
        verifyNoMoreInteractions(ignoreStubs(delegate));

        assertEquals(SnifferInputStream.class, actual.getClass());
        assertEquals(1, actual.read());

        assertEquals((Integer) 0, SnifferSocketImpl.defaultReceiveBufferSize);

    }

    @Test
    public void testGetOutputStream() throws Exception {

        ByteArrayOutputStream expected = new ByteArrayOutputStream();

        when(delegate, "getOutputStream").thenReturn(expected);

        OutputStream actual = sniffySocket.getOutputStream();

        verifyPrivate(delegate).invoke("getOutputStream");
        verifyNoMoreInteractions(delegate);

        assertEquals(SnifferOutputStream.class, actual.getClass());
        actual.write(1);

        assertArrayEquals(new byte[]{1}, expected.toByteArray());

    }

    @Test
    public void testEstimateSendBufferNoSndBufOption() throws Exception {

        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        SnifferSocketImpl.defaultSendBufferSize = null;

        when(delegate, "getOutputStream").thenReturn(expected);
        when(delegate, "getOption", SocketOptions.SO_SNDBUF).thenReturn(null);

        OutputStream actual = sniffySocket.getOutputStream();

        verifyPrivate(delegate).invoke("getOutputStream");
        verifyNoMoreInteractions(ignoreStubs(delegate));

        assertEquals(SnifferOutputStream.class, actual.getClass());
        actual.write(3);

        assertEquals(3, (int) expected.toByteArray()[0]);

        assertEquals((Integer) 0, SnifferSocketImpl.defaultSendBufferSize);

    }

    @Test
    public void testEstimateSendBufferSndBufOptionThrowsException() throws Exception {

        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        SnifferSocketImpl.defaultSendBufferSize = null;

        when(delegate, "getOutputStream").thenReturn(expected);
        when(delegate, "getOption", SocketOptions.SO_SNDBUF).thenThrow(new SocketException());

        OutputStream actual = sniffySocket.getOutputStream();

        verifyPrivate(delegate).invoke("getOutputStream");
        verifyNoMoreInteractions(ignoreStubs(delegate));

        assertEquals(SnifferOutputStream.class, actual.getClass());
        actual.write(3);

        assertEquals(3, (int) expected.toByteArray()[0]);

        assertEquals((Integer) 0, SnifferSocketImpl.defaultSendBufferSize);

    }

    @Test
    public void testAvailable() throws Exception {

        int expected = 1;

        when(delegate, "available").thenReturn(expected);

        int actual = sniffySocket.available();

        verifyPrivate(delegate).invoke("available");
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testClose() throws Exception {

        sniffySocket.close();

        verifyPrivate(delegate).invoke("close");
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testSetOption() throws Exception {

        int optId = 1;
        Object option = new Object();

        sniffySocket.setOption(optId, option);

        verifyPrivate(delegate).invoke("setOption", optId, option);
        verifyNoMoreInteractions(delegate);

    }


    @Test
    public void testGetOption() throws Exception {

        Object expected = new Object();

        when(delegate, "getOption", 1).thenReturn(expected);

        Object actual = sniffySocket.getOption(1);

        verifyPrivate(delegate).invoke("getOption", 1);
        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    public void testToString() throws Exception {

        String expected = "expected";

        when(delegate, "toString").thenReturn(expected);

        String actual = sniffySocket.toString();

        verifyNoMoreInteractions(delegate);

        assertEquals(expected, actual);

    }

    @Test
    @Features({"issues/219"})
    public void testSetReceiveBufferSize() throws Exception {
        int backup = sniffySocket.receiveBufferSize;
        try {
            sniffySocket.setOption(SO_RCVBUF, 5);
            assertEquals(5, sniffySocket.receiveBufferSize);
        } finally {
            sniffySocket.receiveBufferSize = backup;
        }
    }

    @Test
    @Features({"issues/219"})
    public void testSetSendBufferSize() throws Exception {
        int backup = sniffySocket.sendBufferSize;
        try {
            sniffySocket.setOption(SO_SNDBUF, 9);
            assertEquals(9, sniffySocket.sendBufferSize);
        } finally {
            sniffySocket.sendBufferSize = backup;
        }
    }

}
