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

package org.apache.cxf.systest.http_jetty;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests thread pool config.
 */

public class ThreadPoolTest extends AbstractClientServerTestBase {
    private static final QName SERVICE_NAME = 
        new QName("http://apache.org/hello_world_soap_http", "SOAPServiceAddressing");

    private Greeter greeter;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(Server.class, false));
    }

    @Before
    public void setUp() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        greeter = new SOAPService(wsdl, SERVICE_NAME).getPort(Greeter.class);
        BindingProvider bp = (BindingProvider)greeter;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                   Server.ADDRESS);
    }

    @Test
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
            long end = System.currentTimeMillis();
            if ((end - start) > (10 * 1000L)) {
                return;
            }
        }
        long end = System.currentTimeMillis();
        assertTrue("unexpected duration: " + (end - start),
                   end - start > 10 * 1000L);
    }
}
