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
package org.apache.cxf.jaxrs.ext.search.sql;

import java.util.Collections;
import java.util.Date;

import org.apache.cxf.jaxrs.ext.search.FiqlParseException;
import org.apache.cxf.jaxrs.ext.search.FiqlParser;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;


public class SQLPrinterVisitorTest extends Assert {
    private FiqlParser<Condition> parser = new FiqlParser<Condition>(Condition.class);

    @Test
    public void testSQL1() throws FiqlParseException {
        SearchCondition<Condition> filter = parser.parse("name==ami*;level=gt=10");
        SQLPrinterVisitor<Condition> visitor = new SQLPrinterVisitor<Condition>("table");
        filter.accept(visitor);
        String sql = visitor.getResult();
        
        assertTrue("SELECT * FROM table WHERE (name LIKE 'ami%') AND (level > '10')".equals(sql)
                   || "SELECT * FROM table WHERE (level > '10') AND (name LIKE 'ami%')".equals(sql));
    }
    
    @Test
    public void testSQL1WithSearchBean() throws FiqlParseException {
        FiqlParser<SearchBean> beanParser = new FiqlParser<SearchBean>(SearchBean.class);
        SearchCondition<SearchBean> filter = beanParser.parse("name==ami*;level=gt=10");
        SQLPrinterVisitor<SearchBean> visitor = new SQLPrinterVisitor<SearchBean>("table");
        filter.accept(visitor);
        String sql = visitor.getResult();
        
        assertTrue("SELECT * FROM table WHERE (name LIKE 'ami%') AND (level > '10')".equals(sql)
                   || "SELECT * FROM table WHERE (level > '10') AND (name LIKE 'ami%')".equals(sql));
    }
    
    @Test
    public void testSQL2() throws FiqlParseException {
        SearchCondition<Condition> filter = parser.parse("name==ami*,level=gt=10");
        SQLPrinterVisitor<Condition> visitor = new SQLPrinterVisitor<Condition>("table");
        filter.accept(visitor);
        String sql = visitor.getResult();
        assertTrue("SELECT * FROM table WHERE (name LIKE 'ami%') OR (level > '10')".equals(sql)
                   || "SELECT * FROM table WHERE (level > '10') OR (name LIKE 'ami%')".equals(sql));
    }
    
    @Test
    public void testSQL3() throws FiqlParseException {
        SearchCondition<Condition> filter = parser.parse("name==foo*;(name!=*bar,level=gt=10)");
        SQLPrinterVisitor<Condition> visitor = new SQLPrinterVisitor<Condition>("table");
        filter.accept(visitor);
        String sql = visitor.getResult();
        assertTrue(("SELECT * FROM table WHERE (name LIKE 'foo%') AND ((name NOT LIKE '%bar') "
                   + "OR (level > '10'))").equals(sql)
                   || ("SELECT * FROM table WHERE (name LIKE 'foo%') AND "
                   + "((level > '10') OR (name NOT LIKE '%bar'))").equals(sql));
    }
    
    @Test
    public void testSQL3WithSearchBean() throws FiqlParseException {
        FiqlParser<SearchBean> beanParser = new FiqlParser<SearchBean>(SearchBean.class);
        SearchCondition<SearchBean> filter = beanParser.parse("name==foo*;(name!=*bar,level=gt=10)");
        SQLPrinterVisitor<SearchBean> visitor = new SQLPrinterVisitor<SearchBean>("table");
        filter.accept(visitor);
        String sql = visitor.getResult();
        assertTrue(("SELECT * FROM table WHERE (name LIKE 'foo%') AND ((name NOT LIKE '%bar') "
                   + "OR (level > '10'))").equals(sql)
                   || ("SELECT * FROM table WHERE (name LIKE 'foo%') AND "
                   + "((level > '10') OR (name NOT LIKE '%bar'))").equals(sql));
    }
    
    @Test
    public void testSQL4() throws FiqlParseException {
        SearchCondition<Condition> filter = parser.parse("(name==test,level==18);(name==test1,level!=19)");
        SQLPrinterVisitor<Condition> visitor = new SQLPrinterVisitor<Condition>("table");
        filter.accept(visitor);
        String sql = visitor.getResult();
        assertTrue(("SELECT * FROM table WHERE ((name = 'test') OR (level = '18'))"
                   + " AND ((name = 'test1') OR (level <> '19'))").equals(sql)
                   || ("SELECT * FROM table WHERE ((name = 'test1') OR (level <> '19'))"
                   + " AND ((name = 'test') OR (level = '18'))").equals(sql));
    }
    
    @Test
    public void testSQL5() throws FiqlParseException {
        SearchCondition<Condition> filter = parser.parse("name==test");
        SQLPrinterVisitor<Condition> visitor = new SQLPrinterVisitor<Condition>("table");
        filter.accept(visitor);
        String sql = visitor.getResult();
        assertTrue("SELECT * FROM table WHERE name = 'test'".equals(sql));
    }
    
    @Test
    public void testSQL5WithColumns() throws FiqlParseException {
        SearchCondition<Condition> filter = parser.parse("name==test");
        SQLPrinterVisitor<Condition> visitor = 
            new SQLPrinterVisitor<Condition>("table", "NAMES");
        filter.accept(visitor);
        String sql = visitor.getResult();
        assertTrue("SELECT NAMES FROM table WHERE name = 'test'".equals(sql));
    }
    
    @Test
    public void testSQL5WithFieldMap() throws FiqlParseException {
        SearchCondition<Condition> filter = parser.parse("name==test");
        SQLPrinterVisitor<Condition> visitor = 
            new SQLPrinterVisitor<Condition>(
                Collections.singletonMap("name", "NAMES"),
                "table", "NAMES");
        filter.accept(visitor);
        String sql = visitor.getResult();
        assertTrue("SELECT NAMES FROM table WHERE NAMES = 'test'".equals(sql));
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
