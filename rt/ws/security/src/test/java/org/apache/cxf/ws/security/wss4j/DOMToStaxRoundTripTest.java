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
package org.apache.cxf.ws.security.wss4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * In these test-cases, the client is using DOM and the service is using StaX.
 */
public class DOMToStaxRoundTripTest extends AbstractSecurityTest {

    @Test
    public void testUsernameTokenText() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_TEXT);
        inProperties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationConstants.ACTION, ConfigurationConstants.USERNAME_TOKEN);
        properties.put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_TEXT);
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        properties.put(ConfigurationConstants.USER, "username");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));

        // Negative test for wrong password type
        service.getInInterceptors().remove(inhandler);

        inProperties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_DIGEST);
        inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        service.put(SecurityConstants.RETURN_SECURITY_ERROR, true);

        try {
            echo.echo("test");
            fail("Failure expected on the wrong password type");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
            String error = "The security token could not be authenticated or authorized";
            assertTrue(ex.getMessage().contains(error));
        }
    }

    @Test
    public void testUsernameTokenDigest() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_DIGEST);
        inProperties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationConstants.ACTION, ConfigurationConstants.USERNAME_TOKEN);
        properties.put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_DIGEST);
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        properties.put(ConfigurationConstants.USER, "username");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));

        // Negative test for wrong password type
        service.getInInterceptors().remove(inhandler);

        inProperties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_TEXT);
        inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);
        service.put(SecurityConstants.RETURN_SECURITY_ERROR, true);

        try {
            echo.echo("test");
            fail("Failure expected on the wrong password type");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
            String error = "The security token could not be authenticated or authorized";
            assertTrue(ex.getMessage().contains(error));
        }
    }

    @Test
    public void testEncrypt() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties =
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setDecryptionCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION);
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        properties.put(ConfigurationConstants.ENC_PROP_FILE, "outsecurity.properties");
        properties.put(ConfigurationConstants.USER, "myalias");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testEncryptionAlgorithms() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties =
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setDecryptionCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION);
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        properties.put(ConfigurationConstants.ENC_PROP_FILE, "outsecurity.properties");
        properties.put(ConfigurationConstants.USER, "myalias");
        properties.put(ConfigurationConstants.ENC_KEY_TRANSPORT, WSS4JConstants.KEYTRANSPORT_RSA15);
        properties.put(ConfigurationConstants.ENC_SYM_ALGO, WSS4JConstants.TRIPLE_DES);

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        try {
            echo.echo("test");
            fail("Failure expected as RSA v1.5 is not allowed by default");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        inProperties.setAllowRSA15KeyTransportAlgorithm(true);
        service.getInInterceptors().remove(inhandler);
        inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);
        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testEncryptUsernameToken() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties =
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setDecryptionCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.USERNAME_TOKEN + " " + ConfigurationConstants.ENCRYPTION
        );
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        properties.put(ConfigurationConstants.ENC_PROP_FILE, "outsecurity.properties");
        properties.put(ConfigurationConstants.USER, "username");
        properties.put(ConfigurationConstants.ENCRYPTION_USER, "myalias");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignature() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties =
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        properties.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        properties.put(ConfigurationConstants.USER, "myalias");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignedUsernameToken() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties =
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SIGNATURE + " " + ConfigurationConstants.USERNAME_TOKEN
        );
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        properties.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        properties.put(ConfigurationConstants.USER, "myalias");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }


    @Test
    public void testTimestamp() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationConstants.ACTION, ConfigurationConstants.TIMESTAMP);

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignatureTimestamp() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties =
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.TIMESTAMP + " " + ConfigurationConstants.SIGNATURE
        );
        properties.put(
            ConfigurationConstants.SIGNATURE_PARTS,
            "{}{" + WSSConstants.NS_WSU10 + "}Timestamp;"
            + "{}{" + WSSConstants.NS_SOAP11 + "}Body;"
        );
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        properties.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        properties.put(ConfigurationConstants.USER, "myalias");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignatureTimestampWrongNamespace() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties =
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.TIMESTAMP + " " + ConfigurationConstants.SIGNATURE
        );
        properties.put(
            ConfigurationConstants.SIGNATURE_PARTS,
            "{}{" + WSSConstants.NS_WSSE10 + "}Timestamp;"
            + "{}{" + WSSConstants.NS_SOAP11 + "}Body;"
        );
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        properties.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        properties.put(ConfigurationConstants.USER, "myalias");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        try {
            echo.echo("test");
            fail("Failure expected on a wrong namespace");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }
    }

    @Test
    public void testSignaturePKI() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties =
            CryptoFactory.getProperties("cxfca.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, new KeystorePasswordCallback());
        properties.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");
        properties.put(ConfigurationConstants.USER, "alice");
        properties.put(ConfigurationConstants.USE_SINGLE_CERTIFICATE, "true");
        properties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testEncryptSignature() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties =
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);
        inProperties.setDecryptionCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SIGNATURE + " " + ConfigurationConstants.ENCRYPTION
        );
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        properties.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        properties.put(ConfigurationConstants.ENC_PROP_FILE, "outsecurity.properties");
        properties.put(ConfigurationConstants.USER, "myalias");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignatureConfirmation() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties =
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        WSSSecurityProperties outProperties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.SIGNATURE);
        actions.add(WSSConstants.SIGNATURE_CONFIRMATION);
        outProperties.setActions(actions);
        outProperties.setSignatureUser("myalias");

        Properties outCryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        outProperties.setSignatureCryptoProperties(outCryptoProperties);
        outProperties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor staxOhandler = new WSS4JStaxOutInterceptor(outProperties);
        service.getOutInterceptors().add(staxOhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        properties.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        properties.put(ConfigurationConstants.ENABLE_SIGNATURE_CONFIRMATION, "true");
        properties.put(ConfigurationConstants.USER, "myalias");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        Map<String, Object> domInProperties = new HashMap<>();
        domInProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        domInProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        domInProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        domInProperties.put(ConfigurationConstants.ENABLE_SIGNATURE_CONFIRMATION, "true");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(domInProperties);
        client.getInInterceptors().add(inInterceptor);

        assertEquals("test", echo.echo("test"));
    }

    private Service createService() {
        // Create the Service
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceBean(new EchoImpl());
        factory.setAddress("local://Echo");
        factory.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        Server server = factory.create();

        Service service = server.getEndpoint().getService();
        service.getInInterceptors().add(new LoggingInInterceptor());
        service.getOutInterceptors().add(new LoggingOutInterceptor());

        return service;
    }

    private Echo createClientProxy() {
        JaxWsProxyFactoryBean proxyFac = new JaxWsProxyFactoryBean();
        proxyFac.setServiceClass(Echo.class);
        proxyFac.setAddress("local://Echo");
        proxyFac.getClientFactoryBean().setTransportId(LocalTransportFactory.TRANSPORT_ID);

        return (Echo)proxyFac.create();
    }
}
