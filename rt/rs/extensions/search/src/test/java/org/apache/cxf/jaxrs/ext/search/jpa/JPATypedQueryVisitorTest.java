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

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JPATypedQueryVisitorTest extends Assert {

    private EntityManagerFactory emFactory;

    private EntityManager em;

    private Connection connection;

    @Before
    public void setUp() throws Exception {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            connection = DriverManager.getConnection("jdbc:hsqldb:mem:books-jpa", "sa", "");
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Exception during HSQL database init.");
        }
        try {
            emFactory = Persistence.createEntityManagerFactory("testUnit");
            em = emFactory.createEntityManager();
         
            em.getTransaction().begin();
            Book b1 = new Book();
            b1.setId(9);
            em.persist(b1);
            assertTrue(em.contains(b1));
            Book b2 = new Book();
            b2.setId(10);
            em.persist(b2);
            assertTrue(em.contains(b2));
            Book b3 = new Book();
            b3.setId(11);
            em.persist(b3);
            assertTrue(em.contains(b3));
            
            
            em.getTransaction().commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Exception during JPA EntityManager creation.");
        }
    }

    @After
    public void tearDown() throws Exception {
        try {
            if (em != null) {
                em.close();
            }
            if (emFactory != null) {
                emFactory.close();
            }
        } finally {    
            try {
                connection.createStatement().execute("SHUTDOWN");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    @Test
    public void testSimpleQuery() throws Exception {
        SearchCondition<Book> filter = new FiqlParser<Book>(Book.class).parse("id=lt=10,id=gt=10");
        JPATypedQueryVisitor<Book> jpa = new JPATypedQueryVisitor<Book>(em, Book.class);
        filter.accept(jpa);
        TypedQuery<Book> query = jpa.getQuery();
        List<Book> books = query.getResultList();
        assertEquals(2, books.size());
        assertTrue(9 == books.get(0).getId() && 11 == books.get(1).getId()
            || 11 == books.get(0).getId() && 9 == books.get(1).getId());
    }
}
