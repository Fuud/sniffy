package com.github.bedrin.jdbc.sniffer.sql;

public class Query {

    protected final Type type;

    public Query(Type type) {
        this.type = type;
    }

    public enum Type {
        SELECT,
        INSERT,
        UPDATE,
        DELETE,
        OTHER
    }

    public static Query parse(String sql) {

        if (null == sql) return null;

        String normalized = sql.trim().toLowerCase();

        Type type;

        if (normalized.startsWith("select ") || normalized.startsWith("with ")) {
            type = Type.SELECT;
        } else if (normalized.startsWith("insert ")) {
            type = Type.INSERT;
        } else if (normalized.startsWith("update ")) {
            type = Type.UPDATE;
        } else if (normalized.startsWith("delete ")) {
            type = Type.DELETE;
        } else {
            type = Type.OTHER;
        }

        return new Query(type);
    }

}
