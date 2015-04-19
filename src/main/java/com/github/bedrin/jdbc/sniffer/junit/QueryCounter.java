package com.github.bedrin.jdbc.sniffer.junit;

import com.github.bedrin.jdbc.sniffer.Spy;
import com.github.bedrin.jdbc.sniffer.Sniffer;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Provides integration with JUnit. Add following field to your test class:
 * <pre>
 * <code>
 * {@literal @}Rule
 * public final QueryCounter queryCounter = new QueryCounter();
 * }
 * </code>
 * </pre>
 * @see Expectations
 * @see Expectation
 * @see NoQueriesAllowed
 * @since 1.3
 */
public class QueryCounter implements TestRule {

    @Override
    public Statement apply(Statement statement, Description description) {

        Expectations expectations = description.getAnnotation(Expectations.class);
        Expectation expectation = description.getAnnotation(Expectation.class);
        NoQueriesAllowed notAllowedQueries = description.getAnnotation(NoQueriesAllowed.class);

        // If no annotations present, check the test class and its superclasses
        for (Class<?> testClass = description.getTestClass();
             null == expectations && null == expectation && null == notAllowedQueries && !Object.class.equals(testClass);
                testClass = testClass.getSuperclass()) {
            expectations = testClass.getDeclaredAnnotation(Expectations.class);
            expectation = testClass.getDeclaredAnnotation(Expectation.class);
            notAllowedQueries = testClass.getDeclaredAnnotation(NoQueriesAllowed.class);
        }

        if (null != expectation && null != notAllowedQueries) {
            return new InvalidAnnotationsStatement(statement,
                    new IllegalArgumentException("Cannot specify @Expectation and @NotAllowedQueries on one test method")
            );
        } else if (null != expectations && null != notAllowedQueries) {
            return new InvalidAnnotationsStatement(statement,
                    new IllegalArgumentException("Cannot specify @Expectations and @NotAllowedQueries on one test method")
            );
        } else if (null != expectations || null != expectation) {

            List<Expectation> expectationList = new ArrayList<Expectation>();

            if (null != expectation) {
                expectationList.add(expectation);
            }

            if (null != expectations) {
                expectationList.addAll(Arrays.asList(expectations.value()));
            }

            for (Expectation expectation1 : expectationList) {
                if (expectation1.value() != -1) {
                    if (expectation1.atMost() != -1 || expectation1.atLeast() != -1) {
                        return new InvalidAnnotationsStatement(statement,
                                new IllegalArgumentException("Cannot specify value parameter together with atLeast or atMost parameters")
                        );
                    }
                }
            }

            return new SnifferStatement(statement, expectationList);

        } else if (null != notAllowedQueries) {
            Expectation annotation = NoQueriesAllowed.class.getAnnotation(Expectation.class);
            return new SnifferStatement(statement, Collections.singletonList(annotation));
        } else {
            return statement;
        }

    }

    private static class InvalidAnnotationsStatement extends Statement {

        private final Statement delegate;
        private final Throwable exception;

        public InvalidAnnotationsStatement(Statement delegate, Throwable exception) {
            this.delegate = delegate;
            this.exception = exception;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                delegate.evaluate();
            } finally {
                throw exception;
            }
        }

    }

    private static class SnifferStatement extends Statement {

        private final Statement delegate;
        private final List<Expectation> expectationList;

        public SnifferStatement(Statement delegate, List<Expectation> expectationList) {
            this.delegate = delegate;
            this.expectationList = expectationList;
        }

        @Override
        public void evaluate() throws Throwable {

            Spy spy = Sniffer.expect(expectationList);

            spy.execute(new Sniffer.Executable() {
                @Override
                public void execute() throws Throwable{
                    delegate.evaluate();
                }
            });

        }

    }

}
