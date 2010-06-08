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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.ext.atom.AbstractEntryBuilder;
import org.apache.cxf.jaxrs.ext.atom.AbstractFeedBuilder;
import org.apache.cxf.jaxrs.provider.AtomEntryProvider;
import org.apache.cxf.jaxrs.provider.AtomFeedProvider;
import org.apache.cxf.management.web.logging.atom.AtomPushHandler;
import org.apache.cxf.management.web.logging.atom.converter.Converter;
import org.apache.cxf.management.web.logging.atom.converter.StandardConverter;
import org.apache.cxf.management.web.logging.atom.converter.StandardConverter.Format;
import org.apache.cxf.management.web.logging.atom.converter.StandardConverter.Multiplicity;
import org.apache.cxf.management.web.logging.atom.converter.StandardConverter.Output;
import org.apache.cxf.management.web.logging.atom.deliverer.Deliverer;
import org.apache.cxf.management.web.logging.atom.deliverer.WebClientDeliverer;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSLoggingAtomPushTest extends Assert {
    public static final String PORT = TestUtil.getPortNumber(JAXRSLoggingAtomPushTest.class);
    
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSLoggingAtomPushTest.class);
    private static Server server;
    
    
    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void beforeClass() throws Exception {
        // disable logging for server startup
        configureLogging("resources/logging_atompush_disabled.properties");

        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(JAXRSLoggingAtomPushTest.Resource.class);
        sf.setAddress("http://localhost:" + PORT + "/");
        sf.setProviders(Arrays.asList(new AtomFeedProvider(), new AtomEntryProvider()));
        server = sf.create();
        server.start();
    }

    /** Configures global logging */
    private static void configureLogging(String propFile) throws Exception {
        LogManager lm = LogManager.getLogManager();
        InputStream ins = JAXRSLoggingAtomPushTest.class.getResourceAsStream(propFile);
        String s = IOUtils.readStringFromStream(ins);
        ins.close();
        s = s.replaceAll("9080", PORT);
        lm.readConfiguration(new ByteArrayInputStream(s.getBytes("UTF-8")));
    }

    private static void logSixEvents(Logger log) {
        log.severe("severe message");
        log.warning("warning message");
        log.info("info message");
        LogRecord r = new LogRecord(Level.FINE, "fine message");
        r.setThrown(new IllegalArgumentException("tadaam"));
        log.log(r);
        r = new LogRecord(Level.FINER, "finer message with {0} and {1}");
        r.setParameters(new Object[] {
            "param1", "param2"
        });
        r.setLoggerName("faky-logger");
        log.log(r);
        log.finest("finest message");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.destroy();
        }
        LogManager lm = LogManager.getLogManager();
        try {
            // restoring original configuration to not use tested logging handlers
            lm.readConfiguration();
        } catch (Exception e) {
            // ignore missing config file
        }
    }

    @Before
    public void before() throws Exception {
        Resource.clear();
    }

    @Test
    public void testPrivateLogger() throws Exception {
        configureLogging("resources/logging_atompush_disabled.properties");
        Logger log = LogUtils.getL7dLogger(JAXRSLoggingAtomPushTest.class, null, "private-log");
        Converter c = new StandardConverter(Output.FEED, Multiplicity.ONE, Format.CONTENT);
        Deliverer d = new WebClientDeliverer("http://localhost:" + PORT);
        Handler h = new AtomPushHandler(2, c, d);
        log.addHandler(h);
        log.setLevel(Level.ALL);
        logSixEvents(log);
        // need to wait: multithreaded and client-server journey
        Thread.sleep(3000);
        // 6 events / 2 element batch = 3 feeds expected
        assertEquals("Different logged events count;", 3, Resource.feeds.size());
    }
    
    @Test
    public void testPrivateLoggerCustomBuilders() throws Exception {
        configureLogging("resources/logging_atompush_disabled.properties");
        Logger log = LogUtils.getL7dLogger(JAXRSLoggingAtomPushTest.class, null, "private-log");
        AbstractFeedBuilder<List<org.apache.cxf.management.web.logging.LogRecord>> fb = 
            createCustomFeedBuilder();
        AbstractEntryBuilder<List<org.apache.cxf.management.web.logging.LogRecord>> eb =
            createCustomEntryBuilder(); 
        Converter c = new StandardConverter(Output.FEED, Multiplicity.ONE, Format.CONTENT, fb, eb);
        Deliverer d = new WebClientDeliverer("http://localhost:" + PORT);
        Handler h = new AtomPushHandler(2, c, d);
        log.addHandler(h);
        log.setLevel(Level.ALL);
        logSixEvents(log);
        // need to wait: multithreaded and client-server journey
        Thread.sleep(3000);
        // 6 events / 2 element batch = 3 feeds expected
        assertEquals("Different logged events count;", 3, Resource.feeds.size());
    }
    
    @Test
    public void testOneElementBatch() throws Exception {
        configureLogging("resources/logging_atompush.properties");
        logSixEvents(LOG);
        // need to wait: multithreaded and client-server journey
        Thread.sleep(3000);
        assertEquals("Different logged events count;", 6, Resource.feeds.size());
    }

    @Test
    public void testMultiElementBatch() throws Exception {
        configureLogging("resources/logging_atompush_batch.properties");
        logSixEvents(LOG);
        // need to wait: multithreaded and client-server journey
        Thread.sleep(3000);
        // 6 events / 3 element batch = 2 feeds expected
        assertEquals("Different logged events count;", 2, Resource.feeds.size());
    }

    
    
    @Ignore
    private AbstractFeedBuilder<List<org.apache.cxf.management.web.logging.LogRecord>> 
    createCustomFeedBuilder() {

        AbstractFeedBuilder<List<org.apache.cxf.management.web.logging.LogRecord>> fb = 
            new AbstractFeedBuilder<List<org.apache.cxf.management.web.logging.LogRecord>>() {
                @Override
                public String getAuthor(List<org.apache.cxf.management.web.logging.LogRecord> pojo) {
                    return "custom author";
                }
            };
        return fb; 
    }  

    @Ignore
    private AbstractEntryBuilder<List<org.apache.cxf.management.web.logging.LogRecord>> 
    createCustomEntryBuilder() {
        AbstractEntryBuilder<List<org.apache.cxf.management.web.logging.LogRecord>> eb = 
            new AbstractEntryBuilder<List<org.apache.cxf.management.web.logging.LogRecord>>() {
                @Override
                public String getSummary(List<org.apache.cxf.management.web.logging.LogRecord> pojo) {
                    return "custom summary";
                }
            };
        return eb;
    }

    @Test
    public void testAtomPubEntries() throws Exception {
        configureLogging("resources/logging_atompush_atompub.properties");
        logSixEvents(LOG);
        // need to wait: multithreaded and client-server journey
        Thread.sleep(3000);
        // 6 events logged as entries
        assertEquals("Different logged events count;", 6, Resource.entries.size());
    }

    @Ignore
    @Path("/")
    public static class Resource {
        
        private static Queue<Feed> feeds = new ConcurrentLinkedQueue<Feed>();
        private static Queue<Entry> entries = new ConcurrentLinkedQueue<Entry>();
        
        @POST
        public void consume(Feed feed) {
            feeds.add(feed);
        }

        @POST
        @Path("/atomPub")
        public void consume(Entry entry) {
            entries.add(entry);
        }
        
        public static void clear() {
            feeds.clear();
            entries.clear();
        }
    }

}
