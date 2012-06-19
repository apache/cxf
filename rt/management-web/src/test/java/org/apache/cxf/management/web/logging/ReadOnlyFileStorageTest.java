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
package org.apache.cxf.management.web.logging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.PrimitiveSearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ReadOnlyFileStorageTest extends Assert {
    
    private ReadOnlyFileStorage storage;
    
    @Before
    public void setUp() throws Exception {
        storage = new ReadOnlyFileStorage();
       
        storage.setNumberOfColumns("7");
        storage.setColumnSep("|");
              
        Map<Integer, String> columnsMap = new HashMap<Integer, String>(); 
        columnsMap.put(1, ReadOnlyFileStorage.DATE_PROPERTY);
        columnsMap.put(2, ReadOnlyFileStorage.LEVEL_PROPERTY);
        columnsMap.put(7, ReadOnlyFileStorage.MESSAGE_PROPERTY);
       
        storage.setColumnsMap(columnsMap);
       
        storage.setRecordDateFormat(ReadOnlyFileStorage.DATE_ONLY_FORMAT 
                                   + " " + "kk:mm:ss,SSS");
        storage.setFileNameDatePattern("(\\d\\d\\d\\d-\\d\\d-\\d\\d)-.+");
        
        storage.setUseFileModifiedDate(true);
        
       
    }
    
    @After
    public void tearDown() throws Exception {
        storage.close();
    }
    
    @Test
    public void testReadRecords() throws Exception {
        
        storage.setLogLocation(getClass().getResource("logs/2011-01-22-karaf.log").toURI().getPath());
        List<LogRecord> recordsFirstPage1 = readPage(1, 10, 10);
        
        readPage(2, 10, 10);
        readPage(3, 10, 10);
        List<LogRecord> recordsLastPage1 = readPage(4, 10, 2);
        
        List<LogRecord> recordsFirstPage2 = readPage(1, 10, 10);
        compareRecords(recordsFirstPage1, recordsFirstPage2);
        
        List<LogRecord> recordsLastPage2 = readPage(4, 10, 2);
        compareRecords(recordsLastPage1, recordsLastPage2);
        
        LogRecord lastRecord = recordsLastPage1.get(1);
        assertTrue(lastRecord.getMessage().contains("Pax Web available at"));
    }
    
        
    @Test
    public void testReadRecordsWithMultiLines() throws Exception {
        
        storage.setLogLocation(getClass().getResource("logs/2011-01-23-karaf.log").toURI().getPath());
        List<LogRecord> recordsFirstPage1 = readPage(1, 10, 10);
        
        List<LogRecord> recordsLastPage1 = readPage(2, 10, 10);
        
        List<LogRecord> recordsFirstPage2 = readPage(1, 10, 10);
        compareRecords(recordsFirstPage1, recordsFirstPage2);
        
        List<LogRecord> recordsLastPage2 = readPage(2, 10, 10);
        compareRecords(recordsLastPage1, recordsLastPage2);
        
        LogRecord recordWithExceptionInMessage = recordsFirstPage1.get(2);
        assertEquals(LogLevel.ERROR, recordWithExceptionInMessage.getLevel());
        assertTrue(recordWithExceptionInMessage.getMessage()
                   .contains("mvn:org.apache.cxf/cxf-bundle/"));
        assertTrue(recordWithExceptionInMessage.getMessage()
                   .contains("Caused by: org.osgi.framework.BundleException"));
    }
    
    @Test
    public void testReadRecordsWithMultipleFiles() throws Exception {
        
        List<String> locations = new ArrayList<String>();
        locations.add(getClass().getResource("logs/2011-01-22-karaf.log").toURI().getPath());
        locations.add(getClass().getResource("logs/2011-01-23-karaf.log").toURI().getPath());
        storage.setLogLocations(locations);
        List<LogRecord> recordsFirstPage1 = readPage(1, 10, 10);
        readPage(2, 10, 10);
        readPage(3, 10, 10);
        List<LogRecord> recordsPage4 = readPage(4, 10, 10);
        readPage(4, 10, 10);
        readPage(5, 10, 10);
        List<LogRecord> recordsLastPage1 = readPage(6, 10, 2);
        
        LogRecord recordWithExceptionInMessage = recordsPage4.get(4);
        assertEquals(LogLevel.ERROR, recordWithExceptionInMessage.getLevel());
        assertTrue(recordWithExceptionInMessage.getMessage()
                   .contains("mvn:org.apache.cxf/cxf-bundle/"));
        assertTrue(recordWithExceptionInMessage.getMessage()
                   .contains("Caused by: org.osgi.framework.BundleException"));
        
        List<LogRecord> recordsFirstPage2 = readPage(1, 10, 10);
        compareRecords(recordsFirstPage1, recordsFirstPage2);
        
        List<LogRecord> recordsLastPage2 = readPage(6, 10, 2);
        compareRecords(recordsLastPage1, recordsLastPage2);
    }
    
    @Test
    public void testReadRecordsWithMultipleFilesAndSearch() throws Exception {
        
        List<String> locations = new ArrayList<String>();
        locations.add(getClass().getResource("logs/2011-01-22-karaf.log").toURI().getPath());
        locations.add(getClass().getResource("logs/2011-01-23-karaf.log").toURI().getPath());
        storage.setLogLocations(locations);
        SearchCondition<LogRecord> sc = 
            new PrimitiveSearchCondition<LogRecord>("message",
                "*FeaturesServiceImpl.java:323*",
                ConditionType.EQUALS,
                new LogRecord()); 
        List<LogRecord> recordsFirstPage1 = readPage(1, sc, 2, 1);
        List<LogRecord> recordsFirstPage2 = readPage(1, sc, 2, 1);
        compareRecords(recordsFirstPage1, recordsFirstPage2);
    }
    
    @Test
    public void testReadRecordsWithMultipleFilesAndSearchDates() throws Exception {
        
        List<String> locations = new ArrayList<String>();
        locations.add(getClass().getResource("logs/2011-01-22-karaf.log").toURI().getPath());
        locations.add(getClass().getResource("logs/2011-01-23-karaf.log").toURI().getPath());
        storage.setLogLocations(locations);
        
        Map<String, String> props = new HashMap<String, String>();
        props.put(SearchUtils.DATE_FORMAT_PROPERTY, "yyyy-MM-dd'T'HH:mm:ss SSS");
        props.put(SearchUtils.TIMEZONE_SUPPORT_PROPERTY, "false");
        FiqlParser<LogRecord> parser = new FiqlParser<LogRecord>(LogRecord.class, props);
        
        SearchCondition<LogRecord> sc = parser.parse("date==2011-01-22T11:49:17 184");
        
        List<LogRecord> recordsFirstPage1 = readPage(1, sc, 2, 1);
        List<LogRecord> recordsFirstPage2 = readPage(1, sc, 2, 1);
        compareRecords(recordsFirstPage1, recordsFirstPage2);
        
        LogRecord record = recordsFirstPage1.get(0);
        assertEquals("Initializing Timer", record.getMessage());
    }
    
    @Test
    public void testReadRecordsWithMultipleFilesAndSearchDates2() throws Exception {
        
        List<String> locations = new ArrayList<String>();
        locations.add(getClass().getResource("logs/2011-01-22-karaf.log").toURI().getPath());
        locations.add(getClass().getResource("logs/2011-01-23-karaf.log").toURI().getPath());
        storage.setLogLocations(locations);
        
        Map<String, String> props = new HashMap<String, String>();
        props.put(SearchUtils.DATE_FORMAT_PROPERTY, "yyyy-MM-dd");
        props.put(SearchUtils.TIMEZONE_SUPPORT_PROPERTY, "false");
        FiqlParser<LogRecord> parser = new FiqlParser<LogRecord>(LogRecord.class, props);
        
        SearchCondition<LogRecord> sc = parser.parse("date=lt=2011-01-23");
        
        List<LogRecord> recordsFirstPage1 = readPage(1, sc, 32, 32);
        readPage(2, sc, 32, 0);
        List<LogRecord> recordsFirstPage2 = readPage(1, sc, 32, 32);
                
        compareRecords(recordsFirstPage1, recordsFirstPage2);
        
        LogRecord firstRecord = recordsFirstPage1.get(0);
        assertEquals("Starting JMX OSGi agent", firstRecord.getMessage());
        LogRecord lastRecord = recordsFirstPage1.get(31);
        assertTrue(lastRecord.getMessage().contains("Pax Web available at"));
        
        readPage(2, sc, 32, 0);
    }
    
    @Test
    public void testReadRecordsWithMultipleFiles2() throws Exception {
        
        List<String> locations = new ArrayList<String>();
        locations.add(getClass().getResource("logs/2011-01-23-karaf.log").toURI().getPath());
        locations.add(getClass().getResource("logs/2011-01-22-karaf.log").toURI().getPath());
        storage.setLogLocations(locations);
        List<LogRecord> recordsFirstPage1 = readPage(1, 10, 10);
        readPage(2, 10, 10);
        readPage(3, 10, 10);
        readPage(4, 10, 10);
        readPage(5, 10, 10);
        List<LogRecord> recordsLastPage1 = readPage(6, 10, 2);
        
        LogRecord recordWithExceptionInMessage = recordsFirstPage1.get(2);
        assertEquals(LogLevel.ERROR, recordWithExceptionInMessage.getLevel());
        assertTrue(recordWithExceptionInMessage.getMessage()
                   .contains("mvn:org.apache.cxf/cxf-bundle/"));
        assertTrue(recordWithExceptionInMessage.getMessage()
                   .contains("Caused by: org.osgi.framework.BundleException"));
        
        List<LogRecord> recordsFirstPage2 = readPage(1, 10, 10);
        compareRecords(recordsFirstPage1, recordsFirstPage2);
        
        List<LogRecord> recordsLastPage2 = readPage(6, 10, 2);
        compareRecords(recordsLastPage1, recordsLastPage2);
    }
    
    @Test
    public void testReadRecordsWithScanDirectory() throws Exception {
        
        String dir = getClass().getResource("logs").toURI().getPath();
        storage.setLogLocation(dir);
        List<LogRecord> recordsFirstPage1 = readPage(1, 10, 10);
        readPage(2, 10, 10);
        readPage(3, 10, 10);
        List<LogRecord> recordsPage4 = readPage(4, 10, 10);
        readPage(5, 10, 10);
        List<LogRecord> recordsLastPage1 = readPage(6, 10, 2);
        
        LogRecord recordWithExceptionInMessage = recordsPage4.get(4);
        assertEquals(LogLevel.ERROR, recordWithExceptionInMessage.getLevel());
        assertTrue(recordWithExceptionInMessage.getMessage()
                   .contains("mvn:org.apache.cxf/cxf-bundle/"));
        assertTrue(recordWithExceptionInMessage.getMessage()
                   .contains("Caused by: org.osgi.framework.BundleException"));
        
        List<LogRecord> recordsFirstPage2 = readPage(1, 10, 10);
        compareRecords(recordsFirstPage1, recordsFirstPage2);
        
        List<LogRecord> recordsLastPage2 = readPage(6, 10, 2);
        compareRecords(recordsLastPage1, recordsLastPage2);
    }
    
    private void compareRecords(List<LogRecord> records1, List<LogRecord> records2) {
        for (int i = 0; i < records1.size(); i++) {
            LogRecord r1 = records1.get(i);
            LogRecord r2 = records2.get(i);
            assertEquals(r1.getLevel(), r2.getLevel());
            assertEquals(r1.getDate(), r2.getDate());
            assertEquals(r1.getMessage(), r2.getMessage());
        }    
    }
    
    private List<LogRecord> readPage(int page, int pageSize, int expected) {
        return readPage(page, null, pageSize, expected);
        
    }
    
    private List<LogRecord> readPage(int page, SearchCondition<LogRecord> sc,
                                     int pageSize, int expected) {
        List<LogRecord> records = new ArrayList<LogRecord>();
        storage.load(records, sc, page, pageSize);
        assertEquals(expected, records.size());
        return records;
        
    }

}
