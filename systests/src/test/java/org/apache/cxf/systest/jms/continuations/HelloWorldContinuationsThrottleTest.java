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
package org.apache.cxf.systest.jms.continuations;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.http_jetty.continuations.HelloContinuation;
import org.apache.cxf.systest.http_jetty.continuations.HelloContinuationService;
import org.apache.cxf.systest.http_jetty.continuations.HelloWorker;
import org.apache.cxf.systest.jms.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.Before;
import org.junit.Test;

public class HelloWorldContinuationsThrottleTest extends AbstractBusClientServerTestBase {
    
    private static boolean serversStarted;
    private static final String CONFIG_FILE =
        "org/apache/cxf/systest/jms/continuations/jms_test_config.xml";

    @Before
    public void startServers() throws Exception {
        if (serversStarted) {
            return;
        }
        Map<String, String> props = new HashMap<String, String>();                
        if (System.getProperty("activemq.store.dir") != null) {
            props.put("activemq.store.dir", System.getProperty("activemq.store.dir"));
        }
        props.put("java.util.logging.config.file", 
                  System.getProperty("java.util.logging.config.file"));
        
        assertTrue("server did not launch correctly", 
                   launchServer(EmbeddedJMSBrokerLauncher.class, props, null));

        assertTrue("server did not launch correctly", 
                   launchServer(Server3.class));
        serversStarted = true;
    }
    
    @Test
    public void testHttpWrappedContinuatuions() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf.createBus(CONFIG_FILE);
        BusFactory.setDefaultBus(bus);
        
        QName serviceName = new QName("http://cxf.apache.org/systest/jaxws", "HelloContinuationService");
        
        URL wsdlURL = getClass().getResource("/org/apache/cxf/systest/jms/continuations/test2.wsdl");
        
        HelloContinuationService service = new HelloContinuationService(wsdlURL, serviceName);
        assertNotNull(service);
        final HelloContinuation helloPort = service.getHelloContinuationPort();
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS,
                                                             new ArrayBlockingQueue<Runnable>(10));
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch helloDoneSignal = new CountDownLatch(5);
        
        executor.execute(new HelloWorker(helloPort, "Fred", "", startSignal, helloDoneSignal));
        startSignal.countDown();
        
        Thread.sleep(10000);
                
        executor.execute(new HelloWorker(helloPort, "Barry", "Jameson", startSignal, helloDoneSignal));
        executor.execute(new HelloWorker(helloPort, "Harry", "", startSignal, helloDoneSignal));
        executor.execute(new HelloWorker(helloPort, "Rob", "Davidson", startSignal, helloDoneSignal));
        executor.execute(new HelloWorker(helloPort, "James", "ServiceMix", startSignal, helloDoneSignal));
        
                
        helloDoneSignal.await(60, TimeUnit.SECONDS);
        executor.shutdownNow();
        System.out.println("Completed : " + (5 - helloDoneSignal.getCount()));
        assertEquals("Not all invocations have completed", 0, helloDoneSignal.getCount());
    }
        
}
