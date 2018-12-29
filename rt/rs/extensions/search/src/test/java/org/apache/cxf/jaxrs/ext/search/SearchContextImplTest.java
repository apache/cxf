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
package org.apache.cxf.jaxrs.ext.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SearchContextImplTest {

    @Test
    public void testPlainQuery1() {
        Message m = new MessageImpl();
        m.put("search.use.plain.queries", true);
        m.put(Message.QUERY_STRING, "a=b");
        String exp = new SearchContextImpl(m).getSearchExpression();
        assertEquals("a==b", exp);
    }

    @Test
    public void testWrongQueryNoException() {
        Message m = new MessageImpl();
        m.put("search.block.search.exception", true);
        m.put(Message.QUERY_STRING, "_s=ab");
        assertNull(new SearchContextImpl(m).getCondition(Book.class));
    }

    @Test(expected = SearchParseException.class)
    public void testWrongQueryException() {
        Message m = new MessageImpl();
        m.put(Message.QUERY_STRING, "_s=ab");
        new SearchContextImpl(m).getCondition(Book.class);
    }

    @Test
    public void testPlainQuery2() {
        Message m = new MessageImpl();
        m.put("search.use.plain.queries", true);
        m.put(Message.QUERY_STRING, "a=b&a=b1");
        String exp = new SearchContextImpl(m).getSearchExpression();
        assertEquals("(a==b,a==b1)", exp);
    }

    @Test
    public void testPlainQuery3() {
        Message m = new MessageImpl();
        m.put("search.use.plain.queries", true);
        m.put(Message.QUERY_STRING, "a=b&c=d");
        String exp = new SearchContextImpl(m).getSearchExpression();
        assertEquals("(a==b;c==d)", exp);
    }

    @Test
    public void testPlainQuery4() {
        Message m = new MessageImpl();
        m.put("search.use.plain.queries", true);
        m.put(Message.QUERY_STRING, "a=b&a=b2&c=d&f=g");
        String exp = new SearchContextImpl(m).getSearchExpression();
        assertEquals("((a==b,a==b2);c==d;f==g)", exp);
    }

    @Test
    public void testPlainQuery5() {
        Message m = new MessageImpl();
        m.put("search.use.plain.queries", true);
        m.put(Message.QUERY_STRING, "aFrom=1&aTill=3");
        String exp = new SearchContextImpl(m).getSearchExpression();
        assertEquals("(a=ge=1;a=le=3)", exp);
    }



    @Test
    public void testFiqlSearchCondition() {
        doTestFiqlSearchCondition(
            SearchContextImpl.SEARCH_QUERY + "=" + "name==CXF%20Rocks;id=gt=123");
    }

    @Test
    public void testFiqlSearchConditionCustomQueryName() {
        Message m = new MessageImpl();
        m.put(SearchContextImpl.CUSTOM_SEARCH_QUERY_PARAM_NAME, "thequery");
        doTestFiqlSearchCondition(m,
            "thequery" + "=" + "name==CXF%20Rocks;id=gt=123");
    }

    @Test
    public void testFiqlSearchBean() {
        doTestFiqlSearchBean(
            SearchContextImpl.SEARCH_QUERY + "=" + "name==CXF%20Rocks;id=gt=123");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalConditionType() {
        SearchContext context = new SearchContextImpl(new MessageImpl());
        context.getCondition(String.class);
    }
    @Test
    public void testFiqlSearchConditionWithShortQuery() {
        doTestFiqlSearchCondition(
            SearchContextImpl.SHORT_SEARCH_QUERY + "=" + "name==CXF%20Rocks;id=gt=123");
    }

    @Test
    public void testFiqlSearchConditionWithNonFiqlQuery() {
        doTestFiqlSearchCondition(
            "_s=name==CXF%20Rocks;id=gt=123&a=b");
        doTestFiqlSearchCondition(
            "a=b&_s=name==CXF%20Rocks;id=gt=123");
        doTestFiqlSearchCondition(
            "a=b&_s=name==CXF%20Rocks;id=gt=123&c=d");
    }

    private void doTestFiqlSearchCondition(String queryString) {
        doTestFiqlSearchCondition(new MessageImpl(), queryString);
    }

    private void doTestFiqlSearchCondition(Message m, String queryString) {
        m.put(Message.QUERY_STRING, queryString);
        SearchContext context = new SearchContextImpl(m);
        SearchCondition<Book> sc = context.getCondition(Book.class);
        assertNotNull(sc);

        List<Book> books = new ArrayList<>();
        books.add(new Book("CXF is cool", 125L));
        books.add(new Book("CXF Rocks", 125L));

        List<Book> found = sc.findAll(books);
        assertEquals(1, found.size());
        assertEquals(new Book("CXF Rocks", 125L), found.get(0));
    }

    private void doTestFiqlSearchBean(String queryString) {
        Message m = new MessageImpl();
        m.put(Message.QUERY_STRING, queryString);
        SearchContext context = new SearchContextImpl(m);
        SearchCondition<SearchBean> sc = context.getCondition(SearchBean.class);
        assertNotNull(sc);

        List<SearchBean> beans = new ArrayList<>();
        SearchBean sb1 = new SearchBean();
        sb1.set("name", "CXF is cool");
        beans.add(sb1);
        SearchBean sb2 = new SearchBean();
        sb2.set("name", "CXF Rocks");
        sb2.set("id", "124");
        beans.add(sb2);

        List<SearchBean> found = sc.findAll(beans);
        assertEquals(1, found.size());
        assertEquals(sb2, found.get(0));

        assertTrue(sc instanceof AndSearchCondition);
        assertNull(sc.getStatement());
        List<SearchCondition<SearchBean>> scs = sc.getSearchConditions();
        assertEquals(2, scs.size());
        SearchCondition<SearchBean> sc1 = scs.get(0);
        assertEquals("name", sc1.getStatement().getProperty());
        SearchCondition<SearchBean> sc2 = scs.get(1);
        assertEquals("id", sc2.getStatement().getProperty());

        assertTrue("123".equals(sc1.getStatement().getValue())
                   && "CXF Rocks".equals(sc2.getStatement().getValue())
                   || "123".equals(sc2.getStatement().getValue())
                   && "CXF Rocks".equals(sc1.getStatement().getValue()));

    }

    @Test
    public void testPrimitiveStatementSearchBean() {
        Message m = new MessageImpl();
        m.put(Message.QUERY_STRING, "_s=name==CXF");
        SearchContext context = new SearchContextImpl(m);
        SearchCondition<SearchBean> sc = context.getCondition(SearchBean.class);
        assertNotNull(sc);

        PrimitiveStatement ps = sc.getStatement();
        assertNotNull(ps);

        assertEquals("name", ps.getProperty());
        assertEquals("CXF", ps.getValue());
        assertEquals(ConditionType.EQUALS, ps.getCondition());
        assertEquals(String.class, ps.getValueType());
    }

    @Test
    public void testPrimitiveStatementSearchBeanComlexName() {
        Message m = new MessageImpl();
        m.put(Message.QUERY_STRING, "_s=complex.name==CXF");
        SearchContext context = new SearchContextImpl(m);
        SearchCondition<SearchBean> sc = context.getCondition(SearchBean.class);
        assertNotNull(sc);

        PrimitiveStatement ps = sc.getStatement();
        assertNotNull(ps);

        assertEquals("complex.name", ps.getProperty());
        assertEquals("CXF", ps.getValue());
        assertEquals(ConditionType.EQUALS, ps.getCondition());
        assertEquals(String.class, ps.getValueType());
    }

    @Test
    public void testSingleEquals() {
        Message m = new MessageImpl();
        m.put(Message.QUERY_STRING, "_s=name=CXF");
        m.put("fiql.support.single.equals.operator", "true");
        SearchContext context = new SearchContextImpl(m);
        SearchCondition<SearchBean> sc = context.getCondition(SearchBean.class);
        assertNotNull(sc);

        PrimitiveStatement ps = sc.getStatement();
        assertNotNull(ps);

        assertEquals("name", ps.getProperty());
        assertEquals("CXF", ps.getValue());
        assertEquals(ConditionType.EQUALS, ps.getCondition());
        assertEquals(String.class, ps.getValueType());
    }

    @Test
    public void testIsMetCompositeObject() throws Exception {
        SearchCondition<TheBook> filter =
            new FiqlParser<TheBook>(TheBook.class,
                null,
                Collections.singletonMap("address", "address.street")).parse("address==Street1");

        TheBook b = new TheBook();
        b.setAddress(new TheOwnerAddress("Street1"));
        assertTrue(filter.isMet(b));

        b.setAddress(new TheOwnerAddress("Street2"));
        assertFalse(filter.isMet(b));
    }
    @Test
    public void testIsMetCompositeInterface() throws Exception {
        SearchCondition<TheBook> filter =
            new FiqlParser<TheBook>(TheBook.class,
                null,
                Collections.singletonMap("address", "addressInterface.street"))
                    .parse("address==Street1");

        TheBook b = new TheBook();
        b.setAddress(new TheOwnerAddress("Street1"));
        assertTrue(filter.isMet(b));

        b.setAddress(new TheOwnerAddress("Street2"));
        assertFalse(filter.isMet(b));
    }

    public static class TheBook {
        private TheOwnerAddressInterface address;

        public TheOwnerAddress getAddress() {
            return (TheOwnerAddress)address;
        }

        public void setAddress(TheOwnerAddress a) {
            this.address = a;
        }

        public TheOwnerAddressInterface getAddressInterface() {
            return address;
        }

        public void setAddressInterface(TheOwnerAddressInterface a) {
            this.address = a;
        }
    }
    public interface TheOwnerAddressInterface {
        String getStreet();
        void setStreet(String street);
    }
    public static class TheOwnerAddress implements TheOwnerAddressInterface {
        private String street;

        public TheOwnerAddress() {

        }
        public TheOwnerAddress(String s) {
            this.street = s;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }
    }
}