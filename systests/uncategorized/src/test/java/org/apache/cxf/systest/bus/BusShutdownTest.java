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

package org.apache.cxf.systest.bus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.apache.hello_world_soap_http.SOAPService;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class BusShutdownTest {
    public static final String PORT = TestUtil.getPortNumber(BusShutdownTest.class);

    @Test
    public void testStartWorkShutdownWork() throws Exception {


        URL wsdlUrl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull("wsdl resource was not found", wsdlUrl);
        // Since this test always fails in linux box, try to use other port
        String serverAddr = "http://localhost:" + PORT + "/greeter/port";
        makeTwoWayCallOnNewBus(wsdlUrl, serverAddr);


        // verify sutdown cleans the slate and reverts to null state
        //
        // This test should not need to workaroundHangWithDifferentAddr,
        // and when using ADDR, the test should not need:
        //  org.apache.cxf.transports.http_jetty.DontClosePort
        //
        String workaroundHangWithDifferentAddr = serverAddr;

        // reusing same address will cause failure, hang on client invoke
        //possibleWorkaroundHandWithDifferentAddr = ADDR.replace('8', '9');
        makeTwoWayCallOnNewBus(wsdlUrl, workaroundHangWithDifferentAddr);
    }

    private void makeTwoWayCallOnNewBus(URL wsdlUrl, String address) {
        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        Endpoint ep = createService(address);
        doWork(wsdlUrl, address);
        // this should revert the JVM to its original state pending gc
        ep.stop();
        bus.shutdown(true);
    }

    private void doWork(URL wsdlUrl, String address) {
        SOAPService service = new SOAPService(wsdlUrl);
        assertNotNull(service);
        Greeter greeter = service.getSoapPort();

        // overwrite client address
        InvocationHandler handler = Proxy.getInvocationHandler(greeter);
        BindingProvider bp = (BindingProvider)handler;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                   address);
        Client client = ClientProxy.getClient(greeter);
        HTTPConduit c = (HTTPConduit)client.getConduit();
        c.setClient(new HTTPClientPolicy());
        c.getClient().setConnection(ConnectionType.CLOSE);

        // invoke twoway call
        greeter.sayHi();
    }

    private Endpoint createService(String address) {
        Object impl = new GreeterImpl();
        return Endpoint.publish(address, impl);
    }
}