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
        storage.setUseFileModifiedDate(true);
       
       
    }
    
    @After
    public void tearDown() throws Exception {
        storage.close();
    }
    
    @Test
    public void testReadRecords() throws Exception {
        
        storage.setLogLocation(getClass().getResource("logs/karaf.log.1").toURI().getPath());
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
        
        storage.setLogLocation(getClass().getResource("logs/karaf.log").toURI().getPath());
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
        locations.add(getClass().getResource("logs/karaf.log.1").toURI().getPath());
        locations.add(getClass().getResource("logs/karaf.log").toURI().getPath());
        storage.setLogLocations(locations);
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
            assertEquals(r1.getEventTimestamp(), r2.getEventTimestamp());
            assertEquals(r1.getMessage(), r2.getMessage());
        }    
    }
    
    private List<LogRecord> readPage(int page, int pageSize, int expected) {
        List<LogRecord> records = new ArrayList<LogRecord>();
        storage.load(records, null, page, pageSize);
        assertEquals(expected, records.size());
        return records;
        
    }

}
