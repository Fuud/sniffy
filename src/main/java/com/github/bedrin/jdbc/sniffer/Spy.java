package com.github.bedrin.jdbc.sniffer;

import com.github.bedrin.jdbc.sniffer.sql.Query;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.github.bedrin.jdbc.sniffer.Sniffer.DEFAULT_THREAD_MATCHER;
import static com.github.bedrin.jdbc.sniffer.sql.Query.Type.ALL;
import static com.github.bedrin.jdbc.sniffer.util.ExceptionUtil.addSuppressed;
import static com.github.bedrin.jdbc.sniffer.util.ExceptionUtil.throwException;

/**
 * Spy holds a number of queries which were executed at some point of time and uses it as a base for further assertions
 * @see Sniffer#spy()
 * @see Sniffer#expect(int)
 * @since 2.0
 */
public class Spy<C extends Spy<C>> implements Closeable {

    private Counter initialCount;
    private Counter initialThreadLocalCount;

    private final List<String> executedSqls = new LinkedList<String>();
    private final WeakReference<Spy> selfReference;

    private boolean closed = false;
    private StackTraceElement[] closeStackTrace;

    synchronized void addExecutedSql(String sql) {
        executedSqls.add(sql);
    }

    synchronized void resetExecutedSqls() {
        executedSqls.clear();
    }

    Spy() {
        initNumberOfQueries();
        this.selfReference = Sniffer.registerSpy(this);
    }

    private void initNumberOfQueries() {
        this.initialCount = new Counter(Sniffer.COUNTER);
        this.initialThreadLocalCount = new Counter(Sniffer.THREAD_LOCAL_COUNTER.get());
    }

    private List<Expectation> expectations = new ArrayList<Expectation>();

    /**
     * Wrapper for {@link Sniffer#spy()} method; useful for chaining
     * @return a new {@link Spy} instance
     * @since 2.0
     */
    public Spy reset() {
        checkOpened();
        initNumberOfQueries();
        resetExecutedSqls();
        return self();
    }

    /**
     * @return number of SQL statements executed by current thread since some fixed moment of time
     * @since 2.0
     */
    public int executedStatements() {
        return executedStatements(DEFAULT_THREAD_MATCHER);
    }

    /**
     * @param threadMatcher chooses {@link Thread}s for calculating the number of executed queries
     * @return number of SQL statements executed since some fixed moment of time
     * @since 2.0
     */
    public int executedStatements(Threads threadMatcher) {
        return executedStatements(threadMatcher, Query.Type.ALL);
    }

    /**
     * @param threadMatcher chooses {@link Thread}s for calculating the number of executed queries
     * @return number of SQL statements executed since some fixed moment of time
     * @since 2.2
     */
    public int executedStatements(Threads threadMatcher, Query.Type queryType) {

        checkOpened();

        switch (threadMatcher) {
            case ANY:
                return Sniffer.COUNTER.executedStatements(queryType) - initialCount.executedStatements(queryType);
            case CURRENT:
                return Sniffer.THREAD_LOCAL_COUNTER.get().executedStatements(queryType) - initialThreadLocalCount.executedStatements(queryType);
            case OTHERS:
                return Sniffer.COUNTER.executedStatements(queryType) - Sniffer.THREAD_LOCAL_COUNTER.get().executedStatements(queryType)
                        - initialCount.executedStatements(queryType) + initialThreadLocalCount.executedStatements(queryType);
            default:
                throw new IllegalArgumentException(String.format("Unknown thread matcher %s", threadMatcher.getClass().getName()));
        }

    }

