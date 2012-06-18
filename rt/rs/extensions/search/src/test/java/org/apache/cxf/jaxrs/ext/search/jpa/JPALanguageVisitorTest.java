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

import org.apache.cxf.jaxrs.ext.search.FiqlParser;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;

import org.junit.Assert;
import org.junit.Test;

public class JPALanguageVisitorTest extends Assert {

    @Test
    public void testSimpleQuery() throws Exception {
        
        SearchCondition<Book> filter = new FiqlParser<Book>(Book.class).parse("id=lt=10");
        JPALanguageVisitor<Book> jpa = new JPALanguageVisitor<Book>(Book.class);
        filter.accept(jpa);
        assertEquals("SELECT t FROM Book t WHERE t.id < '10'", jpa.getQuery());
        
    }
}
