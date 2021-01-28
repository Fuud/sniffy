package io.sniffy.socket;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.util.ExceptionUtil;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.sniffy.Threads.*;
import static org.junit.Assert.*;

public class SnifferSocketImplFactoryTest extends BaseSocketTest {

    private static class TestSocketImplFactory implements SocketImplFactory {

        private AtomicInteger invocationCounter = new AtomicInteger();

        @Override
        public SocketImpl createSocketImpl() {

            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

            boolean serverSocket = false;

            if (null != stackTrace) {
                for (StackTraceElement ste : stackTrace) {
                    if (ste.getClassName().startsWith("java.net.ServerSocket")) {
                        serverSocket = true;
                    }
                }
            }

            try {
                if (null != SnifferSocketImplFactory.defaultSocketImplClassConstructor) {
                    return SnifferSocketImplFactory.defaultSocketImplClassConstructor.newInstance();
                }
                if (null != SnifferSocketImplFactory.defaultSocketImplFactoryMethod) {
                    return (SocketImpl) SnifferSocketImplFactory.defaultSocketImplFactoryMethod.invoke(null, serverSocket);
                }
                return null;
            } catch (Exception e) {
                ExceptionUtil.throwException(e);
                return null;
            } finally {
                if (!serverSocket) {
                    invocationCounter.incrementAndGet();
                }
            }

        }

    }

    @Test
    public void testExistingFactory() throws IOException {

        TestSocketImplFactory testSocketImplFactory = new TestSocketImplFactory();

        SnifferSocketImplFactory.uninstall();
        Socket.setSocketImplFactory(testSocketImplFactory);

        SnifferSocketImplFactory.install();

        performSocketOperation();

        assertEquals(1, testSocketImplFactory.invocationCounter.intValue());

        SnifferSocketImplFactory.uninstall();

        performSocketOperation();

        assertEquals(2, testSocketImplFactory.invocationCounter.intValue());

        SnifferSocketImplFactory.install();

    }

    @Test
    public void testInstall() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        try (Spy<?> s = Sniffy.spy()) {

            performSocketOperation();

            AtomicReference<Throwable> throwableHolder = new AtomicReference<>();

            Thread thread = new Thread(() -> {
                try {
                    performSocketOperation();
                } catch (Throwable e) {
                    throwableHolder.set(e);
                }
            });
            thread.start();
            thread.join();

            assertNull(throwableHolder.get());

            // Current thread socket operations

            assertEquals(1, (long) s.getSocketOperations(CURRENT, true).entrySet().size());

            s.getSocketOperations(CURRENT, true).values().stream().findAny().ifPresent((socketStats) -> {
                assertEquals(REQUEST.length, socketStats.bytesUp.intValue());
                assertEquals(RESPONSE.length, socketStats.bytesDown.intValue());
            });

            // Other threads socket operations

            assertEquals(1, s.getSocketOperations(OTHERS, true).entrySet().stream().count());

            s.getSocketOperations(OTHERS, true).values().stream().findAny().ifPresent((socketStats) -> {
                assertEquals(REQUEST.length, socketStats.bytesUp.intValue());
                assertEquals(RESPONSE.length, socketStats.bytesDown.intValue());
            });

            // Any threads socket operations

            assertEquals(2, s.getSocketOperations(ANY, true).entrySet().stream().count());

            s.getSocketOperations(OTHERS, true).values().stream().forEach((socketStats) -> {
                assertEquals(REQUEST.length, socketStats.bytesUp.intValue());
                assertEquals(RESPONSE.length, socketStats.bytesDown.intValue());
            });

        }

    }

    @Test
    public void testUninstall() throws Exception {

        Sniffy.initialize();

        SnifferSocketImplFactory.uninstall();

        try (Spy<?> s = Sniffy.spy()) {

            performSocketOperation();

            assertTrue(s.getSocketOperations(CURRENT, true).isEmpty());

        }

        SnifferSocketImplFactory.install();

    }

}