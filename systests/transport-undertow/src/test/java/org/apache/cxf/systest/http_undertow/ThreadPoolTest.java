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

package org.apache.cxf.systest.http_undertow;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests thread pool config.
 */

public class ThreadPoolTest extends AbstractClientServerTestBase {
    private static final String ADDRESS = Server.ADDRESS;
    private static final QName SERVICE_NAME =
        new QName("http://apache.org/hello_world_soap_http", "SOAPServiceAddressing");

    private Greeter greeter;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(Server.class, true));
    }

    @Before
    public void setUp() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        greeter = new SOAPService(wsdl, SERVICE_NAME).getPort(Greeter.class);
        BindingProvider bp = (BindingProvider)greeter;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                   ADDRESS);
    }

    class TestRunnable implements Runnable {
        int i;
        long total;

        TestRunnable(int i) {
            this.i = i;
        }
        public void run() {
            long start = System.currentTimeMillis();
            try {
                greeter.greetMeLater(1600);
            } catch (Throwable t) {
                //ignore
            }
            long end = System.currentTimeMillis();
            total = end - start;
        }
        public long getTotal() {
            return total;
        }
    }


    @Test

    public void testFallbackThreadPoolConfig() throws Exception {
        //make sure things are running
        greeter.greetMeLater(1);
        greeter.greetMeLater(1);
        TestRunnable[] r = new TestRunnable[5];
        Thread[] invokers = new Thread[5];
        for (int i = 0; i < invokers.length; i++) {
            r[i] = new TestRunnable(i);
            invokers[i] = new Thread(r[i]);
            invokers[i].setDaemon(true);
            invokers[i].start();
        }

        int countLess = 0;
        int countMore = 0;
        for (int i = 0; i < invokers.length; i++) {
            invokers[i].join(6 * 1000);
            if (r[i].getTotal() > 3000) {
                countMore++;
            } else {
                countLess++;
            }
        }

        assertTrue(countLess >= 2 && countLess <= 3);
        assertTrue(countMore >= 2 && countMore <= 3);
    }
}
