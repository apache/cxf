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
package org.apache.cxf.systest.jaxrs;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;

import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.ExtensibleElement;
import org.apache.abdera.model.Feed;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.management.web.logging.LogRecords;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSLoggingAtomPushSpringTest extends AbstractClientServerTestBase {
    public static final int PORT = SpringServer.PORT;

    private JAXBContext context; 
    private int fakyLogger;
    private int namedLogger;
    private int resourceLogger;
    private int throwables;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        // must be 'in-process' to communicate with inner class in single JVM
        // and to spawn class SpringServer w/o using main() method
        launchServer(SpringServer.class, true);
    }

    @Ignore
    public static class SpringServer extends AbstractSpringServer {
        public static final int PORT = allocatePortAsInt(SpringServer.class);
        public SpringServer() {
            super("/jaxrs_logging_atompush", PORT);
        }
    }

    @Before
    public void before() throws Exception {
        Resource.clear();
        Resource2.clear();
        Resource3.clear();
        Resource4.clear();
        Resource5.clear();
        context = JAXBContext.newInstance(LogRecords.class, 
            org.apache.cxf.management.web.logging.LogRecord.class);
    }

    @Test
    public void testFeedsWithLogRecordsOneEntry() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/root");
        try  {
            wc.path("/log").get();
        } catch (Exception ex) {
            //ignore
        }
        Thread.sleep(3000);
        List<Feed> elements = Resource.getElements();
        assertEquals(8, elements.size());

        resetCounters();
        for (Feed feed : elements) {
            List<Entry> entries = feed.getEntries();
            assertEquals(1, entries.size());
            Entry e = entries.get(0);
            LogRecords records = readLogRecords(e.getContent());
            List<org.apache.cxf.management.web.logging.LogRecord> list = records.getLogRecords();
            assertNotNull(list);
            assertEquals(1, list.size());
            updateCounters(list.get(0), "Resource");
        }
        
        verifyCounters();
    }
    
    @Test
    public void testFeedsWithBatchLogRecordsOneEntry() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/batch");
        wc.path("/log").get();
        Thread.sleep(3000);
        List<Feed> elements = Resource2.getElements();
        assertEquals(2, elements.size());
        
        resetCounters();
        for (Feed feed : elements) {
            List<Entry> entries = feed.getEntries();
            assertEquals(4, entries.size());
            
            for (Entry e : entries) {
                updateCounters(readLogRecord(e.getContent()), "Resource2");
            }
        }
        
        verifyCounters();
        
    }
    
    @Test
    public void testEntriesWithLogRecordsOneEntry() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/entries");
        wc.path("/log").get();
        Thread.sleep(3000);
        List<Entry> elements = Resource3.getElements();
        assertEquals(8, elements.size());
        
        resetCounters();
        
        for (Entry e : elements) {
            updateCounters(readLogRecord(e.getContent()), "Resource3");
        }
        
        verifyCounters();
        
    }
    
    @Test
    public void testManyEntries() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/entriesMany");
        wc.path("/log").get();
        Thread.sleep(3000);
        List<Entry> elements = Resource4.getElements();
        assertEquals(4, elements.size());
        
        resetCounters();
        
        for (Entry e : elements) {
            LogRecords records = readLogRecords(e.getContent());
            List<org.apache.cxf.management.web.logging.LogRecord> list = records.getLogRecords();
            assertNotNull(list);
            assertEquals(2, list.size());
            for (org.apache.cxf.management.web.logging.LogRecord record : list) {
                updateCounters(record, "Resource4");
            }
            
        }
        verifyCounters();
    }
    
    @Test
    public void testFeedsWithLogRecordsExtension() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/extensions");
        wc.path("/log").get();
        Thread.sleep(3000);
        List<Feed> elements = Resource5.getElements();
        assertEquals(8, elements.size());

        resetCounters();
        for (Feed feed : elements) {
            List<Entry> entries = feed.getEntries();
            assertEquals(1, entries.size());
            Entry e = entries.get(0);
            LogRecords records = readLogRecordsExtension(e);
            List<org.apache.cxf.management.web.logging.LogRecord> list = records.getLogRecords();
            assertNotNull(list);
            assertEquals(1, list.size());
            updateCounters(list.get(0), "Resource5");
        }
        
        verifyCounters();
    }
    
    @Ignore
    @Path("/root")
    public static class Resource {
        private static final Logger LOG1;
        private static final Logger LOG2;
        
        static {
            System.gc();
            LOG1 = LogUtils.getL7dLogger(Resource.class);
            LOG2 = LogUtils.getL7dLogger(Resource.class, null, "namedLogger");
        }
        
        private static List<Feed> feeds = new ArrayList<Feed>();
        
        @GET
        @Path("/log")
        public void doLogging() {
            doLog(LOG1, LOG2);
        }

        @POST
        @Path("/feeds")
        public void consume(Feed feed) {
            feed.toString();
            synchronized (Resource.class) {
                feeds.add(feed);
            }
        }
        
        public static void clear() {
            feeds.clear();
        }
        
        public static synchronized List<Feed> getElements() {
            return new ArrayList<Feed>(feeds);
        }
    }
    
    @Ignore
    @Path("/batch")
    public static class Resource2 {
        private static final Logger LOG1 = LogUtils.getL7dLogger(Resource2.class);
        private static final Logger LOG2 = LogUtils.getL7dLogger(Resource2.class, null, "namedLogger");
        
        private static List<Feed> feeds = new ArrayList<Feed>();
        
        @GET
        @Path("/log")
        public void doLogging() {
            doLog(LOG1, LOG2);
        }

        @POST
        @Path("/feeds")
        public void consume(Feed feed) {
            feed.toString();
            synchronized (Resource2.class) {
                feeds.add(feed);
            }
        }
        
        public static void clear() {
            feeds.clear();
        }
        
        public static synchronized List<Feed> getElements() {
            return new ArrayList<Feed>(feeds);
        }
    }
    
    @Ignore
    @Path("/entries")
    public static class Resource3 {
        private static final Logger LOG1 = LogUtils.getL7dLogger(Resource3.class);
        private static final Logger LOG2 = LogUtils.getL7dLogger(Resource3.class, null, "namedLogger");
        
        private static List<Entry> entries = new ArrayList<Entry>();
        
        @GET
        @Path("/log")
        public void doLogging() {
            doLog(LOG1, LOG2);
        }

        @POST
        @Path("/entries")
        public void consume(Entry entry) {
            entry.toString();
            synchronized (Resource3.class) {
                entries.add(entry);
            }
        }
        
        public static void clear() {
            entries.clear();
        }
        
        public static synchronized List<Entry> getElements() {
            return new ArrayList<Entry>(entries);
        }
    }
    
    @Ignore
    @Path("/entriesMany")
    public static class Resource4 {
        private static final Logger LOG1 = LogUtils.getL7dLogger(Resource4.class);
        private static final Logger LOG2 = LogUtils.getL7dLogger(Resource4.class, null, "namedLogger");
        
        private static List<Entry> entries = new ArrayList<Entry>();
        
        @GET
        @Path("/log")
        public void doLogging() {
            doLog(LOG1, LOG2);
        }

        @POST
        @Path("/entries")
        public void consume(Entry entry) {
            entry.toString();
            synchronized (Resource4.class) {
                entries.add(entry);
            }
        }
        
        public static void clear() {
            entries.clear();
        }
        
        public static synchronized List<Entry> getElements() {
            return new ArrayList<Entry>(entries);
        }
    }
    
    @Ignore
    @Path("/extensions")
    public static class Resource5 {
        private static final Logger LOG1 = LogUtils.getL7dLogger(Resource5.class);
        private static final Logger LOG2 = LogUtils.getL7dLogger(Resource5.class, null, "namedLogger");
        
        private static List<Feed> feeds = new ArrayList<Feed>();
        
        @GET
        @Path("/log")
        public void doLogging() {
            doLog(LOG1, LOG2);
        }

        @POST
        @Path("/feeds")
        public void consume(Feed feed) {
            feed.toString();
            synchronized (Resource5.class) {
                feeds.add(feed);
            }
        }
        
        public static void clear() {
            feeds.clear();
        }
        
        public static List<Feed> getElements() {
            return new ArrayList<Feed>(feeds);
        }
    }
    
    private static void doLog(Logger l1, Logger l2) {
        l1.severe("severe message");
        l1.warning("warning message");
        l1.info("info message");
        LogRecord r = new LogRecord(Level.FINE, "fine message");
        r.setThrown(new IllegalArgumentException("tadaam"));
        l1.log(r);
        r = new LogRecord(Level.FINER, "finer message with {0} and {1}");
        r.setParameters(new Object[] {
            "param1", "param2"
        });
        r.setLoggerName("faky-logger");
        l1.log(r);
        l1.finest("finest message");

        // for LOG2 only 'warning' and above messages should be logged
        l2.severe("severe message");
        l2.severe("severe message2");
        l2.info("info message - should not pass!");
        l2.finer("finer message - should not pass!");
    }
    
    private LogRecords readLogRecords(String value) throws Exception {
        return (LogRecords)context.createUnmarshaller().unmarshal(new StringReader(value));
    }
    
    private org.apache.cxf.management.web.logging.LogRecord readLogRecord(String value) throws Exception {
        return (org.apache.cxf.management.web.logging.LogRecord)
            context.createUnmarshaller().unmarshal(new StringReader(value));
    }
    
    private LogRecords readLogRecordsExtension(Entry e) throws Exception {
        ExtensibleElement el = e.getExtension(new QName("http://cxf.apache.org/log", "logRecords", "log"));
        LogRecords records = new LogRecords();
        List<org.apache.cxf.management.web.logging.LogRecord> list = 
            new ArrayList<org.apache.cxf.management.web.logging.LogRecord>();
        for (Element element : el.getElements()) {
            org.apache.cxf.management.web.logging.LogRecord record = 
                new org.apache.cxf.management.web.logging.LogRecord();
            Element loggerName = element.getFirstChild(
                                     new QName("http://cxf.apache.org/log", "loggerName", "log"));
            if (loggerName != null) {
                record.setLoggerName(loggerName.getText());
            }
            Element throwable = element.getFirstChild(
                                     new QName("http://cxf.apache.org/log", "throwable", "log")); 
            if (throwable != null) {
                record.setThrowable(throwable.getText());
            }
            list.add(record);
        }
        records.setLogRecords(list);
        return records;
    }
    
    private void updateCounters(org.apache.cxf.management.web.logging.LogRecord record, String clsName) {
        String name = record.getLoggerName();
        if (name != null && name.length() > 0) {
            if (("org.apache.cxf.systest.jaxrs.JAXRSLoggingAtomPushSpringTest$" + clsName).equals(name)) {
                resourceLogger++;      
            } else if ("namedLogger".equals(name)) {
                namedLogger++;      
            } else if ("faky-logger".equals(name)) {
                fakyLogger++;      
            }
        } else {
            assertNotNull(record.getThrowable());
            throwables++;
        }
    }
    
    private void resetCounters() {
        fakyLogger = 0;
        namedLogger = 0;
        resourceLogger = 0;
        throwables = 0;
    }
    
    private void verifyCounters() {
        assertEquals(1, throwables);
        assertEquals(4, resourceLogger);
        assertEquals(2, namedLogger);
        assertEquals(1, fakyLogger);
    }
}
