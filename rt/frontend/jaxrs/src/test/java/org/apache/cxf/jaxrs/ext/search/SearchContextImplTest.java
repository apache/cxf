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
import java.util.List;

import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Assert;
import org.junit.Test;

public class SearchContextImplTest extends Assert {

    @Test
    public void testFiqlSearchCondition() {
        doTestFiqlSearchCondition(
            SearchContextImpl.SEARCH_QUERY + "=" + "name==CXF%20Rocks;id=gt=123");
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
        Message m = new MessageImpl();
        m.put(Message.QUERY_STRING, queryString);
        SearchContext context = new SearchContextImpl(m);
        SearchCondition<Book> sc = context.getCondition(Book.class);
        assertNotNull(sc);
        
        List<Book> books = new ArrayList<Book>();
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
        
        List<SearchBean> beans = new ArrayList<SearchBean>();
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
}
