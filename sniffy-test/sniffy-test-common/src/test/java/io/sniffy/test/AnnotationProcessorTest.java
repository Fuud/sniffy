package io.sniffy.test;

import io.sniffy.Expectation;
import io.sniffy.Expectations;
import io.sniffy.Query;
import io.sniffy.Threads;
import io.sniffy.sql.SqlExpectation;
import io.sniffy.sql.SqlStatement;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AnnotationProcessorTest {

    @Test
    @Expectation(threads = Threads.OTHERS, query = Query.INSERT, atLeast = 2, atMost = 5)
    public void testCorrectExpectationAnnotation() throws NoSuchMethodException {
        List<SqlExpectation> sqlExpectations = AnnotationProcessor.buildSqlExpectationList(
                AnnotationProcessorTest.class.getMethod("testCorrectExpectationAnnotation")
        );
        assertNotNull(sqlExpectations);
        assertEquals(1, sqlExpectations.size());

        SqlExpectation sqlExpectation = sqlExpectations.get(0);
        assertEquals(SqlStatement.INSERT, sqlExpectation.query());
        assertEquals(Threads.OTHERS, sqlExpectation.threads());
        assertEquals(2, sqlExpectation.count().min());
        assertEquals(5, sqlExpectation.count().max());
        assertEquals(-1, sqlExpectation.count().value());
        assertEquals(-1, sqlExpectation.rows().value());
        assertEquals(-1, sqlExpectation.rows().value());
        assertEquals(-1, sqlExpectation.rows().value());
    }

    @Test(expected = IllegalArgumentException.class)
    @Expectation(threads = Threads.ANY, query = Query.MERGE, atLeast = 2, atMost = 5, value = 3)
    public void testIncorrectExpectationAnnotation() throws NoSuchMethodException {
        AnnotationProcessor.buildSqlExpectationList(
                AnnotationProcessorTest.class.getMethod("testIncorrectExpectationAnnotation")
        );
    }

    @Test
    @Expectations({
        @Expectation(threads = Threads.ANY, query = Query.DELETE, atLeast = 2, atMost = 5),
        @Expectation(threads = Threads.CURRENT, query = Query.SELECT, atLeast = 3, atMost = 6)
    })
    public void testCorrectExpectationsAnnotation() throws NoSuchMethodException {
        List<SqlExpectation> sqlExpectations = AnnotationProcessor.buildSqlExpectationList(
                AnnotationProcessorTest.class.getMethod("testCorrectExpectationsAnnotation")
        );
        assertNotNull(sqlExpectations);
        assertEquals(2, sqlExpectations.size());

        {
            SqlExpectation sqlExpectation = sqlExpectations.get(0);
            assertEquals(SqlStatement.DELETE, sqlExpectation.query());
            assertEquals(Threads.ANY, sqlExpectation.threads());
            assertEquals(2, sqlExpectation.count().min());
            assertEquals(5, sqlExpectation.count().max());
            assertEquals(-1, sqlExpectation.count().value());
            assertEquals(-1, sqlExpectation.rows().value());
            assertEquals(-1, sqlExpectation.rows().value());
            assertEquals(-1, sqlExpectation.rows().value());
        }

        {
            SqlExpectation sqlExpectation = sqlExpectations.get(1);
            assertEquals(SqlStatement.SELECT, sqlExpectation.query());
            assertEquals(Threads.CURRENT, sqlExpectation.threads());
            assertEquals(3, sqlExpectation.count().min());
            assertEquals(6, sqlExpectation.count().max());
            assertEquals(-1, sqlExpectation.count().value());
            assertEquals(-1, sqlExpectation.rows().value());
            assertEquals(-1, sqlExpectation.rows().value());
            assertEquals(-1, sqlExpectation.rows().value());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    @Expectations(
        @Expectation(threads = Threads.ANY, query = Query.MERGE, atLeast = 2, atMost = 5, value = 3)
    )
    public void testIncorrectExpectationsAnnotation() throws NoSuchMethodException {
        AnnotationProcessor.buildSqlExpectationList(
                AnnotationProcessorTest.class.getMethod("testIncorrectExpectationsAnnotation")
        );
    }

}
