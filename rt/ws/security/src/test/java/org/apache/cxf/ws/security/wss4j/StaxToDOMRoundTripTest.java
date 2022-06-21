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

import javax.xml.namespace.QName;

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
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * In these test-cases, the client is using StaX and the service is using DOM.
 */
public class StaxToDOMRoundTripTest extends AbstractSecurityTest {

    @Test
    public void testUsernameTokenText() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.USERNAME_TOKEN);
        inProperties.put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_TEXT);
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(WSSConstants.USERNAMETOKEN);
        properties.setActions(actions);
        properties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_TEXT);
        properties.setTokenUser("username");
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));

        // Negative test for wrong password type
        service.getInInterceptors().remove(inInterceptor);
        inProperties.put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_DIGEST);
        inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

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
    public void testUsernameTokenTextConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.USERNAME_TOKEN);
        inProperties.put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_TEXT);
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.USERNAME_TOKEN);
        outConfig.put(ConfigurationConstants.PASSWORD_TYPE, "PasswordText");
        outConfig.put(ConfigurationConstants.USER, "username");
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));

        // Negative test for wrong password type
        service.getInInterceptors().remove(inInterceptor);
        inProperties.put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_DIGEST);
        inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

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

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.USERNAME_TOKEN);
        inProperties.put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_DIGEST);
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(WSSConstants.USERNAMETOKEN);
        properties.setActions(actions);
        properties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_DIGEST);
        properties.setTokenUser("username");
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));

        // Negative test for wrong password type
        service.getInInterceptors().remove(inInterceptor);
        inProperties.put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_TEXT);
        inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

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
    public void testUsernameTokenDigestConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.USERNAME_TOKEN);
        inProperties.put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_DIGEST);
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.USERNAME_TOKEN);
        outConfig.put(ConfigurationConstants.PASSWORD_TYPE, "PasswordDigest");
        outConfig.put(ConfigurationConstants.USER, "username");
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));

        // Negative test for wrong password type
        service.getInInterceptors().remove(inInterceptor);
        inProperties.put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_TEXT);
        inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

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

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION);
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.DEC_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.ENCRYPTION);
        properties.setActions(actions);
        properties.setEncryptionUser("myalias");
        properties.setEncryptionSymAlgorithm(XMLSecurityConstants.NS_XENC_AES128);

        Properties cryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setEncryptionCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testEncryptConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION);
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.DEC_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION);
        outConfig.put(ConfigurationConstants.ENCRYPTION_USER, "myalias");
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        outConfig.put(ConfigurationConstants.ENC_PROP_FILE, "outsecurity.properties");
        outConfig.put(ConfigurationConstants.ENC_SYM_ALGO, XMLSecurityConstants.NS_XENC_AES128);
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testEncryptionAlgorithms() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION);
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.DEC_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.ENCRYPTION);
        properties.setActions(actions);
        properties.setEncryptionUser("myalias");

        Properties cryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setEncryptionCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        properties.setEncryptionKeyTransportAlgorithm("http://www.w3.org/2001/04/xmlenc#rsa-1_5");
        properties.setEncryptionSymAlgorithm("http://www.w3.org/2001/04/xmlenc#tripledes-cbc");
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        try {
            echo.echo("test");
            fail("Failure expected as RSA v1.5 is not allowed by default");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        inProperties.put(ConfigurationConstants.ALLOW_RSA15_KEY_TRANSPORT_ALGORITHM, "true");
        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testEncryptionAlgorithmsConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION);
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.DEC_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION);
        outConfig.put(ConfigurationConstants.ENCRYPTION_USER, "myalias");
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        outConfig.put(
            ConfigurationConstants.ENC_KEY_TRANSPORT,
            "http://www.w3.org/2001/04/xmlenc#rsa-1_5"
        );
        outConfig.put(ConfigurationConstants.ENC_SYM_ALGO, XMLSecurityConstants.NS_XENC_AES128);
        outConfig.put(ConfigurationConstants.ENC_PROP_FILE, "outsecurity.properties");
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        try {
            echo.echo("test");
            fail("Failure expected as RSA v1.5 is not allowed by default");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        inProperties.put(ConfigurationConstants.ALLOW_RSA15_KEY_TRANSPORT_ALGORITHM, "true");
        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testEncryptUsernameToken() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.USERNAME_TOKEN + " " + ConfigurationConstants.ENCRYPTION
        );
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.DEC_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(WSSConstants.USERNAMETOKEN);
        actions.add(XMLSecurityConstants.ENCRYPTION);
        properties.setActions(actions);
        properties.addEncryptionPart(
            new SecurePart(new QName(WSSConstants.NS_WSSE10, "UsernameToken"), SecurePart.Modifier.Element)
        );
        properties.setEncryptionUser("myalias");
        properties.setTokenUser("username");
        properties.setEncryptionSymAlgorithm(XMLSecurityConstants.NS_XENC_AES128);

        Properties cryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setEncryptionCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testEncryptUsernameTokenConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.USERNAME_TOKEN + " " + ConfigurationConstants.ENCRYPTION
        );
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.DEC_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.USERNAME_TOKEN + " " + ConfigurationConstants.ENCRYPTION
        );
        outConfig.put(
            ConfigurationConstants.ENCRYPTION_PARTS,
            "{Element}{" + WSSConstants.NS_WSSE10 + "}UsernameToken"
        );
        outConfig.put(ConfigurationConstants.USER, "username");
        outConfig.put(ConfigurationConstants.ENCRYPTION_USER, "myalias");
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        outConfig.put(ConfigurationConstants.ENC_PROP_FILE, "outsecurity.properties");
        outConfig.put(ConfigurationConstants.ENC_SYM_ALGO, XMLSecurityConstants.NS_XENC_AES128);
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignature() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.SIGNATURE);
        properties.setActions(actions);
        properties.setSignatureUser("myalias");

        Properties cryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignatureConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        outConfig.put(ConfigurationConstants.SIGNATURE_USER, "myalias");
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        outConfig.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignedUsernameToken() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SIGNATURE + " " + ConfigurationConstants.USERNAME_TOKEN
        );
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(WSSConstants.USERNAMETOKEN);
        actions.add(XMLSecurityConstants.SIGNATURE);
        properties.setActions(actions);
        properties.setSignatureUser("myalias");

        properties.addSignaturePart(
            new SecurePart(new QName(WSSConstants.NS_WSSE10, "UsernameToken"), SecurePart.Modifier.Element)
        );
        properties.addSignaturePart(
            new SecurePart(new QName(WSSConstants.NS_SOAP11, "Body"), SecurePart.Modifier.Element)
        );

        Properties cryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(cryptoProperties);
        properties.setTokenUser("username");
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignedUsernameTokenConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SIGNATURE + " " + ConfigurationConstants.USERNAME_TOKEN
        );
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.USERNAME_TOKEN + " " + ConfigurationConstants.SIGNATURE
        );
        outConfig.put(
            ConfigurationConstants.SIGNATURE_PARTS,
            "{Element}{" + WSSConstants.NS_WSSE10 + "}UsernameToken;"
            + "{Element}{" + WSSConstants.NS_SOAP11 + "}Body"
        );
        outConfig.put(ConfigurationConstants.USER, "username");
        outConfig.put(ConfigurationConstants.SIGNATURE_USER, "myalias");
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        outConfig.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testTimestamp() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.TIMESTAMP);
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(WSSConstants.TIMESTAMP);
        properties.setActions(actions);

        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));

        // Negative test for no Timestamp
        service.getInInterceptors().remove(inInterceptor);
        inProperties.put(ConfigurationConstants.ACTION, "");
        inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        service.put(SecurityConstants.RETURN_SECURITY_ERROR, true);

        try {
            echo.echo("test");
            fail("Failure expected on no Timestamp");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
            String error = "An error was discovered";
            assertTrue(ex.getMessage().contains(error));
        }
    }

    @Test
    public void testTimestampConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.TIMESTAMP);
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.TIMESTAMP);
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));

        // Negative test for no Timestamp
        service.getInInterceptors().remove(inInterceptor);
        inProperties.put(ConfigurationConstants.ACTION, "");
        inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        service.put(SecurityConstants.RETURN_SECURITY_ERROR, true);

        try {
            echo.echo("test");
            fail("Failure expected on no Timestamp");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
            String error = "An error was discovered";
            assertTrue(ex.getMessage().contains(error));
        }
    }

    @Test
    public void testSignatureTimestamp() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SIGNATURE + " " + ConfigurationConstants.TIMESTAMP
        );
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(WSSConstants.TIMESTAMP);
        actions.add(XMLSecurityConstants.SIGNATURE);
        properties.setActions(actions);
        properties.addSignaturePart(
            new SecurePart(new QName(WSSConstants.NS_WSU10, "Timestamp"), SecurePart.Modifier.Element)
        );
        properties.addSignaturePart(
            new SecurePart(new QName(WSSConstants.NS_SOAP11, "Body"), SecurePart.Modifier.Element)
        );
        properties.setSignatureUser("myalias");

        Properties cryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignatureTimestampConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SIGNATURE + " " + ConfigurationConstants.TIMESTAMP
        );
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.TIMESTAMP + " " + ConfigurationConstants.SIGNATURE
        );
        outConfig.put(
            ConfigurationConstants.SIGNATURE_PARTS,
            "{Element}{" + WSSConstants.NS_WSU10 + "}Timestamp;"
            + "{Element}{" + WSSConstants.NS_SOAP11 + "}Body"
        );
        outConfig.put(ConfigurationConstants.SIGNATURE_USER, "myalias");
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        outConfig.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignatureTimestampWrongNamespace() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SIGNATURE + " " + ConfigurationConstants.TIMESTAMP
        );
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.SIGNATURE);
        actions.add(WSSConstants.TIMESTAMP);
        properties.setActions(actions);
        properties.addSignaturePart(
            new SecurePart(new QName(WSSConstants.NS_WSSE10, "Timestamp"), SecurePart.Modifier.Element)
        );
        properties.addSignaturePart(
            new SecurePart(new QName(WSSConstants.NS_SOAP11, "Body"), SecurePart.Modifier.Element)
        );
        properties.setSignatureUser("myalias");

        Properties cryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
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

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new KeystorePasswordCallback());
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "cxfca.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.SIGNATURE);
        properties.setActions(actions);
        properties.setSignatureUser("alice");

        Properties cryptoProperties =
            CryptoFactory.getProperties("alice.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new KeystorePasswordCallback());
        properties.setUseSingleCert(true);
        properties.setSignatureKeyIdentifier(
            WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE
        );

        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignaturePKIConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new KeystorePasswordCallback());
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "cxfca.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        outConfig.put(ConfigurationConstants.SIGNATURE_USER, "alice");
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new KeystorePasswordCallback());
        outConfig.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");
        outConfig.put(ConfigurationConstants.USE_SINGLE_CERTIFICATE, "true");
        outConfig.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testEncryptSignature() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SIGNATURE + " " + ConfigurationConstants.ENCRYPTION
        );
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        inProperties.put(ConfigurationConstants.DEC_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.ENCRYPTION);
        actions.add(XMLSecurityConstants.SIGNATURE);
        properties.setActions(actions);
        properties.setEncryptionUser("myalias");
        properties.setSignatureUser("myalias");
        properties.setEncryptionSymAlgorithm(XMLSecurityConstants.NS_XENC_AES128);

        Properties cryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setEncryptionCryptoProperties(cryptoProperties);
        properties.setSignatureCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testEncryptSignatureConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SIGNATURE + " " + ConfigurationConstants.ENCRYPTION
        );
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        inProperties.put(ConfigurationConstants.DEC_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.ENCRYPTION + " " + ConfigurationConstants.SIGNATURE
        );
        outConfig.put(ConfigurationConstants.SIGNATURE_USER, "myalias");
        outConfig.put(ConfigurationConstants.ENCRYPTION_USER, "myalias");
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        outConfig.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        outConfig.put(ConfigurationConstants.ENC_PROP_FILE, "outsecurity.properties");
        outConfig.put(ConfigurationConstants.ENC_SYM_ALGO, XMLSecurityConstants.NS_XENC_AES128);
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignatureConfirmation() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        outProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        outProperties.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        outProperties.put(ConfigurationConstants.ENABLE_SIGNATURE_CONFIRMATION, "true");
        outProperties.put(ConfigurationConstants.USER, "myalias");

        WSS4JOutInterceptor domOhandler = new WSS4JOutInterceptor(outProperties);
        service.getOutInterceptors().add(domOhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.SIGNATURE);
        properties.setActions(actions);
        properties.setSignatureUser("myalias");

        Properties cryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        WSSSecurityProperties staxInProperties = new WSSSecurityProperties();
        staxInProperties.setCallbackHandler(new TestPwdCallback());
        Properties staxInCryptoProperties =
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        staxInProperties.setSignatureVerificationCryptoProperties(staxInCryptoProperties);
        staxInProperties.setEnableSignatureConfirmationVerification(true);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(staxInProperties);
        client.getInInterceptors().add(inhandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignatureConfirmationConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        outProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        outProperties.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        outProperties.put(ConfigurationConstants.ENABLE_SIGNATURE_CONFIRMATION, "true");
        outProperties.put(ConfigurationConstants.USER, "myalias");

        WSS4JOutInterceptor domOhandler = new WSS4JOutInterceptor(outProperties);
        service.getOutInterceptors().add(domOhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> clientOutConfig = new HashMap<>();
        clientOutConfig.put(
            ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE
        );
        clientOutConfig.put(ConfigurationConstants.SIGNATURE_USER, "myalias");
        clientOutConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        clientOutConfig.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        WSS4JStaxOutInterceptor clientOhandler = new WSS4JStaxOutInterceptor(clientOutConfig);

        client.getOutInterceptors().add(clientOhandler);

        Map<String, Object> clientInConfig = new HashMap<>();
        clientInConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        clientInConfig.put(ConfigurationConstants.ENABLE_SIGNATURE_CONFIRMATION, "true");
        clientInConfig.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JStaxInInterceptor clientInHandler = new WSS4JStaxInInterceptor(clientInConfig);

        client.getInInterceptors().add(clientInHandler);

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
