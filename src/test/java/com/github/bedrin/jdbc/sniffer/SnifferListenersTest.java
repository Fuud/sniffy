package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import static org.junit.Assert.*;

public class SnifferListenersTest extends BaseTest {

    @Test
    public void testSpyRemovedOnClose() throws Exception {
        Spy spy = Sniffer.spy();
        spy.close();

        for (WeakReference<Spy> spyReference : Sniffer.registeredSpies()) {
            if (spyReference.get() == spy) fail("Spy was not removed from Sniffer observers");
        }

    }

    @Test
    public void testBatchIsLogged() throws Exception {
        try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa")) {
            connection.createStatement().execute("CREATE TEMPORARY TABLE TEMPORARY_TABLE (BAZ VARCHAR(255))");
            try (@SuppressWarnings("unused") Spy spy = Sniffer.expectNever();
                 PreparedStatement preparedStatement = connection.prepareStatement(
                         "INSERT INTO TEMPORARY_TABLE (BAZ) VALUES (?)")) {
                preparedStatement.setString(1, "foo");
                preparedStatement.addBatch();
                preparedStatement.setString(1, "bar");
                preparedStatement.addBatch();
                preparedStatement.executeBatch();
            }
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
            assertEquals(0, e.getMinimumQueries());
            assertEquals(0, e.getMaximumQueries());
            assertEquals(1, e.getNumQueries());
            assertEquals(1, e.getExecutedSqls().size());
            assertEquals("INSERT INTO TEMPORARY_TABLE (BAZ) VALUES (?) /*2 times*/", e.getExecutedSqls().get(0));
            assertEquals(Threads.CURRENT, e.getThreadMatcher());
            assertTrue(e.getMessage().contains("INSERT INTO TEMPORARY_TABLE (BAZ) VALUES (?) /*2 times*/"));
        }

    }

}