    // never methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments 0, 0, {@link Threads#CURRENT}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C expectNever() {
        checkOpened();
        return expectNever(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments 0, 0, {@code threads}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C expectNever(Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, 0, threadMatcher, ALL));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments 0, 0, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C expectNever(Query.Type queryType) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, 0, DEFAULT_THREAD_MATCHER, queryType));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments 0, 0, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expectNever(Threads threadMatcher, Query.Type queryType) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, 0, threadMatcher, queryType));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments 0, 0, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expectNever(Query.Type queryType, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, 0, threadMatcher, queryType));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments 0, 0, {@link Threads#CURRENT}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C verifyNever() throws WrongNumberOfQueriesError {
        checkOpened();
        return verifyNever(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments 0, 0, {@code threads}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C verifyNever(Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, 0, threadMatcher, ALL).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments 0, 0, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C verifyNever(Query.Type queryType) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, 0, DEFAULT_THREAD_MATCHER, queryType).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments 0, 0, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verifyNever(Threads threadMatcher, Query.Type queryType) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, 0, threadMatcher, queryType).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments 0, 0, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verifyNever(Query.Type queryType, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, 0, threadMatcher, queryType).validate();
        return self();
    }

    // atMostOnce methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments 0, 1, {@link Threads#CURRENT}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C expectAtMostOnce() {
        checkOpened();
        return expectAtMostOnce(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments 0, 1, {@code threads}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C expectAtMostOnce(Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, 1, threadMatcher, ALL));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments 0, 1, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C expectAtMostOnce(Query.Type queryType) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, 1, DEFAULT_THREAD_MATCHER, queryType));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments 0, 1, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expectAtMostOnce(Threads threadMatcher, Query.Type queryType) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, 1, threadMatcher, queryType));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments 0, 1, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expectAtMostOnce(Query.Type queryType, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, 1, threadMatcher, queryType));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments 0, 1, {@link Threads#CURRENT}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C verifyAtMostOnce() throws WrongNumberOfQueriesError {
        checkOpened();
        return verifyAtMostOnce(DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments 0, 1, {@code threads}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C verifyAtMostOnce(Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, 1, threadMatcher, ALL).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments 0, 1, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtMostOnce(Query.Type queryType) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, 1, DEFAULT_THREAD_MATCHER, queryType).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments 0, 1, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtMostOnce(Threads threadMatcher, Query.Type queryType) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, 1, threadMatcher, queryType).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments 0, 1, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtMostOnce(Query.Type queryType, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, 1, threadMatcher, queryType).validate();
        return self();
    }

    // atMost methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments 0, {@code allowedStatements}, {@link Threads#CURRENT}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C expectAtMost(int allowedStatements) {
        checkOpened();
        return expectAtMost(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments 0, {@code allowedStatements}, {@code threads}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C expectAtMost(int allowedStatements, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, allowedStatements, threadMatcher, ALL));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments 0, {@code allowedStatements}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C expectAtMost(int allowedStatements, Query.Type queryType) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, allowedStatements, DEFAULT_THREAD_MATCHER, queryType));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments 0, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expectAtMost(int allowedStatements, Threads threadMatcher, Query.Type queryType) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, allowedStatements, threadMatcher, queryType));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments 0, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expectAtMost(int allowedStatements, Query.Type queryType, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(0, allowedStatements, threadMatcher, queryType));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments 0, {@code allowedStatements}, {@link Threads#CURRENT}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C verifyAtMost(int allowedStatements) throws WrongNumberOfQueriesError {
        checkOpened();
        return verifyAtMost(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments 0, {@code allowedStatements}, {@code threads}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C verifyAtMost(int allowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, allowedStatements, threadMatcher, ALL).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments 0, {@code allowedStatements}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtMost(int allowedStatements, Query.Type queryType) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, allowedStatements, DEFAULT_THREAD_MATCHER, queryType).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments 0, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtMost(int allowedStatements, Threads threadMatcher, Query.Type queryType) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, allowedStatements, threadMatcher, queryType).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments 0, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtMost(int allowedStatements, Query.Type queryType, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(0, allowedStatements, threadMatcher, queryType).validate();
        return self();
    }

    // exact methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@link Threads#CURRENT}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C expect(int allowedStatements) {
        checkOpened();
        return expect(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C expect(int allowedStatements, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(allowedStatements, allowedStatements, threadMatcher, ALL));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C expect(int allowedStatements, Query.Type queryType) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(allowedStatements, allowedStatements, DEFAULT_THREAD_MATCHER, queryType));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expect(int allowedStatements, Threads threadMatcher, Query.Type queryType) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(allowedStatements, allowedStatements, threadMatcher, queryType));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expect(int allowedStatements, Query.Type queryType, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(allowedStatements, allowedStatements, threadMatcher, queryType));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@link Threads#CURRENT}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C verify(int allowedStatements) throws WrongNumberOfQueriesError {
        checkOpened();
        return verify(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C verify(int allowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(allowedStatements, allowedStatements, threadMatcher, ALL).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C verify(int allowedStatements, Query.Type queryType) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(allowedStatements, allowedStatements, DEFAULT_THREAD_MATCHER, queryType).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verify(int allowedStatements, Threads threadMatcher, Query.Type queryType) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(allowedStatements, allowedStatements, threadMatcher, queryType).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verify(int allowedStatements, Query.Type queryType, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(allowedStatements, allowedStatements, threadMatcher, queryType).validate();
        return self();
    }

    // atLeast methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@link Threads#CURRENT}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C expectAtLeast(int allowedStatements) {
        checkOpened();
        return expectAtLeast(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C expectAtLeast(int allowedStatements, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, threadMatcher, ALL));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C expectAtLeast(int allowedStatements, Query.Type queryType) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, DEFAULT_THREAD_MATCHER, queryType));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expectAtLeast(int allowedStatements, Threads threadMatcher, Query.Type queryType) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, threadMatcher, queryType));
        return self();
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C expectAtLeast(int allowedStatements, Query.Type queryType, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, threadMatcher, queryType));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@link Threads#CURRENT}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C verifyAtLeast(int allowedStatements) throws WrongNumberOfQueriesError {
        checkOpened();
        return verifyAtLeast(allowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C verifyAtLeast(int allowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, threadMatcher, ALL).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtLeast(int allowedStatements, Query.Type queryType) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, DEFAULT_THREAD_MATCHER, queryType).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtLeast(int allowedStatements, Threads threadMatcher, Query.Type queryType) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, threadMatcher, queryType).validate();
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query.Type)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    public C verifyAtLeast(int allowedStatements, Query.Type queryType, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(allowedStatements, Integer.MAX_VALUE, threadMatcher, queryType).validate();
        return self();
    }

    // between methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query.Type)} with arguments {@code minAllowedStatements}, {@code maxAllowedStatements}, {@link Threads#CURRENT}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements) {
        checkOpened();
        return expectBetween(minAllowedStatements, maxAllowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.0
     */
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher, ALL));
        return self();
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.2
     */
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Query.Type queryType) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, DEFAULT_THREAD_MATCHER, queryType));
        return self();
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.2
     */
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher, Query.Type queryType) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher, queryType));
        return self();
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.2
     */
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Query.Type queryType, Threads threadMatcher) {
        checkOpened();
        expectations.add(new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher, queryType));
        return self();
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads)} with arguments {@code minAllowedStatements}, {@link Threads#CURRENT}, {@link Query.Type#ALL}
     * @since 2.0
     */
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements) throws WrongNumberOfQueriesError {
        checkOpened();
        return verifyBetween(minAllowedStatements, maxAllowedStatements, DEFAULT_THREAD_MATCHER);
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.0
     */
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher, ALL).validate();
        return self();
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.2
     */
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Query.Type queryType) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, DEFAULT_THREAD_MATCHER, queryType).validate();
        return self();
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.2
     */
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher, Query.Type queryType) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher, queryType).validate();
        return self();
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.2
     */
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Query.Type queryType, Threads threadMatcher) throws WrongNumberOfQueriesError {
        checkOpened();
        new ThreadMatcherExpectation(minAllowedStatements, maxAllowedStatements, threadMatcher, queryType).validate();
        return self();
    }

    // end

    /**
     * Verifies all expectations added previously using {@code expect} methods family
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.0
     */
    public void verify() throws WrongNumberOfQueriesError {
        checkOpened();
        WrongNumberOfQueriesError assertionError = getWrongNumberOfQueriesError();
        if (null != assertionError) {
            throw assertionError;
        }
    }

    /**
     *
     * @return WrongNumberOfQueriesError or null if there are no errors
     * @since 2.1
     */
    public WrongNumberOfQueriesError getWrongNumberOfQueriesError() {
        checkOpened();
        WrongNumberOfQueriesError assertionError = null;
        Throwable currentException = null;
        for (Expectation expectation : expectations) {
            try {
                expectation.validate();
            } catch (WrongNumberOfQueriesError e) {
                if (null == assertionError) {
                    currentException = assertionError = e;
                } else {
                    currentException.initCause(e);
                    currentException = e;
                }
            }
        }
        return assertionError;
    }

    /**
     * Alias for {@link #verify()} method; it is useful for try-with-resource API:
     * <pre>
     * <code>
     *     {@literal @}Test
     *     public void testTryWithResourceApi() throws SQLException {
     *         final Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa");
     *         try (@SuppressWarnings("unused") Spy s = Sniffer.expectAtMostOnce();
     *              Statement statement = connection.createStatement()) {
     *             statement.execute("SELECT 1 FROM DUAL");
     *         }
     *     }
     * }
     * </code>
     * </pre>
     * @since 2.0
     */
    public void close() {
        checkOpened();
        try {
            verify();
        } finally {
            Sniffer.removeSpyReference(selfReference);
            closed = true;
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            closeStackTrace = new StackTraceElement[stackTrace.length - 1];
            System.arraycopy(stackTrace, 1, closeStackTrace, 0, stackTrace.length - 1);
        }
    }

    private void checkOpened() {
        if (closed) {
            throw new SpyClosedException("Spy is closed", closeStackTrace);
        }
    }

    /**
     * Executes the {@link Sniffer.Executable#execute()} method on provided argument and verifies the expectations
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.0
     */
    public C execute(Sniffer.Executable executable) {
        checkOpened();
        try {
            executable.execute();
        } catch (Throwable e) {
            throw verifyAndAddToException(e);
        }

        verify();
        return self();
    }

    /**
     * Executes the {@link Runnable#run()} method on provided argument and verifies the expectations
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.0
     */
    public C run(Runnable runnable) {
        checkOpened();
        try {
            runnable.run();
        } catch (Throwable e) {
            throw verifyAndAddToException(e);
        }

        verify();
        return self();
    }

    /**
     * Executes the {@link Callable#call()} method on provided argument and verifies the expectations
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.0
     */
    public <V> SpyWithValue<V> call(Callable<V> callable) {
        checkOpened();
        V result;

        try {
            result = callable.call();
        } catch (Throwable e) {
            throw verifyAndAddToException(e);
        }

        verify();
        return new SpyWithValue<V>(result);
    }

    private RuntimeException verifyAndAddToException(Throwable e) {
        try {
            verify();
        } catch (WrongNumberOfQueriesError ae) {
            if (!addSuppressed(e, ae)) {
                ae.printStackTrace();
            }
        }
        throwException(e);
        return new RuntimeException(e);
    }

    private interface Expectation {

        void validate() throws WrongNumberOfQueriesError;

    }

    private class ThreadMatcherExpectation implements Expectation {

        private final int minimumQueries;
        private final int maximumQueries;
        private final Threads threadMatcher;
        private final Query.Type queryType;

        public ThreadMatcherExpectation(int minimumQueries, int maximumQueries, Threads threadMatcher, Query.Type queryType) {
            this.minimumQueries = minimumQueries;
            this.maximumQueries = maximumQueries;
            this.threadMatcher = threadMatcher;
            this.queryType = queryType;
        }

        public void validate() throws WrongNumberOfQueriesError {

            int numQueries = executedStatements(threadMatcher, queryType);

            if (numQueries > maximumQueries || numQueries < minimumQueries) synchronized (Spy.this) {
                throw new WrongNumberOfQueriesError(threadMatcher, minimumQueries, maximumQueries, numQueries, executedSqls);
            }

        }

    }

    @SuppressWarnings("unchecked")
    private C self() {
        return (C) this;
    }

}
