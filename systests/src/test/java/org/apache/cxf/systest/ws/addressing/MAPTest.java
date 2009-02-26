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

package org.apache.cxf.systest.ws.addressing;

import java.lang.reflect.UndeclaredThrowableException;
import org.apache.hello_world_soap_http.BadRecordLitFault;
import org.junit.Test;


/**
 * Tests the addition of WS-Addressing Message Addressing Properties.
 */
public class MAPTest extends MAPTestBase {

    private static final String CONFIG;
    static {
        CONFIG = "org/apache/cxf/systest/ws/addressing/cxf" 
            + (("HP-UX".equals(System.getProperty("os.name"))
                || "Windows XP".equals(System.getProperty("os.name"))) ? "-hpux" : "")
            + ".xml";
    }
    
    public String getConfigFileName() {
        return CONFIG;
    }
    
    
    @Test
    public void testUsingKeepAliveConnection() throws Exception {
        if (!"HP-UX".equals(System.getProperty("os.name"))) {
            return;
        }
        int n = 100;
        for (int i = 0; i < n; i++) {
            greeter.greetMeOneWay("oneway on keep-alive connection");
        }
        for (int i = 0; i < n; i++) {
            assertNotNull(greeter.greetMe("twoway on keep-alive connection"));
        }
        for (int i = 0; i < 0; i++) {
            try {
                greeter.testDocLitFault("BadRecordLitFault");
                fail("expected fault from service");
            } catch (BadRecordLitFault brlf) {
                //checkVerification();
            } catch (UndeclaredThrowableException ex) {
                throw (Exception)ex.getCause();
            }
        }
    }

    public void testFallbackThreadPoolConfig() throws Exception { 
        Runnable r = new Runnable() {
            public void run() {
                greeter.greetMeLater(5 * 1000);
            }
        };
        Thread[] invokers = new Thread[5];
        long start = System.currentTimeMillis();
        for (int i = 0; i < invokers.length; i++) {
            invokers[i] = new Thread(r);
            invokers[i].setDaemon(true);
            invokers[i].start();
        }
        for (int i = 0; i < invokers.length; i++) {
            invokers[i].join(15 * 1000);
        }
        long end = System.currentTimeMillis();
        assertTrue("unexpected duration: " + (end - start),
                   end - start > 9 * 1000L);
    }

}

