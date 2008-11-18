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

package org.apache.cxf.systest.http_jetty.continuations;

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
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClientServerWrappedContinuationTest extends AbstractClientServerTestBase {

    private static final String CLIENT_CONFIG_FILE =
        "org/apache/cxf/systest/http_jetty/continuations/cxf.xml";
    private static final String SERVER_CONFIG_FILE =
        "org/apache/cxf/systest/http_jetty/continuations/jaxws-server.xml";
    
    public static class Server extends AbstractBusTestServerBase {

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus bus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(bus);
            
            Object implementor = new HelloImplWithWrapppedContinuation();
            String address = "http://localhost:9092/hellocontinuation";
            Endpoint.publish(address, implementor);
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }

    @Test
    public void testHttpWrappedContinuatuions() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf.createBus(CLIENT_CONFIG_FILE);
        BusFactory.setDefaultBus(bus);
        
        QName serviceName = new QName("http://cxf.apache.org/systest/jaxws", "HelloContinuationService");
        
        URL wsdlURL = new URL("http://localhost:9092/hellocontinuation?wsdl");
        
        HelloContinuationService service = new HelloContinuationService(wsdlURL, serviceName);
        assertNotNull(service);
        final HelloContinuation helloPort = service.getHelloContinuationPort();
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 0, TimeUnit.SECONDS,
                                                             new ArrayBlockingQueue<Runnable>(6));
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch controlDoneSignal = new CountDownLatch(5);
        CountDownLatch helloDoneSignal = new CountDownLatch(5);
        
        executor.execute(new ControlWorker(helloPort, "Fred", startSignal, controlDoneSignal));
        executor.execute(new HelloWorker(helloPort, "Fred", "", startSignal, helloDoneSignal));
        
        executor.execute(new ControlWorker(helloPort, "Barry", startSignal, controlDoneSignal));
        executor.execute(new HelloWorker(helloPort, "Barry", "Jameson", startSignal, helloDoneSignal));
        
        executor.execute(new ControlWorker(helloPort, "Harry", startSignal, controlDoneSignal));
        executor.execute(new HelloWorker(helloPort, "Harry", "", startSignal, helloDoneSignal));
        
        executor.execute(new ControlWorker(helloPort, "Rob", startSignal, controlDoneSignal));
        executor.execute(new HelloWorker(helloPort, "Rob", "Davidson", startSignal, helloDoneSignal));
        
        executor.execute(new ControlWorker(helloPort, "James", startSignal, controlDoneSignal));
        executor.execute(new HelloWorker(helloPort, "James", "ServiceMix", startSignal, helloDoneSignal));
        
        startSignal.countDown();
        
        controlDoneSignal.await(10, TimeUnit.SECONDS);
        helloDoneSignal.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();
        assertEquals("Not all invocations have been resumed", 0, controlDoneSignal.getCount());
        assertEquals("Not all invocations have completed", 0, helloDoneSignal.getCount());
    }
    
    
}
