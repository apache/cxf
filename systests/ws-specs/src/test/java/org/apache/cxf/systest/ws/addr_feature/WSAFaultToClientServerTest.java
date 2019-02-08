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
package org.apache.cxf.systest.ws.addr_feature;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.AddressingFeature;

import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.systest.ws.AbstractWSATestBase;
import org.apache.cxf.systest.ws.addr_feature.FaultToEndpointServer.HelloHandler;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WSAFaultToClientServerTest  extends AbstractWSATestBase {

    @Before
    public void setUp() throws Exception {
        createBus();
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("FaultTo server did not launch correctly", launchServer(FaultToEndpointServer.class, true));
    }

    @Test
    public void testOneWayFaultTo() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        QName serviceName = new QName("http://apache.org/hello_world_soap_http", "SOAPServiceAddressing");

        Greeter greeter = new SOAPService(wsdl, serviceName).getPort(Greeter.class, new AddressingFeature());
        EndpointReferenceType faultTo = new EndpointReferenceType();
        AddressingProperties addrProperties = new AddressingProperties();
        AttributedURIType epr = new AttributedURIType();
        String faultToAddress = "http://localhost:" + FaultToEndpointServer.FAULT_PORT  + "/faultTo";
        epr.setValue(faultToAddress);
        faultTo.setAddress(epr);
        addrProperties.setFaultTo(faultTo);

        BindingProvider provider = (BindingProvider) greeter;
        Map<String, Object> requestContext = provider.getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                           "http://localhost:" + FaultToEndpointServer.PORT + "/jaxws/greeter");
        requestContext.put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES, addrProperties);

        greeter.greetMeOneWay("test");
        //wait for the fault request
        int i = 2;
        while (HelloHandler.getFaultRequestPath() == null && i > 0) {
            Thread.sleep(500);
            i--;
        }
        assertTrue("FaultTo request fpath isn't expected",
                   "/faultTo".equals(HelloHandler.getFaultRequestPath()));
    }

    @Test
    public void testTwoWayFaultTo() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        AddNumbersPortType port = getTwoWayPort();

        //setup a real decoupled endpoint that will process the fault correctly
        HTTPConduit c = (HTTPConduit)ClientProxy.getClient(port).getConduit();
        c.getClient().setDecoupledEndpoint("http://localhost:" + FaultToEndpointServer.FAULT_PORT2 + "/sendFaultHere");

        EndpointReferenceType faultTo = new EndpointReferenceType();
        AddressingProperties addrProperties = new AddressingProperties();
        AttributedURIType epr = new AttributedURIType();
        epr.setValue("http://localhost:" + FaultToEndpointServer.FAULT_PORT2 + "/sendFaultHere");
        faultTo.setAddress(epr);
        addrProperties.setFaultTo(faultTo);

        BindingProvider provider = (BindingProvider) port;
        Map<String, Object> requestContext = provider.getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                           "http://localhost:" + FaultToEndpointServer.PORT + "/jaxws/add");
        requestContext.put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES, addrProperties);

        try {
            port.addNumbers(-1, -2);
            fail("Exception is expected");
        } catch (Exception e) {
            //do nothing
        }

        String in = new String(input.toByteArray());
        //System.out.println(in);
        assertTrue("The response from faultTo endpoint is expected and actual response is " + in,
                   in.indexOf("Address: http://localhost:" + FaultToEndpointServer.FAULT_PORT2
                                     + "/sendFaultHere") > -1);
        assertTrue("WS addressing header is expected",
                   in.indexOf("http://www.w3.org/2005/08/addressing") > -1);
        assertTrue("Fault deatil is expected",
                   in.indexOf("Negative numbers cant be added") > -1);

        ((Closeable)port).close();
    }

    private AddNumbersPortType getTwoWayPort() throws Exception {
        URL wsdl = getClass().getResource("/wsdl_systest_wsspec/add_numbers.wsdl");
        assertNotNull("WSDL is null", wsdl);
        QName serviceName = new QName("http://apache.org/cxf/systest/ws/addr_feature/", "AddNumbersService");
        AddNumbersService service = new AddNumbersService(wsdl, serviceName);
        assertNotNull("Service is null ", service);
        return service.getAddNumbersPort(new AddressingFeature());
    }


}
