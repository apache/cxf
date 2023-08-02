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
import java.time.ZonedDateTime;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPFault;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.DateUtil;
import org.apache.wss4j.common.util.XMLUtils;
import org.example.contract.doubleit.DoubleItFault;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Some tests for modified requests
 */
public class ModifiedRequestTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(ModifiedRequestServer.class);
    static final String STAX_PORT = allocatePort(ModifiedRequestServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static boolean unrestrictedPoliciesInstalled =
        TestUtilities.checkUnrestrictedPoliciesInstalled();

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
        stopAllServers();
    }

    @org.junit.Test
    public void testModifiedSignedTimestamp() throws Exception {

        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ModifiedRequestTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

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

        makeInvocation(port, false);

        // Streaming invocation
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, STAX_PORT);

        cxfClient = ClientProxy.getClient(port);
        modifyInterceptor = new ModifiedTimestampInterceptor();
        cxfClient.getOutInterceptors().add(modifyInterceptor);

        makeInvocation(port, true);

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testModifiedSignature() throws Exception {

        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ModifiedRequestTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

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

        makeInvocation(port, false);

        // Streaming invocation
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, STAX_PORT);

        cxfClient = ClientProxy.getClient(port);
        modifyInterceptor = new ModifiedSignatureInterceptor();
        cxfClient.getOutInterceptors().add(modifyInterceptor);

        makeInvocation(port, true);

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testUntrustedSignature() throws Exception {

        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ModifiedRequestTest.class.getResource("client-untrusted.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ModifiedRequestTest.class.getResource("DoubleItFault.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        makeInvocation(port, false);

        // Streaming invocation
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, STAX_PORT);

        makeInvocation(port, true);

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testModifiedEncryptedKey() throws Exception {

        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ModifiedRequestTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

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

        makeInvocation(port, false);

        // Streaming invocation
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, STAX_PORT);

        cxfClient = ClientProxy.getClient(port);
        modifyInterceptor = new ModifiedEncryptedKeyInterceptor();
        cxfClient.getOutInterceptors().add(modifyInterceptor);

        makeInvocation(port, true);

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testModifiedEncryptedSOAPBody() throws Exception {

        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ModifiedRequestTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

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

        makeInvocation(port, false);

        // Streaming invocation
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, STAX_PORT);

        cxfClient = ClientProxy.getClient(port);
        modifyInterceptor = new ModifiedEncryptedSOAPBody();
        cxfClient.getOutInterceptors().add(modifyInterceptor);

        makeInvocation(port, true);

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    private void makeInvocation(DoubleItPortType port, boolean streaming) throws DoubleItFault {
        try {
            port.doubleIt(25);
            fail("Expected failure on a modified request");
        } catch (SOAPFaultException ex) {
            SOAPFault fault = ex.getFault();
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
        }
    }

    private static final class ModifiedTimestampInterceptor extends AbstractModifyRequestInterceptor {

        @Override
        public void modifySecurityHeader(Element securityHeader) {
            if (securityHeader != null) {
                // Find the Timestamp + change it.

                Element timestampElement =
                    XMLUtils.findElement(securityHeader, "Timestamp", WSS4JConstants.WSU_NS);
                Element createdValue =
                    XMLUtils.findElement(timestampElement, "Created", WSS4JConstants.WSU_NS);

                ZonedDateTime created = ZonedDateTime.parse(createdValue.getTextContent());
                // Add 5 seconds
                createdValue.setTextContent(DateUtil.getDateTimeFormatter(true).format(created.plusSeconds(5L)));
            }
        }

        public void modifySOAPBody(Element soapBody) {
            //
        }
    }

    private static final class ModifiedSignatureInterceptor extends AbstractModifyRequestInterceptor {

        @Override
        public void modifySecurityHeader(Element securityHeader) {
            if (securityHeader != null) {
                Element signatureElement =
                    XMLUtils.findElement(securityHeader, "Signature", WSS4JConstants.SIG_NS);

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

    private static final class ModifiedEncryptedKeyInterceptor extends AbstractModifyRequestInterceptor {

        @Override
        public void modifySecurityHeader(Element securityHeader) {
            if (securityHeader != null) {
                Element encryptedKey =
                    XMLUtils.findElement(securityHeader, "EncryptedKey", WSS4JConstants.ENC_NS);
                Element cipherValue =
                    XMLUtils.findElement(encryptedKey, "CipherValue", WSS4JConstants.ENC_NS);
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

    private static final class ModifiedEncryptedSOAPBody extends AbstractModifyRequestInterceptor {

        @Override
        public void modifySecurityHeader(Element securityHeader) {
           //
        }

        public void modifySOAPBody(Element soapBody) {
            if (soapBody != null) {
                Element cipherValue =
                    XMLUtils.findElement(soapBody, "CipherValue", WSS4JConstants.ENC_NS);
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
