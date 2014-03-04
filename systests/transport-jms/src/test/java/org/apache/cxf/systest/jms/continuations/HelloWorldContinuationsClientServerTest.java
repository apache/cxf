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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.jms.ConnectionFactoryFeature;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class HelloWorldContinuationsClientServerTest {
    private static final String WSDL_PATH = "org/apache/cxf/systest/jms/continuations/test.wsdl";

    private static ConnectionFactoryFeature cff;

    private static Bus bus;

    @BeforeClass
    public static void startServers() throws Exception {
        bus = BusFactory.getDefaultBus();
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
        PooledConnectionFactory cfp = new PooledConnectionFactory(cf);
        cff = new ConnectionFactoryFeature(cfp);
        Object implementor = new HelloWorldWithContinuationsJMS();        
        EndpointImpl ep = (EndpointImpl)Endpoint.create(null, implementor);
        ep.setBus(bus);
        ep.getFeatures().add(cff);
        ep.publish();
    }
    
    @AfterClass
    public static void stopServers() {
        bus.shutdown(false);
    }

    @Test
    public void testHttpWrappedContinuations() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/systest/jaxws", "HelloContinuationService");
        
        URL wsdlURL = getClass().getClassLoader().getResource(WSDL_PATH);
        
        HelloContinuationService service = new HelloContinuationService(wsdlURL, serviceName);
        final HelloContinuation helloPort = service.getPort(HelloContinuation.class, cff);
        ExecutorService executor = Executors.newCachedThreadPool();
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
        Assert.assertEquals("Some invocations are still running", 0, helloDoneSignal.getCount());

    }
        
}
