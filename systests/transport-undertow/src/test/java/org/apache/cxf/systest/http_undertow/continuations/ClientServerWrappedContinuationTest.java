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

package org.apache.cxf.systest.http_undertow.continuations;

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
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ClientServerWrappedContinuationTest extends AbstractClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);
    public static final String HTTPS_PORT = allocatePort(Server.class, 1);

    private static final String CLIENT_CONFIG_FILE =
        "org/apache/cxf/systest/http_undertow/continuations/cxf.xml";
    private static final String CLIENT_HTTPS_CONFIG_FILE =
        "org/apache/cxf/systest/http_undertow/continuations/cxf_https.xml";
    private static final String SERVER_CONFIG_FILE =
        "org/apache/cxf/systest/http_undertow/continuations/jaxws-server.xml";

    public static class Server extends AbstractBusTestServerBase {

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus bus = bf.createBus(SERVER_CONFIG_FILE);
            setBus(bus);
            BusFactory.setDefaultBus(bus);

            Object implementor = new HelloImplWithWrapppedContinuation();
            String address = "http://localhost:" + PORT + "/hellocontinuation";
            EndpointImpl endpointImpl = (EndpointImpl)Endpoint.publish(address, implementor);
            endpointImpl.getInInterceptors().add(new LoggingInInterceptor());
            endpointImpl.getOutInterceptors().add(new LoggingOutInterceptor());
            address = "https://localhost:" + HTTPS_PORT + "/securecontinuation";
            Endpoint.publish(address, implementor);
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }

    @Test
    public void testHttpWrappedContinuations() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf.createBus(CLIENT_CONFIG_FILE);
        BusFactory.setDefaultBus(bus);

        QName serviceName = new QName("http://cxf.apache.org/systest/jaxws", "HelloContinuationService");

        URL wsdlURL = new URL("http://localhost:" + PORT + "/hellocontinuation?wsdl");

        HelloContinuationService service = new HelloContinuationService(wsdlURL, serviceName);
        assertNotNull(service);
        final HelloContinuation helloPort = service.getHelloContinuationPort();
        ClientProxy.getClient(helloPort).getInInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(helloPort).getOutInterceptors().add(new LoggingOutInterceptor());
        doTest(helloPort);
        bus.shutdown(true);
    }

    @Test
    public void testHttpsWrappedContinuations() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf.createBus(CLIENT_HTTPS_CONFIG_FILE);
        BusFactory.setDefaultBus(bus);

        QName serviceName = new QName("http://cxf.apache.org/systest/jaxws", "HelloContinuationService");

        URL wsdlURL = new URL("https://localhost:" + HTTPS_PORT + "/securecontinuation?wsdl");

        HelloContinuationService service = new HelloContinuationService(wsdlURL, serviceName);
        assertNotNull(service);
        final HelloContinuation helloPort = service.getHelloContinuationPort();
        doTest(helloPort);
        bus.shutdown(true);
    }

    private void doTest(final HelloContinuation helloPort) throws Exception {
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

        controlDoneSignal.await(100, TimeUnit.SECONDS);
        helloDoneSignal.await(100, TimeUnit.SECONDS);
        executor.shutdownNow();
        assertEquals("Not all invocations have been resumed", 0, controlDoneSignal.getCount());
        assertEquals("Not all invocations have completed", 0, helloDoneSignal.getCount());

        helloPort.sayHi("Dan1", "to:100");
        helloPort.sayHi("Dan2", "to:100");
        helloPort.sayHi("Dan3", "to:100");
    }


}
