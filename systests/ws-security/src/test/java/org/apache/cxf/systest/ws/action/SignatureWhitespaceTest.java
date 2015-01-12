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

package org.apache.cxf.systest.ws.action;

import java.io.File;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * A test for CXF-5679.
 */
public class SignatureWhitespaceTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(SignatureServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(SignatureServer.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void testNormalSignedSOAPBody() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SignatureWhitespaceTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = SignatureWhitespaceTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignaturePort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        port.doubleIt(25);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTrailingWhitespaceInSOAPBody() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SignatureWhitespaceTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = SignatureWhitespaceTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignaturePort2");

        Dispatch<StreamSource> dispatch = 
            service.createDispatch(portQName, StreamSource.class, Service.Mode.MESSAGE);
        
        // Creating a DOMSource Object for the request
        
        URL requestFile = 
            SignatureWhitespaceTest.class.getResource("request-with-trailing-whitespace.xml");
        
        StreamSource request = new StreamSource(new File(requestFile.getPath()));

        updateAddressPort(dispatch, PORT);
        
        // Make a successful request
        StreamSource response = dispatch.invoke(request);
        assertNotNull(response);
    }
    
    @org.junit.Test
    public void testAddedCommentsInSOAPBody() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SignatureWhitespaceTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = SignatureWhitespaceTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignaturePort2");

        Dispatch<StreamSource> dispatch = 
            service.createDispatch(portQName, StreamSource.class, Service.Mode.MESSAGE);
        
        // Creating a DOMSource Object for the request
        
        URL requestFile = 
            SignatureWhitespaceTest.class.getResource("request-with-comment.xml");
        
        StreamSource request = new StreamSource(new File(requestFile.getPath()));

        updateAddressPort(dispatch, PORT);
        
        // Make a successful request
        StreamSource response = dispatch.invoke(request);
        assertNotNull(response);
    }
}
