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

import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchParseException;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
//userName eq "admin@amarkevich.talend.com" and entitlements sw "TDP_"

public class SQLHierarchicalQueryTest {
    @Test
    public void testSimpleHierarchicalQuery() throws SearchParseException {
        FiqlParser<SearchBean> parser = new FiqlParser<>(SearchBean.class);
        SearchCondition<SearchBean> filter = parser.parse("cartridges.colour==blue");
        SQLPrinterVisitor<SearchBean> visitor = new SQLPrinterVisitor<>("printers");
        filter.accept(visitor.visitor());
        String sql = visitor.getQuery();

        assertEquals("SELECT * FROM printers left join cartridges"
                     + " on printers.id = cartridges.printer_id"
                     + " WHERE cartridges.colour = 'blue'",
                     sql);
    }
    
    @Test
    public void testAndHierarchicalQuery() throws SearchParseException {
        FiqlParser<SearchBean> parser = new FiqlParser<>(SearchBean.class);
        SearchCondition<SearchBean> filter = parser.parse("name==Epson;cartridges.colour==blue");
        SQLPrinterVisitor<SearchBean> visitor = new SQLPrinterVisitor<>("printers");
        filter.accept(visitor.visitor());
        String sql = visitor.getQuery();

        assertEquals("SELECT * FROM printers left join cartridges"
                     + " on printers.id = cartridges.printer_id"
                     + " WHERE (name = 'Epson') AND (cartridges.colour = 'blue')",
                     sql);
    }
    
    @Test(expected = SearchParseException.class)
    public void testLongHierarchicalQuery() {
        FiqlParser<SearchBean> parser = new FiqlParser<>(SearchBean.class);
        SearchCondition<SearchBean> filter = parser.parse("cartridges.producer.location==Japan");
        SQLPrinterVisitor<SearchBean> visitor = new SQLPrinterVisitor<>("printers");
        filter.accept(visitor.visitor());
    }
    
    @Test(expected = SearchParseException.class)
    public void testTooManyJoins() {
        FiqlParser<SearchBean> parser = new FiqlParser<>(SearchBean.class);
        SearchCondition<SearchBean> filter = parser.parse("cartridges.colour==blue;cartridges.location==Japan");
        SQLPrinterVisitor<SearchBean> visitor = new SQLPrinterVisitor<>("printers");
        filter.accept(visitor.visitor());
    }
}