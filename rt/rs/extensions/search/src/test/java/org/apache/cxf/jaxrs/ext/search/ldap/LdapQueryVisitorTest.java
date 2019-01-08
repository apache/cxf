/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxrs.ext.search.ldap;

import java.util.Date;

import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchParseException;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class LdapQueryVisitorTest {
    private FiqlParser<Condition> parser = new FiqlParser<>(Condition.class);

    @Test
    public void testSimple() throws SearchParseException {
        SearchCondition<Condition> filter = parser.parse("name!=ami");
        LdapQueryVisitor<Condition> visitor = new LdapQueryVisitor<>();
        filter.accept(visitor.visitor());
        String ldap = visitor.getQuery();

        assertEquals("(!name=ami)", ldap);
    }

    @Test
    public void testAndQuery() throws SearchParseException {
        SearchCondition<Condition> filter = parser.parse("name==ami*;level=gt=10");
        LdapQueryVisitor<Condition> visitor = new LdapQueryVisitor<>();
        visitor.setEncodeQueryValues(false);
        filter.accept(visitor.visitor());
        String ldap = visitor.getQuery();

        assertEquals("(&(name=ami*)(level>=10))", ldap);
    }

    @Test
    public void testOrQuery() throws SearchParseException {
        SearchCondition<Condition> filter = parser.parse("name==ami*,level=gt=10");
        LdapQueryVisitor<Condition> visitor = new LdapQueryVisitor<>();
        visitor.setEncodeQueryValues(false);
        filter.accept(visitor.visitor());
        String ldap = visitor.getQuery();

        assertEquals("(|(name=ami*)(level>=10))", ldap);
    }

    @Test
    public void testAndOrQuery() throws SearchParseException {
        SearchCondition<Condition> filter =
            parser.parse("name==foo;(name!=bar,level=le=10)");
        LdapQueryVisitor<Condition> visitor = new LdapQueryVisitor<>();
        filter.accept(visitor.visitor());
        String ldap = visitor.getQuery();

        assertEquals("(&(name=foo)(|(!name=bar)(level<=10)))", ldap);
    }

    @Test
    public void testComplexQuery() throws SearchParseException {
        SearchCondition<Condition> filter =
            parser.parse("(name==test,level==18);(name==test1,level!=19)");
        LdapQueryVisitor<Condition> visitor = new LdapQueryVisitor<>();
        filter.accept(visitor.visitor());
        String ldap = visitor.getQuery();
        assertEquals("(&(|(name=test)(level=18))(|(name=test1)(!level=19)))", ldap);
    }


    @Ignore
    public static class Condition {
        private String name;
        private Integer level;
        private Date time;

        public Condition() {
        }

        public Condition(String name, Integer level, Date time) {
            this.name = name;
            this.level = level;
            this.time = time;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getLevel() {
            return level;
        }

        public void setLevel(Integer level) {
            this.level = level;
        }

        public Date getTime() {
            return time;
        }

        public void setTime(Date time) {
            this.time = time;
        }

        public void setException(Exception ex) {
            // do nothing
        }

    }
}