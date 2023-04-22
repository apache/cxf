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

package org.apache.cxf.systest.soap;

import java.io.StringReader;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.ClientImpl.IllegalEmptyResponseException;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world_xml_http.bare.XMLService;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.Assert;
import org.junit.BeforeClass;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test what happens when we make an invocation and get back an empty SOAP Body (see CXF-7653)
 */
public class EmptySOAPBodyTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(EmptySOAPBodyServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(EmptySOAPBodyServer.class, true)
        );
        assertTrue("Server failed to launch",
                   launchServer(EmptySoapProviderServer.class, true));
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @org.junit.Test
    public void testPlaintext() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = EmptySOAPBodyTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = EmptySOAPBodyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItPlaintextPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        try {
            port.doubleIt(25);
            fail("Should have thown an exception");
        } catch (SOAPFaultException t) {
            assertTrue("Wrong exception cause " + t.getCause(),
                 t.getCause() instanceof IllegalEmptyResponseException);
        }

        ((java.io.Closeable)port).close();

        bus.shutdown(true);
    }

    @org.junit.Test
    public void testProviderSource() throws Exception {
        QName providerServiceName = new QName("http://apache.org/hello_world_xml_http/bare",
                                              "HelloProviderService");

        QName providerPortName = new QName("http://apache.org/hello_world_xml_http/bare", "HelloProviderPort");

        URL wsdl = new URL("http://localhost:" + EmptySoapProviderServer.REG_PORT
                           + "/helloProvider/helloPort?wsdl");
        assertNotNull(wsdl);

        XMLService service = new XMLService(wsdl, providerServiceName, new LoggingFeature());
        assertNotNull(service);
        Dispatch<Source> dispatch = service.createDispatch(providerPortName, Source.class,
                                                           jakarta.xml.ws.Service.Mode.PAYLOAD);

        String str = new String("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
                              + "<ns2:in xmlns=\"http://apache.org/hello_world_xml_http/bare/types\""
                              + " xmlns:ns2=\"http://apache.org/hello_world_xml_http/bare\">"
                              + "<elem1>empty</elem1><elem2>this is element 2</elem2><elem3>42</elem3></ns2:in>"
                              + "</soap:Body></soap:Envelope>");
        StreamSource req = new StreamSource(new StringReader(str));
        Source resSource = dispatch.invoke(req);
        Assert.assertNull("null result is expected", resSource);
    }

}