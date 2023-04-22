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

import java.io.StringReader;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.systest.ws.common.DoubleItPortTypeImpl;
import org.apache.cxf.systest.ws.common.KeystorePasswordCallback;
import org.apache.cxf.systest.ws.ut.SecurityHeaderCacheInterceptor;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxOutInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.SignatureActionToken;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.HandlerAction;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A set of tests for WS-Security actions (i.e. the non WS-SecurityPolicy approach).
 */
public class ActionTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);
    public static final String PORT2 = allocatePort(Server.class, 2);

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
                launchServer(Server.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(UTServer.class, true)
        );
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @org.junit.Test
    public void test3DESEncryptionGivenKey() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleIt3DESEncryptionPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testUsernameToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUsernameTokenPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);


        // Successful call
        assertEquals(50, port.doubleIt(25));

        // This should fail, as the client is not sending a UsernameToken
        portQName = new QName(NAMESPACE, "DoubleItUsernameTokenPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        try {
            port.doubleIt(25);
            fail("Failure expected on not sending a UsernameToken element");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            assertEquals(ex.getMessage(), WSSecurityException.UNIFIED_SECURITY_ERR);
        }

        // Here the Server is adding the WSS4JInInterceptor in code
        portQName = new QName(NAMESPACE, "DoubleItUsernameTokenPort3");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, UTServer.PORT);

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testUsernameTokenReplay() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUsernameTokenPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        Client cxfClient = ClientProxy.getClient(port);
        SecurityHeaderCacheInterceptor cacheInterceptor =
            new SecurityHeaderCacheInterceptor();
        cxfClient.getOutInterceptors().add(cacheInterceptor);

        // Make two invocations with the same UsernameToken
        assertEquals(50, port.doubleIt(25));
        try {
            port.doubleIt(25);
            fail("Failure expected on a replayed UsernameToken");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            assertEquals(ex.getMessage(), WSSecurityException.UNIFIED_SECURITY_ERR);
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testUsernameTokenNoValidation() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUsernameTokenNoValPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // Successful call
        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testEncryptedPassword() throws Exception {

        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptedPasswordPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignedTimestampReplay() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignedTimestampPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        Client cxfClient = ClientProxy.getClient(port);
        SecurityHeaderCacheInterceptor cacheInterceptor =
            new SecurityHeaderCacheInterceptor();
        cxfClient.getOutInterceptors().add(cacheInterceptor);

        // Make two invocations with the same SecurityHeader
        assertEquals(50, port.doubleIt(25));
        try {
            port.doubleIt(25);
            fail("Failure expected on a replayed Timestamp");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            assertEquals(ex.getMessage(), WSSecurityException.UNIFIED_SECURITY_ERR);
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Here the client is using "Actions", where the server is using an AsymmetricBinding policy
    @org.junit.Test
    public void testAsymmetricActionToPolicy() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // Successful call
        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Here the client is using "Actions", where the server is using an AsymmetricBinding policy,
    // and we are building the service in code using JaxWsServerFactoryBean instead of Spring
    @org.junit.Test
    public void testAsymmetricActionToPolicyServerFactory() throws Exception {

        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        URL serviceWSDL = ActionTest.class.getResource("DoubleItActionPolicy.wsdl");
        svrFactory.setWsdlLocation(serviceWSDL.toString());
        String address = "http://localhost:" + PORT2 + "/DoubleItAsymmetric";
        svrFactory.setAddress(address);
        DoubleItPortTypeImpl serviceBean = new DoubleItPortTypeImpl();
        serviceBean.setEnforcePrincipal(false);
        svrFactory.setServiceBean(serviceBean);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPort");
        svrFactory.setEndpointName(portQName);

        Map<String, Object> props = new HashMap<>();
        props.put("security.callback-handler", "org.apache.cxf.systest.ws.common.KeystorePasswordCallback");
        props.put("security.signature.properties", "bob.properties");
        props.put("security.encryption.properties", "alice.properties");
        props.put("security.encryption.username", "alice");
        svrFactory.setProperties(props);

        org.apache.cxf.endpoint.Server server = svrFactory.create();

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT2);

        // Successful call
        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        server.destroy();
        bus.shutdown(true);
    }

    // Here the client is using "Actions", where the server is using an AsymmetricBinding policy
    @org.junit.Test
    public void testAsymmetricEncryptBeforeSigningActionToPolicy() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricEncryptBeforeSigningPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // Successful call
        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testEncryption() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptionPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // Successful call
        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignatureNegativeClient() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignatureNegativeClientPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        try {
            port.doubleIt(25);
            fail("Failure expected as the client doesn't trust the cert of the service");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignatureNegativeClientStreaming() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignatureNegativeClientPort2");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        try {
            port.doubleIt(25);
            fail("Failure expected as the client doesn't trust the cert of the service");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignatureNegativeServer() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignatureNegativeServerPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        try {
            port.doubleIt(25);
            fail("Failure expected as the service doesn't trust the client cert");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignatureNegativeServerStreaming() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignatureNegativeServerPort2");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        try {
            port.doubleIt(25);
            fail("Failure expected as the service doesn't trust the client cert");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignedSAML() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignedSAMLPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignatureProgrammatic() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignatureConfigPort");

        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // Programmatic interceptor
        Map<String, Object> props = new HashMap<>();
        props.put(ConfigurationConstants.ACTION, "Signature");
        props.put(ConfigurationConstants.SIGNATURE_USER, "alice");
        props.put(ConfigurationConstants.PW_CALLBACK_REF, new KeystorePasswordCallback());
        props.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        props.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");
        WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(props);
        Client client = ClientProxy.getClient(port);
        client.getOutInterceptors().add(outInterceptor);

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignatureProgrammaticStAX() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignatureConfigPort");

        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // Programmatic interceptor
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.setActions(Collections.singletonList(WSSConstants.SIGNATURE));
        properties.setSignatureUser("alice");
        properties.setCallbackHandler(new KeystorePasswordCallback());
        properties.setSignatureKeyIdentifier(WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE);
        Properties sigProperties =
            CryptoFactory.getProperties("alice.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(sigProperties);

        WSS4JStaxOutInterceptor outInterceptor = new WSS4JStaxOutInterceptor(properties);
        Client client = ClientProxy.getClient(port);
        client.getOutInterceptors().add(outInterceptor);

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignatureProgrammaticMultipleActors() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignatureConfigPort2");

        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        Client client = ClientProxy.getClient(port);

        // Add a UsernameToken for the "dave" actor
        Map<String, Object> props = new HashMap<>();
        props.put(ConfigurationConstants.ACTION, "UsernameToken");
        props.put(ConfigurationConstants.ACTOR, "dave");
        props.put(ConfigurationConstants.USER, "alice");
        props.put(ConfigurationConstants.PW_CALLBACK_REF, new KeystorePasswordCallback());
        WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(props);
        client.getOutInterceptors().add(outInterceptor);

        // Add a Signature for the "bob" actor - this is what the service is expecting
        Map<String, Object> props2 = new HashMap<>();
        props2.put(ConfigurationConstants.ACTION, "Signature");
        props2.put(ConfigurationConstants.ACTOR, "bob");
        props2.put(ConfigurationConstants.SIGNATURE_USER, "alice");
        props2.put(ConfigurationConstants.PW_CALLBACK_REF, new KeystorePasswordCallback());
        props2.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        props2.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");
        outInterceptor = new WSS4JOutInterceptor(props2);
        outInterceptor.setId("WSS4JOutInterceptor2");
        client.getOutInterceptors().add(outInterceptor);

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignatureDispatchPayload() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignatureConfigPort");

        Dispatch<StreamSource> dispatch =
            service.createDispatch(portQName, StreamSource.class, Service.Mode.PAYLOAD);
        updateAddressPort(dispatch, PORT);

        // Programmatic interceptor
        Map<String, Object> props = new HashMap<>();
        props.put(ConfigurationConstants.ACTION, "Signature");
        props.put(ConfigurationConstants.SIGNATURE_USER, "alice");
        props.put(ConfigurationConstants.PW_CALLBACK_REF, new KeystorePasswordCallback());
        props.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        props.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");
        WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(props);
        Client client = ((DispatchImpl<StreamSource>) dispatch).getClient();
        client.getOutInterceptors().add(outInterceptor);

        String payload = "<ns2:DoubleIt xmlns:ns2=\"http://www.example.org/schema/DoubleIt\">"
            + "<numberToDouble>25</numberToDouble></ns2:DoubleIt>";
        StreamSource request = new StreamSource(new StringReader(payload));
        StreamSource response = dispatch.invoke(request);
        assertNotNull(response);

        Document doc = StaxUtils.read(response.getInputStream());
        assertEquals("50", doc.getElementsByTagNameNS(null, "doubledNumber").item(0).getTextContent());

        ((java.io.Closeable)dispatch).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignatureDispatchMessage() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignatureConfigPort");

        Dispatch<StreamSource> dispatch =
            service.createDispatch(portQName, StreamSource.class, Service.Mode.MESSAGE);
        updateAddressPort(dispatch, PORT);

        // Programmatic interceptor
        Map<String, Object> props = new HashMap<>();
        props.put(ConfigurationConstants.ACTION, "Signature");
        props.put(ConfigurationConstants.SIGNATURE_USER, "alice");
        props.put(ConfigurationConstants.PW_CALLBACK_REF, new KeystorePasswordCallback());
        props.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        props.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");
        WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(props);
        Client client = ((DispatchImpl<StreamSource>) dispatch).getClient();
        client.getOutInterceptors().add(outInterceptor);

        String payload = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Header></soap:Header><soap:Body>"
            + "<ns2:DoubleIt xmlns:ns2=\"http://www.example.org/schema/DoubleIt\">"
            + "<numberToDouble>25</numberToDouble></ns2:DoubleIt>"
            + "</soap:Body></soap:Envelope>";
        StreamSource request = new StreamSource(new StringReader(payload));
        StreamSource response = dispatch.invoke(request);
        assertNotNull(response);

        Document doc = StaxUtils.read(response.getInputStream());
        assertEquals("50", doc.getElementsByTagNameNS(null, "doubledNumber").item(0).getTextContent());

        ((java.io.Closeable)dispatch).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignatureHandlerActions() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignatureConfigPort");

        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // Programmatic interceptor
        Map<String, Object> props = new HashMap<>();
        HandlerAction signatureAction = new HandlerAction();
        signatureAction.setAction(WSConstants.SIGN);

        SignatureActionToken actionToken = new SignatureActionToken();
        actionToken.setUser("alice");
        actionToken.setKeyIdentifierId(WSConstants.BST_DIRECT_REFERENCE);

        Properties cryptoProperties = CryptoFactory.getProperties("alice.properties", this.getClass().getClassLoader());
        Crypto crypto =
            CryptoFactory.getInstance(cryptoProperties, this.getClass().getClassLoader(), null);
        actionToken.setCrypto(crypto);
        signatureAction.setActionToken(actionToken);

        List<HandlerAction> actions = Collections.singletonList(signatureAction);

        props.put(WSHandlerConstants.HANDLER_ACTIONS, actions);
        props.put(ConfigurationConstants.PW_CALLBACK_REF, new KeystorePasswordCallback());
        WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(props);
        Client client = ClientProxy.getClient(port);
        client.getOutInterceptors().add(outInterceptor);

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
}
