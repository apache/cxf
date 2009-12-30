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
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.xml.bind.JAXBContext;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.AtomFeedProvider;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSLoggingAtomPullSpringTest extends AbstractClientServerTestBase {

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
        public SpringServer() {
            super("/jaxrs_logging_atompull");
        }
    }

    @Before
    public void before() throws Exception {
        context = JAXBContext.newInstance(org.apache.cxf.jaxrs.ext.logging.LogRecord.class);
    }

    @Test
    public void testFeed() throws Exception {
        WebClient wc = WebClient.create("http://localhost:9080/resource/root");
        wc.path("/log").get();
        Thread.sleep(3000);
        
        WebClient wc2 = WebClient.create("http://localhost:9080/atom/logs",
                                         Collections.singletonList(new AtomFeedProvider()));
        Feed feed = wc2.accept("application/atom+xml").get(Feed.class);
        feed.toString();
        assertEquals(8, feed.getEntries().size());
        
        resetCounters();
        for (Entry e : feed.getEntries()) {
            updateCounters(readLogRecord(e.getContent()), "Resource");
        }
        
        verifyCounters();
    }
    
    
    @Ignore
    @Path("/root")
    public static class Resource {
        private static final Logger LOG1 = LogUtils.getL7dLogger(Resource.class);
        private static final Logger LOG2 = LogUtils.getL7dLogger(Resource.class, null, "namedLogger");
        
        @GET
        @Path("/log")
        public void doLogging() {
            doLog(LOG1, LOG2);
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
    
    private org.apache.cxf.jaxrs.ext.logging.LogRecord readLogRecord(String value) throws Exception {
        return (org.apache.cxf.jaxrs.ext.logging.LogRecord)
            context.createUnmarshaller().unmarshal(new StringReader(value));
    }
    
    
    private void updateCounters(org.apache.cxf.jaxrs.ext.logging.LogRecord record, String clsName) {
        String name = record.getLoggerName();
        if (name != null && name.length() > 0) {
            if (("org.apache.cxf.systest.jaxrs.JAXRSLoggingAtomPullSpringTest$" + clsName).equals(name)) {
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
