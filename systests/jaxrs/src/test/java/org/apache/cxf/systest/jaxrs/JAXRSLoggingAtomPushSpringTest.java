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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.apache.abdera.model.Element;
import org.apache.abdera.model.Feed;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSLoggingAtomPushSpringTest extends AbstractClientServerTestBase {

    private static List<Element> retrieved = new ArrayList<Element>();

    @BeforeClass
    public static void beforeClass() throws Exception {
        // must be 'in-process' to communicate with inner class in single JVM
        // and to spawn class SpringServer w/o using main() method
        launchServer(SpringServer.class, true);
    }

    @Ignore
    public static class SpringServer extends AbstractSpringServer {
        public SpringServer() {
            super("/jaxrs_logging_atompush");
        }
    }

    @Before
    public void before() {
        retrieved.clear();
    }

    @Ignore
    @Path("/")
    public static class Resource {
        private static final Logger LOG1 = LogUtils.getL7dLogger(Resource.class);
        private static final Logger LOG2 = LogUtils.getL7dLogger(Resource.class, null, "namedLogger");

        @GET
        @Path("/log")
        public void doLogging() {
            LOG1.severe("severe message");
            LOG1.warning("warning message");
            LOG1.info("info message");
            LogRecord r = new LogRecord(Level.FINE, "fine message");
            r.setThrown(new IllegalArgumentException("tadaam"));
            LOG1.log(r);
            r = new LogRecord(Level.FINER, "finer message with {0} and {1}");
            r.setParameters(new Object[] {
                "param1", "param2"
            });
            r.setLoggerName("faky-logger");
            LOG1.log(r);
            LOG1.finest("finest message");

            // for LOG2 only 'warning' and above messages should be logged
            LOG2.severe("severe message");
            LOG2.info("info message - should not pass!");
            LOG2.finer("finer message - should not pass!");
        }

        // 2. ATOM push handler should populate logs here
        @POST
        @Path("/feed")
        public void consume(Feed feed) {
            // System.out.println(feed);
            retrieved.add(feed);
        }
    }

    @Test
    public void testLogEvents() throws Exception {
        WebClient wc = WebClient.create("http://localhost:9080");
        wc.path("/log").get();
        Thread.sleep(1000);
        assertEquals(7, retrieved.size());
    }
}
