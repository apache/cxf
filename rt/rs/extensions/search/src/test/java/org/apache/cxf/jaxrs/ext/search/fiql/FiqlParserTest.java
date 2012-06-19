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
package org.apache.cxf.jaxrs.ext.search.fiql;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeFactory;

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchParseException;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class FiqlParserTest extends Assert {
    private FiqlParser<Condition> parser = new FiqlParser<Condition>(Condition.class);

    @Test(expected = SearchParseException.class)
    public void testCompareWrongComparator() throws SearchParseException {
        parser.parse("name>booba");
    }

    @Test(expected = SearchParseException.class)
    public void testCompareMissingName() throws SearchParseException {
        parser.parse("==30");
    }

    @Test(expected = SearchParseException.class)
    public void testCompareMissingValue() throws SearchParseException {
        parser.parse("name=gt=");
    }

    @Test
    public void testCompareValueTextSpaces() throws SearchParseException {
        parser.parse("name=gt=some text");
    }

    @Test(expected = SearchParseException.class)
    public void testCompareNameTextSpaces() throws SearchParseException {
        parser.parse("some name=gt=text");
    }

    @Test(expected = SearchParseException.class)
    public void testDanglingOperator() throws SearchParseException {
        parser.parse("name==a;(level==10;),");
    }

    @Test
    public void testMultilevelExpression() throws SearchParseException {
        parser.parse("name==a;(level==10,(name!=b;name!=c;(level=gt=10)))");
    }

    @Test
    public void testMultilevelExpression2() throws SearchParseException {
        parser.parse("((name==a;level==10),name!=b;name!=c);level=gt=10");
    }

    @Test
    public void testRedundantBrackets() throws SearchParseException {
        parser.parse("name==a;((((level==10))))");
    }

    @Test
    public void testAndOfOrs() throws SearchParseException {
        parser.parse("(name==a,name==b);(level=gt=0,level=lt=10)");
    }

    @Test
    public void testOrOfAnds() throws SearchParseException {
        parser.parse("(name==a;name==b),(level=gt=0;level=lt=10)");
    }

    @Test(expected = SearchParseException.class)
    public void testUnmatchedBracket() throws SearchParseException {
        parser.parse("name==a;(name!=b;(level==10,(name!=b))");
    }

    @Test(expected = SearchParseException.class)
    public void testUnmatchedBracket2() throws SearchParseException {
        parser.parse("name==bbb;))()level==111");
    }

    @Test(expected = SearchParseException.class)
    public void testMissingComparison() throws SearchParseException {
        parser.parse("name==bbb;,level==111");
    }

    @Test(expected = SearchParseException.class)
    public void testSetterMissing() throws SearchParseException {
        parser.parse("noSuchSetter==xxx");
    }

    @Test(expected = SearchParseException.class)
    public void testSetterWrongType() throws SearchParseException {
        parser.parse("exception==text");
    }

    @Test
    public void testSetterNumericText() throws SearchParseException {
        parser.parse("name==10");
    }

    @Test
    public void testParseName() throws SearchParseException {
        SearchCondition<Condition> filter = parser.parse("name==king");
        assertTrue(filter.isMet(new Condition("king", 10, new Date())));
        assertTrue(filter.isMet(new Condition("king", 0, null)));
        assertFalse(filter.isMet(new Condition("diamond", 10, new Date())));
        assertFalse(filter.isMet(new Condition("diamond", 0, null)));
    }

    @Test
    public void testParseLevel() throws SearchParseException {
        SearchCondition<Condition> filter = parser.parse("level=gt=10");
        assertTrue(filter.isMet(new Condition("whatever", 15, new Date())));
        assertTrue(filter.isMet(new Condition(null, 15, null)));
        assertFalse(filter.isMet(new Condition("blah", 5, new Date())));
        assertFalse(filter.isMet(new Condition("foobar", 0, null)));
    }

    @Test
    public void testParseDateWithDefaultFormat() throws SearchParseException, ParseException {
        SearchCondition<Condition> filter = parser.parse("time=le=2010-03-11T18:00:00.000+00:00");
        DateFormat df = new SimpleDateFormat(FiqlParser.DEFAULT_DATE_FORMAT);
        assertTrue(filter.isMet(new Condition("whatever", 15, df.parse("2010-03-11T18:00:00.000+0000"))));
        assertTrue(filter.isMet(new Condition(null, null, df.parse("2010-03-10T22:22:00.000+0000"))));
        assertFalse(filter.isMet(new Condition("blah", null, df.parse("2010-03-12T00:00:00.000+0000"))));
        assertFalse(filter.isMet(new Condition(null, 123, df.parse("2010-03-12T00:00:00.000+0000"))));
    }

    @Test
    public void testParseDateWithCustomFormat() throws SearchParseException, ParseException {
        Map<String, String> props = new HashMap<String, String>();
        props.put(SearchUtils.DATE_FORMAT_PROPERTY, "yyyy-MM-dd'T'HH:mm:ss");
        props.put(SearchUtils.TIMEZONE_SUPPORT_PROPERTY, "false");
        parser = new FiqlParser<Condition>(Condition.class, props);
        
        SearchCondition<Condition> filter = parser.parse("time=le=2010-03-11T18:00:00");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        assertTrue(filter.isMet(new Condition("whatever", 15, df.parse("2010-03-11T18:00:00"))));
        assertTrue(filter.isMet(new Condition(null, null, df.parse("2010-03-10T22:22:00"))));
        assertFalse(filter.isMet(new Condition("blah", null, df.parse("2010-03-12T00:00:00"))));
        assertFalse(filter.isMet(new Condition(null, 123, df.parse("2010-03-12T00:00:00"))));
    }
    
    @Test
    public void testParseDateDuration() throws Exception {
        SearchCondition<Condition> filter = parser.parse("time=gt=-PT1M");
        Date now = new Date();
        Date tenMinutesAgo = new Date();
        DatatypeFactory.newInstance().newDuration("-PT10M").addTo(tenMinutesAgo);
        assertTrue(filter.isMet(new Condition(null, null, now)));
        assertFalse(filter.isMet(new Condition(null, null, tenMinutesAgo)));
    }

    @Test
    public void testParseComplex1() throws SearchParseException {
        SearchCondition<Condition> filter = parser.parse("name==ami*;level=gt=10");
        assertEquals(ConditionType.AND, filter.getConditionType());

        List<SearchCondition<Condition>> conditions = filter.getSearchConditions();
        assertEquals(2, conditions.size());
        PrimitiveStatement st1 = conditions.get(0).getStatement();
        PrimitiveStatement st2 = conditions.get(1).getStatement();
        assertTrue((ConditionType.EQUALS.equals(st1.getCondition()) 
            && ConditionType.GREATER_THAN.equals(st2.getCondition())) 
            || (ConditionType.EQUALS.equals(st2.getCondition()) 
                && ConditionType.GREATER_THAN.equals(st1.getCondition())));

        assertTrue(filter.isMet(new Condition("amichalec", 12, new Date())));
        assertTrue(filter.isMet(new Condition("ami", 12, new Date())));
        assertFalse(filter.isMet(new Condition("ami", 8, null)));
        assertFalse(filter.isMet(new Condition("am", 20, null)));
    }

    @Test
    public void testSQL1() throws SearchParseException {
        SearchCondition<Condition> filter = parser.parse("name==ami*;level=gt=10");
        String sql = SearchUtils.toSQL(filter, "table");
        assertTrue("SELECT * FROM table WHERE (name LIKE 'ami%') AND (level > '10')".equals(sql)
                   || "SELECT * FROM table WHERE (level > '10') AND (name LIKE 'ami%')".equals(sql));
    }

    @Test
    public void testParseComplex2() throws SearchParseException {
        SearchCondition<Condition> filter = parser.parse("name==ami*,level=gt=10");
        assertEquals(ConditionType.OR, filter.getConditionType());

        List<SearchCondition<Condition>> conditions = filter.getSearchConditions();
        assertEquals(2, conditions.size());

        PrimitiveStatement st1 = conditions.get(0).getStatement();
        PrimitiveStatement st2 = conditions.get(1).getStatement();
        assertTrue((ConditionType.EQUALS.equals(st1.getCondition()) 
            && ConditionType.GREATER_THAN.equals(st2.getCondition())) 
            || (ConditionType.EQUALS.equals(st2.getCondition()) 
                && ConditionType.GREATER_THAN.equals(st1.getCondition())));

        assertTrue(filter.isMet(new Condition("ami", 0, new Date())));
        assertTrue(filter.isMet(new Condition("foo", 20, null)));
        assertFalse(filter.isMet(new Condition("foo", 0, null)));
    }

    @Test
    public void testSQL2() throws SearchParseException {
        SearchCondition<Condition> filter = parser.parse("name==ami*,level=gt=10");
        String sql = SearchUtils.toSQL(filter, "table");
        assertTrue("SELECT * FROM table WHERE (name LIKE 'ami%') OR (level > '10')".equals(sql)
                   || "SELECT * FROM table WHERE (level > '10') OR (name LIKE 'ami%')".equals(sql));
    }

    @Test
    public void testParseComplex3() throws SearchParseException {
        SearchCondition<Condition> filter = parser.parse("name==foo*;(name!=*bar,level=gt=10)");
        assertTrue(filter.isMet(new Condition("fooooo", 0, null)));
        assertTrue(filter.isMet(new Condition("fooooobar", 20, null)));
        assertFalse(filter.isMet(new Condition("fooobar", 0, null)));
        assertFalse(filter.isMet(new Condition("bar", 20, null)));
    }

    @Test
    public void testSQL3() throws SearchParseException {
        SearchCondition<Condition> filter = parser.parse("name==foo*;(name!=*bar,level=gt=10)");
        String sql = SearchUtils.toSQL(filter, "table");
        assertTrue(("SELECT * FROM table WHERE (name LIKE 'foo%') AND ((name NOT LIKE '%bar') "
                    + "OR (level > '10'))").equals(sql)
                   || ("SELECT * FROM table WHERE (name LIKE 'foo%') AND "
                       + "((level > '10') OR (name NOT LIKE '%bar'))").equals(sql));
    }

    @Test
    public void testSQL4() throws SearchParseException {
        SearchCondition<Condition> filter = parser.parse("(name==test,level==18);(name==test1,level!=19)");
        String sql = SearchUtils.toSQL(filter, "table");
        assertTrue(("SELECT * FROM table WHERE ((name = 'test') OR (level = '18'))"
                    + " AND ((name = 'test1') OR (level <> '19'))").equals(sql)
                   || ("SELECT * FROM table WHERE ((name = 'test1') OR (level <> '19'))"
                       + " AND ((name = 'test') OR (level = '18'))").equals(sql));
    }

    @Test
    public void testSQL5() throws SearchParseException {
        SearchCondition<Condition> filter = parser.parse("name==test");
        String sql = SearchUtils.toSQL(filter, "table");
        assertTrue("SELECT * FROM table WHERE name = 'test'".equals(sql));
    }

    @Test
    public void testParseComplex4() throws SearchParseException {
        SearchCondition<Condition> filter = parser.parse("name==foo*;name!=*bar,level=gt=10");
        assertTrue(filter.isMet(new Condition("zonk", 20, null)));
        assertTrue(filter.isMet(new Condition("foobaz", 0, null)));
        assertTrue(filter.isMet(new Condition("foobar", 20, null)));
        assertFalse(filter.isMet(new Condition("fooxxxbar", 0, null)));
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

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
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
