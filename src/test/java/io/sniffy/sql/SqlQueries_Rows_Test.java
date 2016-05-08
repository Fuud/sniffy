package io.sniffy.sql;

import io.sniffy.*;
import org.junit.Test;

public class SqlQueries_Rows_Test extends BaseTest {

    @Test
    public void testNoneRows() {
        executeStatement(Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.noneRows())) {
            executeStatement(Query.DELETE);
        }
    }

    @Test
    public void testOneQueryNoRows() {
        executeStatement(Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.atMostOneQuery().delete().noneRows())) {
            executeStatement(Query.DELETE);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testTwoQueryMinTwoRows_Exception() {
        executeStatement(Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactQueries(2).minRows(2).delete().anyThreads())) {
            executeStatement(Query.DELETE);
            executeStatement(Query.DELETE);
        }
    }

    @Test
    public void testTwoQueryMaxTwoMergeRowsOtherThreads() {
        executeStatement(Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactQueries(2).maxRows(2).otherThreads().merge())) {
            executeStatementsInOtherThread(2, Query.MERGE);
        }
    }

    @Test
    public void testMinMaxRows() {
        try (Spy $= Sniffer.expect(SqlQueries.minRows(2).maxRows(3))) {
            executeStatement();
            executeStatement();
            executeStatement();
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testMinMaxRows_Exception() {
        try (Spy $= Sniffer.expect(SqlQueries.minRows(2).maxRows(3))) {
            executeStatement();
            executeStatement();
            executeStatement();
            executeStatement();
        }
    }

    @Test
    public void testMinMaxRowsOtherThreads() {
        try (Spy $= Sniffer.expect(SqlQueries.maxRows(3).otherThreads())) {
            executeStatements(4);
            executeStatementsInOtherThread(2);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testMinMaxRowsOtherThreads_Exception() {
        try (Spy $= Sniffer.expect(SqlQueries.minRows(5).otherThreads())) {
            executeStatements(6);
            executeStatementsInOtherThread(4);
        }
    }

    @Test
    public void testMaxRowsInserted() {
        try (Spy $= Sniffer.expect(SqlQueries.maxRows(3).insert())) {
            executeStatements(4, Query.SELECT);
            executeStatements(2, Query.INSERT);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testAtMostOneRowUpdated_Exception() {
        try (Spy $= Sniffer.expect(SqlQueries.atMostOneRow().update())) {
            executeStatements(2, Query.INSERT);
            executeStatements(2, Query.UPDATE);
            executeStatements(1, Query.DELETE);
        }
    }

    @Test
    public void testExactThreeRowsDeleteOtherThreads() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactRows(3).delete().otherThreads())) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(1, Query.DELETE);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testBetweenNineAndElevenRowSelectedAllQueries_Exception() {
        try (Spy $= Sniffer.expect(SqlQueries.rowsBetween(9,11).select().anyThreads())) {
            executeStatements(2, Query.INSERT);
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatements(1, Query.SELECT);
            executeStatementsInOtherThread(1, Query.SELECT);
        }
    }

    @Test
    public void testExactThreeRowsDeleteOtherThreadsMinMaxQueries() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactRows(3).delete().otherThreads().minQueries(5).maxQueries(7))) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(6, Query.DELETE);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testExactThreeRowsDeleteOtherThreadsMaxMinQueries_Exception() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactRows(3).otherThreads().delete().minQueries(5).maxQueries(7))) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(4, Query.DELETE);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test
    public void testExactQueriesAnyThreadsDeleteMaxMinRows() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactQueries(8).anyThreads().delete().maxRows(4).minRows(2))) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(6, Query.DELETE);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testExactQueriesAnyThreadsDeleteMinMaxRows_Exception() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactQueries(8).anyThreads().delete().minRows(1).maxRows(2))) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(6, Query.DELETE);
            executeStatements(2, Query.DELETE);
        }
    }

}
