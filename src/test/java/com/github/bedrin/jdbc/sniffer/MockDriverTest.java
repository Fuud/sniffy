package com.github.bedrin.jdbc.sniffer;

import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Enumeration;
import java.util.Properties;

import static org.junit.Assert.*;

public class MockDriverTest {

    @BeforeClass
    public static void loadDriver() throws ClassNotFoundException {
        Class.forName("com.github.bedrin.jdbc.sniffer.MockDriver");
    }

    @Test
    public void testRegisterDriver() {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        boolean found = false;
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver instanceof MockDriver) found = true;
        }
        assertTrue(found);
    }

    @Test
    public void testGetDriver() throws ClassNotFoundException, SQLException {
        Driver driver = DriverManager.getDriver("sniffer:jdbc:h2:~/test");
        assertNotNull(driver);
    }

    @Test
    public void testGetMockConnection() throws ClassNotFoundException, SQLException {
        try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa")) {
            assertNotNull(connection);
            assertTrue(Proxy.isProxyClass(connection.getClass()));
        }
    }

    @Test
    public void testGetMockConnectionRaisesException() throws ClassNotFoundException, SQLException {
        try {
            DriverManager.getConnection("sniffer:unknown:jdbc:url");
            fail();
        } catch (SQLException e) {
            assertNotNull(e);
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void testGetPropertyInfo() throws ClassNotFoundException, SQLException {
        Driver driver = DriverManager.getDriver("sniffer:jdbc:h2:~/test");
        DriverPropertyInfo[] propertyInfo = driver.getPropertyInfo("jdbc:h2:~/test", new Properties());
        assertNotNull(propertyInfo);
    }

    @Test
    public void testJdbcComplaint() throws ClassNotFoundException, SQLException {
        Driver driver = DriverManager.getDriver("sniffer:jdbc:h2:~/test");
        assertTrue(driver.jdbcCompliant());
    }

    @Test
    public void testExecuteStatement() throws ClassNotFoundException, SQLException {
        Sniffer.reset();
        try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1 FROM DUAL");
        }
        assertEquals(1, Sniffer.executedStatements());
        Sniffer.verifyNotMoreThanOne();
        Sniffer.verifyNotMore();
    }

    @Test
    public void testVersion() throws ClassNotFoundException, SQLException {
        Driver driver = DriverManager.getDriver("sniffer:jdbc:h2:~/test");
        assertEquals(Constants.MAJOR_VERSION, driver.getMajorVersion());
        assertEquals(Constants.MINOR_VERSION, driver.getMinorVersion());
    }

    @Test
    public void testGetParentLogger() throws ClassNotFoundException, SQLException {
        Driver driver = DriverManager.getDriver("sniffer:jdbc:h2:~/test");
        try {
            driver.getParentLogger();
            fail();
        } catch (SQLFeatureNotSupportedException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testExecuteQueryStatement() throws ClassNotFoundException, SQLException {
        Sniffer.reset();
        try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
             Statement statement = connection.createStatement()) {
            statement.executeQuery("SELECT 1 FROM DUAL");
        }
        assertEquals(1, Sniffer.executedStatements());
        Sniffer.verifyNotMoreThanOne();
        Sniffer.verifyNotMore();
    }

    @Test
    public void testExecutePreparedStatement() throws ClassNotFoundException, SQLException {
        Sniffer.reset();
        try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM DUAL")) {
            preparedStatement.execute();
        }
        assertEquals(1, Sniffer.executedStatements());
        Sniffer.verifyNotMoreThanOne();
        Sniffer.verifyNotMore();
    }

    @Test
    public void testExecuteQueryPreparedStatement() throws ClassNotFoundException, SQLException {
        Sniffer.reset();
        try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM DUAL")) {
            preparedStatement.executeQuery();
        }
        assertEquals(1, Sniffer.executedStatements());
        Sniffer.verifyNotMoreThanOne();
        Sniffer.verifyNotMore();
    }

    @Test
    public void testExecuteStatementThrowsException() throws ClassNotFoundException, SQLException {
        Sniffer.reset();
        try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1 FROM DUAL_HUAL");
        } catch (Exception e) {
            assertFalse(InvocationTargetException.class.isAssignableFrom(e.getClass()));
            assertTrue(SQLException.class.isAssignableFrom(e.getClass()));
        }
        assertEquals(1, Sniffer.executedStatements());
    }

    @SuppressWarnings("unused")
    public static int timesTwo(int arg) {
        return arg * 2;
    }

    @Test
    public void testCallStatement() throws ClassNotFoundException, SQLException {
        try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE ALIAS IF NOT EXISTS TIMES_TWO FOR \"com.github.bedrin.jdbc.sniffer.MockDriverTest.timesTwo\"");
            }

            Sniffer.reset();
            try (CallableStatement callableStatement = connection.prepareCall("CALL TIMES_TWO(?)")) {
                callableStatement.setInt(1, 1);
                callableStatement.execute();
            }
            assertEquals(1, Sniffer.executedStatements());
            Sniffer.verifyNotMoreThanOne();
            Sniffer.verifyNotMore();
        }
    }

}