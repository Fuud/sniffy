package com.github.bedrin.jdbc.sniffer.spock

import com.github.bedrin.jdbc.sniffer.Sniffer
import com.github.bedrin.jdbc.sniffer.WrongNumberOfQueriesError
import groovy.sql.Sql
import spock.lang.*

class QueryCounterSpec extends Specification {

    @Shared
    def sql = Sql.newInstance("sniffer:jdbc:h2:~/test", "sa", "sa")

    def spy = Sniffer.spy()

    def "Execute single query"() {
        when:
        sql.execute("SELECT 1 FROM DUAL")

        then:
        spy.verify(1)
    }

    @FailsWith(WrongNumberOfQueriesError)
    def "Execute single query - negative"() {
        when:
        sql.execute("SELECT 1 FROM DUAL")
        sql.execute("SELECT 1 FROM DUAL")

        then:
        spy.verify(1)
    }

}