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
package org.apache.cxf.jaxrs.ext.search.jpa;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Tuple;
import org.apache.cxf.jaxrs.ext.search.SearchConditionParser;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JPATypedQueryVisitorFiqlTest extends AbstractJPATypedQueryVisitorTest {

    @Test
    public void testOrQuery() throws Exception {
        List<Book> books = queryBooks("id=lt=10,id=gt=10");
        assertEquals(2, books.size());
        assertTrue(9 == books.get(0).getId() && 11 == books.get(1).getId()
            || 11 == books.get(0).getId() && 9 == books.get(1).getId());
    }

    @Test
    public void testOrQueryNoMatch() throws Exception {
        List<Book> books = queryBooks("id==7,id==5");
        assertEquals(0, books.size());
    }

    @Test
    public void testAndQuery() throws Exception {
        List<Book> books = queryBooks("id==10;bookTitle==num10");
        assertEquals(1, books.size());
        assertTrue(10 == books.get(0).getId() && "num10".equals(books.get(0).getBookTitle()));
    }


    @Test
    public void testGetLibraryBook() throws Exception {
        List<Book> books = queryBooks("library.books.bookTitle==num10");
        assertEquals(3, books.size());
    }

    @Test
    public void testQueryCollection() throws Exception {
        List<Book> books =
            queryBooks("reviews.authors==Ted");
        assertEquals(3, books.size());
    }

    @Test
    public void testQueryCollection2() throws Exception {
        List<Book> books =
            queryBooks("reviews.book.id==10");
        assertEquals(1, books.size());
    }

    @Test
    public void testQueryCollection3() throws Exception {
        List<Book> books =
            queryBooks("reviews.book.ownerInfo.name.name==Barry");
        assertEquals(1, books.size());
    }

    @Test
    public void testQueryElementCollection() throws Exception {
        List<Book> books =
            queryBooks("authors==John");
        assertEquals(2, books.size());
    }

    @Test
    public void testNumberOfReviews() throws Exception {
        List<Book> books =
            queryBooks("reviews=gt=0");
        assertEquals(3, books.size());
    }

    @Test
    public void testNumberOfReviews2() throws Exception {
        List<Book> books =
            queryBooks("reviews=gt=3");
        assertEquals(0, books.size());
    }

    @Test
    public void testNumberOfReviewAuthors() throws Exception {
        List<Book> books =
            queryBooks("count(reviews.authors)=gt=0");
        assertEquals(3, books.size());
    }

    @Test
    public void testNumberOfReviewAuthors2() throws Exception {
        List<Book> books =
            queryBooks("count(reviews.authors)=gt=3");
        assertEquals(0, books.size());
    }

    @Test
    public void testNumberOfAuthors() throws Exception {
        List<Book> books =
            queryBooks("count(authors)=gt=0");
        assertEquals(3, books.size());
    }

    @Test
    public void testNumberOfAuthors2() throws Exception {
        List<Book> books =
            queryBooks("count(authors)=gt=3");
        assertEquals(0, books.size());
    }

    @Test
    public void testQueryCollectionSize2() throws Exception {
        List<Book> books =
            queryBooks("reviews.authors=gt=0");
        assertEquals(3, books.size());
    }

    @Test
    public void testAndQueryCollection() throws Exception {
        List<Book> books =
            queryBooks("id==10;authors==John;reviews.review==good;reviews.authors==Ted");
        assertEquals(1, books.size());
        assertTrue(10 == books.get(0).getId() && "num10".equals(books.get(0).getBookTitle()));
    }

    @Test
    public void testAndQueryNoMatch() throws Exception {
        List<Book> books = queryBooks("id==10;bookTitle==num9");
        assertEquals(0, books.size());
    }

    @Test
    public void testEqualsQuery() throws Exception {
        List<Book> books = queryBooks("id==10");
        assertEquals(1, books.size());
        assertTrue(10 == books.get(0).getId());
    }

    @Test
    public void testEqualsCriteriaQueryTuple() throws Exception {
        List<Tuple> books = criteriaQueryBooksTuple("id==10");
        assertEquals(1, books.size());
        Tuple tuple = books.get(0);
        int tupleId = tuple.get("id", Integer.class);
        assertEquals(10, tupleId);
    }

    @Test
    public void testEqualsCriteriaQueryCount() throws Exception {
        assertEquals(1L, criteriaQueryBooksCount("id==10"));
    }


    @Test
    public void testEqualsCriteriaQueryConstruct() throws Exception {
        List<BookInfo> books = criteriaQueryBooksConstruct("id==10");
        assertEquals(1, books.size());
        BookInfo info = books.get(0);
        assertEquals(10, info.getId());
        assertEquals("num10", info.getTitle());
    }

    @Test
    public void testOrderByAsc() throws Exception {
        List<Book> books = criteriaQueryBooksOrderBy("reviews=gt=0", true);
        assertEquals(3, books.size());
        assertEquals(9, books.get(0).getId());
        assertEquals(10, books.get(1).getId());
        assertEquals(11, books.get(2).getId());
    }

    @Test
    public void testOrderByDesc() throws Exception {
        List<Book> books = criteriaQueryBooksOrderBy("reviews=gt=0", false);
        assertEquals(3, books.size());
        assertEquals(11, books.get(0).getId());
        assertEquals(10, books.get(1).getId());
        assertEquals(9, books.get(2).getId());
    }

    @Test
    public void testEqualsCriteriaQueryArray() throws Exception {
        List<Object[]> books = criteriaQueryBooksArray("id==10");
        assertEquals(1, books.size());
        Object[] info = books.get(0);
        assertEquals(10, ((Integer)info[0]).intValue());
        assertEquals("num10", info[1]);
    }

    @Test
    public void testEqualsAddressQuery() throws Exception {
        List<Book> books = queryBooks("address==Street1",
            Collections.singletonMap("address", "address.street"));
        assertEquals(1, books.size());
        Book book = books.get(0);
        assertTrue(9 == book.getId());
        assertEquals("Street1", book.getAddress().getStreet());
    }

    @Test
    public void testEqualsAddressQuery2() throws Exception {
        List<Book> books = queryBooks("street==Street1",
            null,
            Collections.singletonMap("street", "address.street"));
        assertEquals(1, books.size());
        Book book = books.get(0);
        assertTrue(9 == book.getId());
        assertEquals("Street1", book.getAddress().getStreet());
    }

    @Test
    public void testEqualsAddressQuery3() throws Exception {
        Map<String, String> beanPropertiesMap = new HashMap<>();
        beanPropertiesMap.put("street", "address.street");
        beanPropertiesMap.put("housenum", "address.houseNumber");
        List<Book> books =
            queryBooks("street==Street2;housenum=lt=5", null, beanPropertiesMap);
        assertEquals(1, books.size());
        Book book = books.get(0);
        assertTrue(10 == book.getId());
        assertEquals("Street2", book.getAddress().getStreet());

    }

    @Test
    public void testEqualsAddressQuery4() throws Exception {
        Map<String, String> beanPropertiesMap = new HashMap<>();
        beanPropertiesMap.put("street", "address.street");
        List<Book> books = queryBooks("street==Str*t*", null, beanPropertiesMap);
        assertEquals(3, books.size());
    }

    @Test
    public void testEqualsAddressQuery5() throws Exception {
        Map<String, String> beanPropertiesMap = new HashMap<>();
        beanPropertiesMap.put("street", "address.street");
        List<Book> books = queryBooks("street==Street&'3", null, beanPropertiesMap);
        assertEquals(1, books.size());
    }

    @Test
    public void testEqualsOwnerNameQuery() throws Exception {
        List<Book> books = queryBooks("ownerInfo.name.name==Fred");
        assertEquals(1, books.size());
        Book book = books.get(0);
        assertEquals("Fred", book.getOwnerInfo().getName().getName());
    }

    @Test
    public void testEqualsOwnerNameQuery3() throws Exception {
        List<Book> books = queryBooks("ownerName==Fred", null,
            Collections.singletonMap("ownerName", "ownerInfo.name.name"));
        assertEquals(1, books.size());
        Book book = books.get(0);
        assertEquals("Fred", book.getOwnerInfo().getName().getName());
    }

    @Test
    public void testFindBookInTownLibrary() throws Exception {
        List<Book> books = queryBooks("libAddress==town;bookTitle==num10", null,
            Collections.singletonMap("libAddress", "library.address"));
        assertEquals(1, books.size());
        Book book = books.get(0);
        assertEquals("Barry", book.getOwnerInfo().getName().getName());
    }

    @Test
    public void testEqualsOwnerBirthDate() throws Exception {
        List<Book> books = queryBooks("ownerbdate==2000-01-01", null,
            Collections.singletonMap("ownerbdate", "ownerInfo.dateOfBirth"));
        assertEquals(1, books.size());
        Book book = books.get(0);
        assertEquals("Fred", book.getOwnerInfo().getName().getName());

        Date d = parseDate("2000-01-01");

        assertEquals("Fred", book.getOwnerInfo().getName().getName());
        assertEquals(d, book.getOwnerInfo().getDateOfBirth());
    }


    @Test
    public void testEqualsWildcard() throws Exception {
        List<Book> books = queryBooks("bookTitle==num1*");
        assertEquals(2, books.size());
        assertTrue(10 == books.get(0).getId() && 11 == books.get(1).getId()
            || 11 == books.get(0).getId() && 10 == books.get(1).getId());
    }

    @Test
    public void testGreaterQuery() throws Exception {
        List<Book> books = queryBooks("id=gt=10");
        assertEquals(1, books.size());
        assertTrue(11 == books.get(0).getId());
    }

    @Test
    public void testGreaterEqualQuery() throws Exception {
        List<Book> books = queryBooks("id=ge=10");
        assertEquals(2, books.size());
        assertTrue(10 == books.get(0).getId() && 11 == books.get(1).getId()
            || 11 == books.get(0).getId() && 10 == books.get(1).getId());
    }

    @Test
    public void testLessEqualQuery() throws Exception {
        List<Book> books = queryBooks("id=le=10");
        assertEquals(2, books.size());
        assertTrue(9 == books.get(0).getId() && 10 == books.get(1).getId());
    }

    @Test
    public void testNotEqualsQuery() throws Exception {
        List<Book> books = queryBooks("id!=10");
        assertEquals(2, books.size());
        assertTrue(9 == books.get(0).getId() && 11 == books.get(1).getId()
            || 11 == books.get(0).getId() && 9 == books.get(1).getId());
    }

    @Override
    protected SearchConditionParser<Book> getParser(Map<String, String> visitorProps,
            Map<String, String> parserBinProps) {
        return new FiqlParser<Book>(Book.class, visitorProps, parserBinProps);
    }

    @Override
    public SearchConditionParser<Book> getParser() {
        return new FiqlParser<Book>(Book.class);
    }
}