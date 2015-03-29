package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

public class SnifferExpectApiTest extends BaseTest {

    @Test(expected = AssertionError.class)
    public void testNotMoreThan() throws Exception {
        try (ExpectedQueries eq = Sniffer.expectNotMoreThan(1)) {
            executeStatements(2);
        }
    }

    @Test
    public void testNotMoreThanAllThreads() throws Exception {
        try (ExpectedQueries eq = Sniffer.expectNotMoreThanThreadLocal(1)) {
            executeStatementInOtherThread();
            executeStatement();
        }
    }

    @Test
    public void testNotMoreThanOtherThreads() throws Exception {
        try (ExpectedQueries eq = Sniffer.expectNotMoreThanOtherThreads(1)) {
            executeStatementInOtherThread();
            executeStatements(2);
        }
    }

}