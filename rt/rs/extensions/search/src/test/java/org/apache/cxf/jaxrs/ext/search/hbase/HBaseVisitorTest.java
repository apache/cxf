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
package org.apache.cxf.jaxrs.ext.search.hbase;

import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.Filter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class HBaseVisitorTest extends Assert {
    public static final byte[] BOOK_FAMILY = "book".getBytes();
    public static final byte[] NAME_QUALIFIER = "name".getBytes();
    
    Table table;
    @Before
    public void setUp() throws Exception {
        try {
            Configuration hBaseConfig =  HBaseConfiguration.create();
            Connection connection = ConnectionFactory.createConnection(hBaseConfig);
            table = connection.getTable(TableName.valueOf("books"));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    @Test
    @Ignore("Enable as soon as it is understood how to run HBase tests in process")
    public void testScanWithFilterVisitor() throws Exception {
        Scan scan = new Scan();
        
        SearchCondition<SearchBean> sc = new FiqlParser<SearchBean>(SearchBean.class).parse("name==CXF");
        HBaseQueryVisitor<SearchBean> visitor = new HBaseQueryVisitor<SearchBean>("book");
        sc.accept(visitor);
        Filter filter = visitor.getQuery();
        scan.setFilter(filter);
        ResultScanner rs = table.getScanner(scan);
        try {
            int count = 0;
            for (Result r = rs.next(); r != null; r = rs.next()) {
                assertEquals("row2", new String(r.getRow()));
                assertEquals("CXF", new String(r.getValue(BOOK_FAMILY, NAME_QUALIFIER)));
                count++;
            }
            assertEquals(1, count);
        } finally {
            rs.close();  
        }
    }
    
    @After
    public void tearDown() throws Exception {
        if (table != null) {
            table.close();
        }
    }
}
