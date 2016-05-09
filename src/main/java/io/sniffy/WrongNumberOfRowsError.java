package io.sniffy;

import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;

import java.util.*;

import static io.sniffy.Query.*;
import static io.sniffy.util.StringUtil.LINE_SEPARATOR;

/**
 * @since 2.0
 */
public class WrongNumberOfRowsError extends SniffyAssertionError {

    private final Threads threadMatcher;
    private final Query query;
    private final int minimumRows;
    private final int maximumRows;
    private final int numRows;
    private final Map<StatementMetaData, SqlStats> executedStatements;

    public WrongNumberOfRowsError(
            Threads threadMatcher, Query query,
            int minimumRows, int maximumQueries, int numRows,
            Map<StatementMetaData, SqlStats> executedStatements) {
        super(buildDetailMessage(threadMatcher, query, minimumRows, maximumQueries, numRows, executedStatements));
        this.threadMatcher = threadMatcher;
        this.query = query;
        this.minimumRows = minimumRows;
        this.maximumRows = maximumQueries;
        this.numRows = numRows;
        this.executedStatements = Collections.unmodifiableMap(executedStatements);
    }

    public Threads getThreadMatcher() {
        return threadMatcher;
    }

    public Query getQuery() {
        return query;
    }

    public int getMinimumRows() {
        return minimumRows;
    }

    public int getMaximumRows() {
        return maximumRows;
    }

    public int getNumRows() {
        return numRows;
    }

    /**
     * @since 2.3.1
     * @return
     */
    public Collection<StatementMetaData> getExecutedStatements() {
        return executedStatements.keySet();
    }

    public List<String> getExecutedSqls() {
        List<String> executedSqls = new ArrayList<String>(executedStatements.size());
        for (StatementMetaData statement : executedStatements.keySet()) {
            executedSqls.add(statement.sql);
        }
        return executedSqls;
    }

    private static String buildDetailMessage(
            Threads threadMatcher, Query query,
            int minimumQueries, int maximumQueries, int numQueries,
            Map<StatementMetaData, SqlStats> executedStatements) {
        StringBuilder sb = new StringBuilder();
        sb.append("Expected between ").append(minimumQueries).append(" and ").append(maximumQueries);
        if (Threads.CURRENT == threadMatcher) {
            sb.append(" current thread");
        } else if (Threads.OTHERS == threadMatcher) {
            sb.append(" other threads");
        }
        sb.append(" rows ");
        if (SELECT == query) {
            sb.append("returned / affected");
        } else if (INSERT == query || UPDATE == query || MERGE == query) {
            sb.append("affected");
        } else {
            sb.append("returned / affected");
        }
        sb.append(LINE_SEPARATOR);
        sb.append("Observed ").append(numQueries).append(" rows instead:");
        if (null != executedStatements) for (Map.Entry<StatementMetaData, SqlStats> entry : executedStatements.entrySet()) {
            StatementMetaData statement = entry.getKey();
            SqlStats sqlStats = entry.getValue();
            if (Query.ANY == query || null == query || statement.query == query) {
                sb.append(statement.sql).append("; /* ").append(sqlStats.rows).append(" rows */").append(LINE_SEPARATOR);
            }
        }
        return sb.toString();
    }

}
