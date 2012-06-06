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

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.xml.bind.JAXBContext;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.provider.AtomEntryProvider;
import org.apache.cxf.jaxrs.provider.AtomFeedProvider;
import org.apache.cxf.management.web.logging.LogLevel;
import org.apache.cxf.management.web.logging.ReadWriteLogStorage;
import org.apache.cxf.management.web.logging.ReadableLogStorage;
import org.apache.cxf.management.web.logging.atom.AtomPullServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSLoggingAtomPullSpringTest extends AbstractBusClientServerTestBase {
    public static final int PORT = SpringServer.PORT;

    private static JAXBContext context; 
    private int fakyLogger;
    private int namedLogger;
    private int resourceLogger;
    private int throwables;
    private int errorLevels;
    private int traceLevels;
    private int infoLevels;
    private int debugLevels;
    private int warningLevels;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        //make sure the Resource things have their static initializers called 
        //to make sure the Loggers are created.  Otherwise, the Loggers that the server 
        //sets the handler into could be garbage collected before the init is called
        new Resource();
        new Resource2();
        new Resource3();
        // must be 'in-process' to communicate with inner class in single JVM
        // and to spawn class SpringServer w/o using main() method
        String resourceDir = 
            JAXRSLoggingAtomPullSpringTest.class.getResource("resources").toURI().getPath();
        System.setProperty("systemtests.jaxrs.logs.folder", resourceDir);
        launchServer(SpringServer.class, true);
        context = JAXBContext.newInstance(org.apache.cxf.management.web.logging.LogRecord.class);
        createStaticBus();
        System.clearProperty("systemtests.jaxrs.logs.folder");
    }
    
    @Ignore
    public static class SpringServer extends AbstractSpringServer {
        public static final int PORT = allocatePortAsInt(SpringServer.class);
        public SpringServer() {
            super("/jaxrs_logging_atompull", PORT);
        }
    }

    @Before
    public void before() throws Exception {
        Storage.clearRecords();
    }

    @Test
    public void testFeed() throws Exception {
        String listing = WebClient.create("http://localhost:" + PORT + "/services").get(String.class);
        assertTrue(listing, listing.contains("http://localhost:" + PORT + "/atom/logs"));
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/resource/root");
        wc.path("/log").get();
        Thread.sleep(3000);
        
        checkSimpleFeed(getFeed("http://localhost:" + PORT + "/atom/logs").getEntries());
        checkSimpleFeed(getFeed("http://localhost:" + PORT + "/atom/logs").getEntries());
     
        List<Entry> entries = new LinkedList<Entry>();
        WebClient wcEntry = WebClient.create("http://localhost:" + PORT + "/atom/logs",
            Collections.singletonList(new AtomEntryProvider()))
            .accept("application/atom+xml;type=entry");
        for (int i = 0; i < 8; i++) {
            Entry entry = wcEntry.path("entry/" + i).get(Entry.class);
            entry.toString();
            entries.add(entry);
            wcEntry.back(true);
        }
        checkSimpleFeed(entries);
    }
    
    private void checkSimpleFeed(List<Entry> entries) throws Exception {
        assertEquals(8, entries.size());
        
        resetCounters();
        for (Entry e : entries) {
            updateCounters(readLogRecord(e.getContent()), "Resource", "namedLogger");
        }
        
        verifyCounters();
    }
    
    @Test
    public void testPagedFeed() throws Exception {
        WebClient wcReset = WebClient.create("http://localhost:" + PORT + "/reset");
        wcReset.post(null);
        
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/resource2/paged");
        wc.path("/log").get();
        Thread.sleep(3000);
        
        verifyPages("http://localhost:" + PORT + "/atom2/logs", "next", 3, 2, "theNamedLogger");
        verifyPages("http://localhost:" + PORT + "/atom2/logs/3", "previous", 2, 3, "theNamedLogger");
    }
    
    @Test
    public void testPagedFeedWithQuery() throws Exception {
        WebClient wcReset = WebClient.create("http://localhost:" + PORT + "/reset");
        wcReset.post(null);
        
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/resource2/paged/log");
        wc.get();
        Thread.sleep(3000);
        
        String address = "http://localhost:" + PORT + "/atom2/logs"
                         + "?_s=level==INFO,level==ERROR,level==WARN";
        verifyPagesWithQuery(address, "next", 3, 2, "Resource2", "theNamedLogger");
        verifyPagesWithQuery(address, "next", 3, 2, "Resource2", "theNamedLogger");    
    }
    
    
    @Test
    public void testPagedFeedWithReadWriteStorage() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/resource3/storage");
        wc.path("/log").get();
        Thread.sleep(3000);
        
        verifyStoragePages("http://localhost:" 
                           + PORT + "/atom3/logs", "next", "Resource3", "theStorageLogger", false);
        List<org.apache.cxf.management.web.logging.LogRecord> list = Storage.getRecords();
        assertEquals(4, list.size());
        verifyStoragePages("http://localhost:" 
                           + PORT + "/atom3/logs", "next", "Resource3", "theStorageLogger", false);
        verifyStoragePages("http://localhost:" + PORT + "/atom3/logs/2", "previous", "Resource3", 
                           "theStorageLogger", false);
    }
    
    @Test
    public void testPagedFeedWithReadOnlyStorage() throws Exception {
        verifyStoragePages("http://localhost:" 
                           + PORT + "/atom4/logs", "next", "Resource4", "readOnlyStorageLogger", true);
        verifyStoragePages("http://localhost:" + PORT + "/atom4/logs/2", "previous", "Resource4", 
                           "readOnlyStorageLogger", true);
    }
    
    @Test
    public void testPagedFeedWithReadonlyStorageAngQuery() throws Exception {
        checkInfoLevelOnly();
        checkDebugLevelOnly();
        checkInfoLevelOnly();
        checkDebugLevelOnly();
        checkInfoOrDebugLevel();
    }
    
    private void checkInfoLevelOnly() throws Exception {
        String address = "http://localhost:" + PORT + "/atom5/logs"
            + "?_s=level==INFO";
        List<Entry> entries = new ArrayList<Entry>();
        String href1 = fillPagedEntries(entries, address, 1, "next", true);
        resetCounters();
        for (Entry e : entries) {
            updateCounters(readLogRecord(e.getContent()), "", "");
        }
        assertEquals(0, errorLevels);
        assertEquals(1, infoLevels);
        assertEquals(0, debugLevels);
        assertEquals(0, traceLevels);
        assertEquals(0, warningLevels);
        
        entries.clear();
        fillPagedEntries(entries, href1, 0, "next", false);
    }
    
    private void checkInfoOrDebugLevel() throws Exception {
        String address = "http://localhost:" + PORT + "/atom5/logs"
            + "?_s=level==INFO,level==DEBUG";
        List<Entry> entries = new ArrayList<Entry>();
        String href1 = fillPagedEntries(entries, address, 1, "next", true);
        resetCounters();
        for (Entry e : entries) {
            updateCounters(readLogRecord(e.getContent()), "", "");
        }
        assertEquals(0, errorLevels);
        assertEquals(1, infoLevels);
        assertEquals(0, debugLevels);
        assertEquals(0, traceLevels);
        assertEquals(0, warningLevels);
        
        entries.clear();
        String href2 = fillPagedEntries(entries, href1, 1, "next", true);
        resetCounters();
        for (Entry e : entries) {
            updateCounters(readLogRecord(e.getContent()), "", "");
        }
        assertEquals(0, errorLevels);
        assertEquals(0, infoLevels);
        assertEquals(1, debugLevels);
        assertEquals(0, traceLevels);
        assertEquals(0, warningLevels);
        
        entries.clear();
        fillPagedEntries(entries, href2, 0, "next", false);
    }
    
    private void checkDebugLevelOnly() throws Exception {
        String address = "http://localhost:" + PORT + "/atom5/logs"
            + "?_s=level==DEBUG";
        List<Entry> entries = new ArrayList<Entry>();
        String href1 = fillPagedEntries(entries, address, 1, "next", true);
        resetCounters();
        for (Entry e : entries) {
            updateCounters(readLogRecord(e.getContent()), "", "");
        }
        assertEquals(0, errorLevels);
        assertEquals(0, infoLevels);
        assertEquals(1, debugLevels);
        assertEquals(0, traceLevels);
        assertEquals(0, warningLevels);
        
        entries.clear();
        fillPagedEntries(entries, href1, 0, "next", false);
    }
    
    private void verifyStoragePages(String startAddress, String rel, 
                                    String resourceName, String nLogger,
                                    boolean readOnly) 
        throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        String href1 = fillPagedEntries(entries, startAddress, 4, rel, true);
        fillPagedEntries(entries, href1, 4, rel, "next".equals(rel) && readOnly ? true : false);
        assertEquals(8, entries.size());
        
        resetCounters();
        for (Entry e : entries) {
            updateCounters(readLogRecord(e.getContent()), resourceName, nLogger);
        }
        if ("Resource4".equals(resourceName)) {
            assertEquals(1, throwables);
            assertEquals(6, resourceLogger);
            assertEquals(2, namedLogger);
            assertEquals(0, fakyLogger);
        } else {
            verifyCounters();
        }
    }
    
    private void verifyPages(String startAddress, String rel, 
                             int firstValue, int lastValue, String nLogger) 
        throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        String href1 = fillPagedEntries(entries, startAddress, 
                                        firstValue, rel, true);
        String href2 = fillPagedEntries(entries, href1, 3, rel, true);
        fillPagedEntries(entries, href2, lastValue, rel, false);
        assertEquals(8, entries.size());
        
        resetCounters();
        for (Entry e : entries) {
            updateCounters(readLogRecord(e.getContent()), "Resource2", nLogger);
        }
        verifyCounters();
    }
    
    private void verifyPagesWithQuery(String startAddress, String rel, 
                             int firstValue, int lastValue, String resourceName, String nLogger) 
        throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        String hrefRel = fillPagedEntries(entries, startAddress, 
                                        firstValue, rel, true);
        hrefRel = fillPagedEntries(entries, hrefRel, lastValue, rel, false);
        assertEquals(firstValue + lastValue, entries.size());
        
        resetCounters();
        for (Entry e : entries) {
            updateCounters(readLogRecord(e.getContent()), "Resource2", nLogger);
        }
        assertEquals(3, errorLevels);
        assertEquals(1, infoLevels);
        assertEquals(0, debugLevels);
        assertEquals(0, traceLevels);
        assertEquals(1, warningLevels);
    }
    
    private String fillPagedEntries(List<Entry> entries, String href, int expected, 
                                    String rel, boolean relExpected) {
        Feed feed = getFeed(href);

        assertEquals(expected, feed.getEntries().size());
        entries.addAll(feed.getEntries());
        
        Link link = feed.getLink(rel);
        if (relExpected) {
            assertNotNull(link);
            return link.getHref().toString();
        } else {
            assertNull(link);
            return null;
        }
    }
    
    private Feed getFeed(String address) {
        WebClient wc = WebClient.create(address,
                                         Collections.singletonList(new AtomFeedProvider()));
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000);
        
        Feed feed = wc.accept("application/atom+xml").get(Feed.class);
        feed.toString();
        return feed;
    }
    
    @Ignore
    @Path("/root")
    public static class Resource {
        private static final Logger LOG1 = LogUtils.getL7dLogger(Resource.class);
        private static final Logger LOG2 = LogUtils.getL7dLogger(Resource.class, null, "namedLogger");
        
        @GET
        @Path("/log")
        public void doLogging() {
            doLog(Resource.LOG1, Resource.LOG2);
        }

    }
    
    @Ignore
    @Path("/paged")
    public static class Resource2 {
        private static final Logger LOG1 = LogUtils.getL7dLogger(Resource2.class);
        private static final Logger LOG2 = LogUtils.getL7dLogger(Resource2.class, null, "theNamedLogger");
        
        @GET
        @Path("/log")
        public void doLogging() {
            doLog(Resource2.LOG1, Resource2.LOG2);
        }

    }
    
    @Ignore
    @Path("/storage")
    public static class Resource3 {
        private static final Logger LOG1 = LogUtils.getL7dLogger(Resource3.class);
        private static final Logger LOG2 = LogUtils.getL7dLogger(Resource3.class, null, "theStorageLogger");
        
        @GET
        @Path("/log")
        public void doLogging() {
            doLog(Resource3.LOG1, Resource3.LOG2);
        }

    }
    
    @Path("/")
    public static class AtomServerReset {
        
        private AtomPullServer server;
        
        public void setServer(AtomPullServer s) {
            this.server = s;
        }
        
        
        @POST
        public void reset() {
            server.reset();
        }

    }
    
    @Ignore
    public static class ExternalStorage implements ReadableLogStorage {

        private List<org.apache.cxf.management.web.logging.LogRecord> records = 
            new LinkedList<org.apache.cxf.management.web.logging.LogRecord>();
        
        public ExternalStorage() {
            addRecord("org.apache.cxf.systest.jaxrs.JAXRSLoggingAtomPullSpringTest$Resource4", 
                      Level.SEVERE, null);
            addRecord("org.apache.cxf.systest.jaxrs.JAXRSLoggingAtomPullSpringTest$Resource4", 
                      Level.WARNING, null);
            addRecord("org.apache.cxf.systest.jaxrs.JAXRSLoggingAtomPullSpringTest$Resource4", 
                      Level.INFO, null);
            addRecord("org.apache.cxf.systest.jaxrs.JAXRSLoggingAtomPullSpringTest$Resource4",
                      Level.FINE, new IllegalArgumentException());
            addRecord("org.apache.cxf.systest.jaxrs.JAXRSLoggingAtomPullSpringTest$Resource4", 
                      Level.FINEST, null);
            addRecord("org.apache.cxf.systest.jaxrs.JAXRSLoggingAtomPullSpringTest$Resource4", 
                      Level.FINER, null);
            addRecord("readOnlyStorageLogger", Level.SEVERE, null);
            addRecord("readOnlyStorageLogger", Level.SEVERE, null);
        }
        
        private void addRecord(String loggerName, Level level, Throwable t) {
            org.apache.cxf.management.web.logging.LogRecord lr = 
                new org.apache.cxf.management.web.logging.LogRecord();
            lr.setLoggerName(loggerName);
            lr.setLevel(LogLevel.fromJUL(level));
            if (t != null) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                lr.setThrowable(sw.getBuffer().toString());
            }
            records.add(lr);
        }
        
        public void close() {
            // TODO Auto-generated method stub
            
        }

        public int getSize() {
            // this storage is getting the records from a file log entries are added to
            return -1;
        }

        public int load(List<org.apache.cxf.management.web.logging.LogRecord> list, 
                        SearchCondition<org.apache.cxf.management.web.logging.LogRecord> condition, 
                        int pageNumber, 
                        int pageSize) {
            int loadFrom = pageNumber == 1 ? 0 : pageSize * (pageNumber - 1);
            for (int i = loadFrom; i < loadFrom + pageSize; i++) {
                if (condition.isMet(records.get(i))) {
                    list.add(records.get(i));
                }
            }
            return pageNumber;
        }
        
    }
    
    @Ignore
    public static class Storage implements ReadWriteLogStorage {
        private static List<org.apache.cxf.management.web.logging.LogRecord> records = 
            new LinkedList<org.apache.cxf.management.web.logging.LogRecord>();

        public int load(List<org.apache.cxf.management.web.logging.LogRecord> list,
                         SearchCondition<org.apache.cxf.management.web.logging.LogRecord> sc,
                         int pageNumber, int pageSize) {
            
            int loadFrom = pageNumber == 1 ? 0 : pageSize * (pageNumber - 1);
            if (loadFrom + pageSize <= records.size()) {
                list.addAll(records.subList(loadFrom, loadFrom + pageSize));
            }
            return pageNumber;
        }

        public void save(List<org.apache.cxf.management.web.logging.LogRecord> list) {
            records.addAll(list);
        }
        
        public void clear() {
        }

        public void close() {
        }

        public int getSize() {
            return records.size();
        }

        public static List<org.apache.cxf.management.web.logging.LogRecord> getRecords() {
            return records;
        }
        
        public static void clearRecords() {
            records.clear();
        }
    }
    
    
    private static void doLog(Logger l1, Logger l2) {
        
        // 3 severe, 1 warning, 1 info and 3 debug messages 
        
        // 1
        l1.severe("severe message");
        // 2
        l1.warning("warning message");
        // 3
        l1.info("info message");
        LogRecord r = new LogRecord(Level.FINE, "fine message");
        if ("Resource4".equals(l1.getName())) {
            r.setLoggerName(l1.getName());
        }
        r.setThrown(new IllegalArgumentException("tadaam"));
        // 4
        l1.log(r);
        r = new LogRecord(Level.FINER, "finer message with {0} and {1}");
        r.setParameters(new Object[] {
            "param1", "param2"
        });
        r.setLoggerName("faky-logger");
        // 5
        l1.log(r);
        // 6
        l1.finest("finest message");

        // for LOG2 only 'warning' and above messages should be logged
        // 7
        l2.severe("severe message");
        // 8
        l2.severe("severe message2");
        l2.info("info message - should not pass!");
        l2.finer("finer message - should not pass!");
        
    }
    
    private org.apache.cxf.management.web.logging.LogRecord readLogRecord(String value) throws Exception {
        return (org.apache.cxf.management.web.logging.LogRecord)
            context.createUnmarshaller().unmarshal(new StringReader(value));
    }
    
    
    private void updateCounters(org.apache.cxf.management.web.logging.LogRecord record, 
                                String clsName,
                                String namedLoggerName) {
        String name = record.getLoggerName();
        if (name != null && name.length() > 0) {
            if (("org.apache.cxf.systest.jaxrs.JAXRSLoggingAtomPullSpringTest$" + clsName).equals(name)) {
                resourceLogger++;      
            } else if (namedLoggerName.equals(name)) {
                namedLogger++;      
            } else if ("faky-logger".equals(name)) {
                fakyLogger++;      
            }
        } 
        
        if (record.getThrowable().length() > 0) {
            throwables++;
        }
        
        LogLevel level = record.getLevel();
        if (level == LogLevel.ERROR) {
            errorLevels++;
        } else if (level == LogLevel.DEBUG) {
            debugLevels++;
        } else if (level == LogLevel.TRACE) {
            traceLevels++;    
        } else if (level == LogLevel.INFO) {
            infoLevels++;
        } else {
            warningLevels++;    
        }
    }
    
    private void resetCounters() {
        fakyLogger = 0;
        namedLogger = 0;
        resourceLogger = 0;
        throwables = 0;
        
        errorLevels = 0;
        infoLevels = 0;
        debugLevels = 0;
        traceLevels = 0;
        warningLevels = 0;
    }
    
    private void verifyCounters() {
        assertEquals(1, throwables);
        assertEquals(4, resourceLogger);
        assertEquals(2, namedLogger);
        assertEquals(1, fakyLogger);
        
        assertEquals(3, errorLevels);
        assertEquals(1, infoLevels);
        assertEquals(2, debugLevels);
        assertEquals(1, traceLevels);
        assertEquals(1, warningLevels);
    }
}
