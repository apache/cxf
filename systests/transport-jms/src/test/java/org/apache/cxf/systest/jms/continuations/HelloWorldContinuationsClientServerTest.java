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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class HelloWorldContinuationsClientServerTest extends AbstractBusClientServerTestBase {
    static EmbeddedJMSBrokerLauncher broker;
    
    private static final String CONFIG_FILE =
        "org/apache/cxf/systest/jms/continuations/jms_test_config.xml";

    
    public static class Server extends AbstractBusTestServerBase {
        EmbeddedJMSBrokerLauncher broker;
        Endpoint ep;
        public Server(EmbeddedJMSBrokerLauncher b) {
            broker = b;
        }
        
        protected void run()  {
            setBus(BusFactory.getDefaultBus());
            broker.updateWsdl(getBus(),
                              "org/apache/cxf/systest/jms/continuations/test.wsdl");
            Object implementor = new HelloWorldWithContinuationsJMS();        
            String address = "jms://";
            ep = Endpoint.publish(address, implementor);
        }
        public void tearDown() {
            ep.stop();
            ep = null;
        }

    }

    @BeforeClass
    public static void startServers() throws Exception {
        broker = new EmbeddedJMSBrokerLauncher("vm://HelloWorldContinuationsClientServerTest");
        System.setProperty("EmbeddedBrokerURL", broker.getBrokerURL());
        launchServer(broker);
        launchServer(new Server(broker));
    }
    @AfterClass
    public static void clearProperty() {
        System.clearProperty("EmbeddedBrokerURL");
    }

    @Test
    public void testHttpWrappedContinuations() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf.createBus(CONFIG_FILE);
        BusFactory.setDefaultBus(bus);
        
        QName serviceName = new QName("http://cxf.apache.org/systest/jaxws", "HelloContinuationService");
        
        URL wsdlURL = getClass().getResource("/org/apache/cxf/systest/jms/continuations/test.wsdl");
        
        HelloContinuationService service = new HelloContinuationService(wsdlURL, serviceName);
        assertNotNull(service);
        final HelloContinuation helloPort = service.getHelloContinuationPort();
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS,
                                                             new ArrayBlockingQueue<Runnable>(10));
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch helloDoneSignal = new CountDownLatch(5);
        
        executor.execute(new HelloWorker(helloPort, "Fred", "", startSignal, helloDoneSignal));
        executor.execute(new HelloWorker(helloPort, "Barry", "Jameson", startSignal, helloDoneSignal));
        executor.execute(new HelloWorker(helloPort, "Harry", "", startSignal, helloDoneSignal));
        executor.execute(new HelloWorker(helloPort, "Rob", "Davidson", startSignal, helloDoneSignal));
        executor.execute(new HelloWorker(helloPort, "James", "ServiceMix", startSignal, helloDoneSignal));
        
        startSignal.countDown();
        helloDoneSignal.await(60, TimeUnit.SECONDS);
        executor.shutdownNow();
        ((java.io.Closeable)helloPort).close();
        assertEquals("Not all invocations have completed", 0, helloDoneSignal.getCount());
        bus.shutdown(true);
    }
        
}
