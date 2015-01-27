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

package org.apache.cxf.systest.ws.fault;

import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPFaultException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.fault.server.ModifiedRequestServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.ws.security.util.XmlSchemaDateFormat;
import org.example.contract.doubleit.DoubleItFault;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * Some tests for modified requests
 */
public class ModifiedRequestTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(ModifiedRequestServer.class);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(ModifiedRequestServer.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void testModifiedSignedTimestamp() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ModifiedRequestTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = ModifiedRequestTest.class.getResource("DoubleItFault.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        Client cxfClient = ClientProxy.getClient(port);
        ModifiedTimestampInterceptor modifyInterceptor = 
            new ModifiedTimestampInterceptor();
        cxfClient.getOutInterceptors().add(modifyInterceptor);
        
        makeInvocation(port);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testModifiedSignature() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ModifiedRequestTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = ModifiedRequestTest.class.getResource("DoubleItFault.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        Client cxfClient = ClientProxy.getClient(port);
        ModifiedSignatureInterceptor modifyInterceptor = 
            new ModifiedSignatureInterceptor();
        cxfClient.getOutInterceptors().add(modifyInterceptor);
        
        makeInvocation(port);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testUntrustedSignature() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ModifiedRequestTest.class.getResource("client/client-untrusted.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = ModifiedRequestTest.class.getResource("DoubleItFault.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        makeInvocation(port);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testModifiedEncryptedKey() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ModifiedRequestTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = ModifiedRequestTest.class.getResource("DoubleItFault.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        Client cxfClient = ClientProxy.getClient(port);
        ModifiedEncryptedKeyInterceptor modifyInterceptor = 
            new ModifiedEncryptedKeyInterceptor();
        cxfClient.getOutInterceptors().add(modifyInterceptor);
        
        makeInvocation(port);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testModifiedEncryptedSOAPBody() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ModifiedRequestTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = ModifiedRequestTest.class.getResource("DoubleItFault.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        Client cxfClient = ClientProxy.getClient(port);
        ModifiedEncryptedSOAPBody modifyInterceptor = 
            new ModifiedEncryptedSOAPBody();
        cxfClient.getOutInterceptors().add(modifyInterceptor);
        
        makeInvocation(port);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    private void makeInvocation(DoubleItPortType port) throws DoubleItFault {
        try {
            port.doubleIt(25);
            fail("Expected failure on a modified request");
        } catch (SOAPFaultException ex) {
            SOAPFault fault = ex.getFault();
<<<<<<< HEAD
            assertEquals("soap:Sender", fault.getFaultCode());
            assertEquals("The signature or decryption was invalid", fault.getFaultString());
            Iterator<?> subcodeIterator = fault.getFaultSubcodes();
            assertTrue(subcodeIterator.hasNext());
            Object subcode = subcodeIterator.next();
            assertEquals(WSConstants.FAILED_CHECK, subcode);
            assertFalse(subcodeIterator.hasNext());
=======
            if (streaming) {
                assertTrue("soap:Sender".equals(fault.getFaultCode())
                           || "soap:Receiver".equals(fault.getFaultCode()));
                assertTrue(fault.getFaultString().contains(WSSecurityException.UNIFIED_SECURITY_ERR));
                Iterator<?> subcodeIterator = fault.getFaultSubcodes();
                assertFalse(subcodeIterator.hasNext());
            } else {
                assertTrue(fault.getFaultCode().endsWith("Sender"));
                assertEquals(fault.getFaultString(), WSSecurityException.UNIFIED_SECURITY_ERR);
                Iterator<?> subcodeIterator = fault.getFaultSubcodes();
                assertTrue(subcodeIterator.hasNext());
                Object subcode = subcodeIterator.next();
                assertEquals(WSSecurityException.SECURITY_ERROR, subcode);
                assertFalse(subcodeIterator.hasNext());
            }
>>>>>>> 32e2daf... Fixing failing test
        }
    }
    
    private static class ModifiedTimestampInterceptor extends AbstractModifyRequestInterceptor {

        public void modifySecurityHeader(Element securityHeader) {
            if (securityHeader != null) {
                // Find the Timestamp + change it.
                
                Element timestampElement = 
                    WSSecurityUtil.findElement(securityHeader, "Timestamp", WSConstants.WSU_NS);
                Element createdValue = 
                    WSSecurityUtil.findElement(timestampElement, "Created", WSConstants.WSU_NS);
                DateFormat zulu = new XmlSchemaDateFormat();
                
                DatatypeFactory datatypeFactory;
                try {
                    datatypeFactory = DatatypeFactory.newInstance();
                    XMLGregorianCalendar createdCalendar = 
                        datatypeFactory.newXMLGregorianCalendar(createdValue.getTextContent());
                    // Add 5 seconds
                    Duration duration = datatypeFactory.newDuration(5000L);
                    createdCalendar.add(duration);
                    Date createdDate = createdCalendar.toGregorianCalendar().getTime();
                    createdValue.setTextContent(zulu.format(createdDate));
                } catch (DatatypeConfigurationException e) {
                    // TODO Auto-generated catch block
                }
            }
        }
        
        public void modifySOAPBody(Element soapBody) {
            //
        }
    }
    
    private static class ModifiedSignatureInterceptor extends AbstractModifyRequestInterceptor {

        public void modifySecurityHeader(Element securityHeader) {
            if (securityHeader != null) {
                Element signatureElement = 
                    WSSecurityUtil.findElement(securityHeader, "Signature", WSConstants.SIG_NS);
                
                Node firstChild = signatureElement.getFirstChild();
                while (!(firstChild instanceof Element) && firstChild != null) {
                    firstChild = signatureElement.getNextSibling();
                }
                ((Element)firstChild).setAttributeNS(null, "Id", "xyz");
            }
        }
        
        public void modifySOAPBody(Element soapBody) {
            //
        }
    }
    
    private static class ModifiedEncryptedKeyInterceptor extends AbstractModifyRequestInterceptor {

        public void modifySecurityHeader(Element securityHeader) {
            if (securityHeader != null) {
                Element encryptedKey = 
                    WSSecurityUtil.findElement(securityHeader, "EncryptedKey", WSConstants.ENC_NS);
                Element cipherValue = 
                    WSSecurityUtil.findElement(encryptedKey, "CipherValue", WSConstants.ENC_NS);
                String cipherText = cipherValue.getTextContent();
                
                StringBuilder stringBuilder = new StringBuilder(cipherText);
                int index = stringBuilder.length() / 2;
                char ch = stringBuilder.charAt(index);
                if (ch != 'A') {
                    ch = 'A';
                } else {
                    ch = 'B';
                }
                stringBuilder.setCharAt(index, ch);
                cipherValue.setTextContent(stringBuilder.toString());
            }
        }
        
        public void modifySOAPBody(Element soapBody) {
            //
        }
        
    }
    
    private static class ModifiedEncryptedSOAPBody extends AbstractModifyRequestInterceptor {

        public void modifySecurityHeader(Element securityHeader) {
           //
        }
        
        public void modifySOAPBody(Element soapBody) {
            if (soapBody != null) {
                Element cipherValue = 
                    WSSecurityUtil.findElement(soapBody, "CipherValue", WSConstants.ENC_NS);
                String cipherText = cipherValue.getTextContent();
                
                StringBuilder stringBuilder = new StringBuilder(cipherText);
                int index = stringBuilder.length() / 2;
                char ch = stringBuilder.charAt(index);
                if (ch != 'A') {
                    ch = 'A';
                } else {
                    ch = 'B';
                }
                stringBuilder.setCharAt(index, ch);
                cipherValue.setTextContent(stringBuilder.toString());
            }
        }
        
    }
    
}
