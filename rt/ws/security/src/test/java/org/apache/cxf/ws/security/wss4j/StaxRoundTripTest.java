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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
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
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSPasswordCallback;
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
 */
public class StaxRoundTripTest extends AbstractSecurityTest {

    @Test
    public void testUsernameTokenText() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        inProperties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_TEXT);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        WSS4JPrincipalInterceptor principalInterceptor = new WSS4JPrincipalInterceptor();
        principalInterceptor.setPrincipalName("username");
        service.getInInterceptors().add(inhandler);
        service.getInInterceptors().add(principalInterceptor);

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
    public void testUsernameTokenTextConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inConfig = new HashMap<>();
        inConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inConfig.put(ConfigurationConstants.PASSWORD_TYPE, "PasswordText");
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inConfig);
        WSS4JPrincipalInterceptor principalInterceptor = new WSS4JPrincipalInterceptor();
        principalInterceptor.setPrincipalName("username");
        service.getInInterceptors().add(inhandler);
        service.getInInterceptors().add(principalInterceptor);

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
        service.getInInterceptors().remove(inhandler);
        inConfig.put(ConfigurationConstants.PASSWORD_TYPE, "PasswordDigest");
        inhandler = new WSS4JStaxInInterceptor(inConfig);
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
    public void testUsernameTokenTextUnknownUser() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        inProperties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_TEXT);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

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
        properties.setTokenUser("Alice");
        properties.setCallbackHandler(new UnknownUserPasswordCallbackHandler());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        service.put(SecurityConstants.RETURN_SECURITY_ERROR, true);

        try {
            echo.echo("test");
            fail("Failure expected on an unknown user");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
            String error = "The security token could not be authenticated or authorized";
            assertTrue(ex.getMessage().contains(error));
        }
    }

    @Test
    public void testUsernameTokenTextUnknownPassword() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        inProperties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_TEXT);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

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
        properties.setCallbackHandler(new UnknownUserPasswordCallbackHandler());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        service.put(SecurityConstants.RETURN_SECURITY_ERROR, true);

        try {
            echo.echo("test");
            fail("Failure expected on an unknown password");
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
        inProperties.setCallbackHandler(new TestPwdCallback());
        inProperties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_DIGEST);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        WSS4JPrincipalInterceptor principalInterceptor = new WSS4JPrincipalInterceptor();
        principalInterceptor.setPrincipalName("username");
        service.getInInterceptors().add(inhandler);
        service.getInInterceptors().add(principalInterceptor);

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
    public void testUsernameTokenDigestConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inConfig = new HashMap<>();
        inConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inConfig.put(ConfigurationConstants.PASSWORD_TYPE, "PasswordDigest");
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inConfig);
        WSS4JPrincipalInterceptor principalInterceptor = new WSS4JPrincipalInterceptor();
        principalInterceptor.setPrincipalName("username");
        service.getInInterceptors().add(inhandler);
        service.getInInterceptors().add(principalInterceptor);

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
        service.getInInterceptors().remove(inhandler);
        inConfig.put(ConfigurationConstants.PASSWORD_TYPE, "PasswordText");
        inhandler = new WSS4JStaxInInterceptor(inConfig);
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
    public void testUsernameTokenDigestUnknownUser() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        inProperties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_DIGEST);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

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
        properties.setTokenUser("Alice");
        properties.setCallbackHandler(new UnknownUserPasswordCallbackHandler());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        service.put(SecurityConstants.RETURN_SECURITY_ERROR, true);

        try {
            echo.echo("test");
            fail("Failure expected on an unknown user");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
            String error = "The security token could not be authenticated or authorized";
            assertTrue(ex.getMessage().contains(error));
        }
    }

    @Test
    public void testUsernameTokenDigestUnknownPassword() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        inProperties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_DIGEST);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

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
        properties.setCallbackHandler(new UnknownUserPasswordCallbackHandler());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        service.put(SecurityConstants.RETURN_SECURITY_ERROR, true);

        try {
            echo.echo("test");
            fail("Failure expected on an unknown password");
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

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.ENCRYPTION);
        properties.setActions(actions);
        properties.setEncryptionSymAlgorithm(XMLSecurityConstants.NS_XENC_AES128);
        properties.setEncryptionUser("myalias");

        Properties outCryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setEncryptionCryptoProperties(outCryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testEncryptConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inConfig = new HashMap<>();
        inConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inConfig.put(ConfigurationConstants.DEC_PROP_FILE, "insecurity.properties");
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inConfig);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION);
        outConfig.put(ConfigurationConstants.ENCRYPTION_USER, "myalias");
        outConfig.put(ConfigurationConstants.ENC_SYM_ALGO, XMLSecurityConstants.NS_XENC_AES128);
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        outConfig.put(ConfigurationConstants.ENC_PROP_FILE, "outsecurity.properties");
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

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

        Properties outCryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setEncryptionCryptoProperties(outCryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testEncryptUsernameTokenConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inConfig = new HashMap<>();
        inConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inConfig.put(ConfigurationConstants.DEC_PROP_FILE, "insecurity.properties");
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inConfig);
        service.getInInterceptors().add(inhandler);

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

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties =
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        WSS4JPrincipalInterceptor principalInterceptor = new WSS4JPrincipalInterceptor();
        principalInterceptor.setPrincipalName("CN=myAlias");
        service.getInInterceptors().add(inhandler);
        service.getInInterceptors().add(principalInterceptor);

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

        Properties outCryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(outCryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignatureConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inConfig = new HashMap<>();
        inConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inConfig.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inConfig);

        WSS4JPrincipalInterceptor principalInterceptor = new WSS4JPrincipalInterceptor();
        principalInterceptor.setPrincipalName("CN=myAlias");
        service.getInInterceptors().add(inhandler);
        service.getInInterceptors().add(principalInterceptor);

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

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties =
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        //WSS4JPrincipalInterceptor principalInterceptor = new WSS4JPrincipalInterceptor();
        //principalInterceptor.setPrincipalName("username");
        service.getInInterceptors().add(inhandler);
        //service.getInInterceptors().add(principalInterceptor);

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

        Properties outCryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(outCryptoProperties);
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

        Map<String, Object> inConfig = new HashMap<>();
        inConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inConfig.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inConfig);
        //WSS4JPrincipalInterceptor principalInterceptor = new WSS4JPrincipalInterceptor();
        //principalInterceptor.setPrincipalName("username");
        service.getInInterceptors().add(inhandler);
        //service.getInInterceptors().add(principalInterceptor);

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

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

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
    }

    @Test
    public void testTimestampConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inConfig = new HashMap<>();
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inConfig);
        service.getInInterceptors().add(inhandler);

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

        Properties outCryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(outCryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignatureTimestampConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inConfig = new HashMap<>();
        inConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inConfig.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inConfig);
        service.getInInterceptors().add(inhandler);

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
    public void testSignaturePKI() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties =
            CryptoFactory.getProperties("cxfca.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        WSS4JPrincipalInterceptor principalInterceptor = new WSS4JPrincipalInterceptor();
        principalInterceptor.setPrincipalName("CN=alice,OU=eng,O=apache.org");
        service.getInInterceptors().add(inhandler);
        service.getInInterceptors().add(principalInterceptor);

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

        Properties outCryptoProperties =
            CryptoFactory.getProperties("alice.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(outCryptoProperties);
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

        Map<String, Object> inConfig = new HashMap<>();
        inConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inConfig.put(ConfigurationConstants.SIG_VER_PROP_FILE, "cxfca.properties");
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inConfig);
        WSS4JPrincipalInterceptor principalInterceptor = new WSS4JPrincipalInterceptor();
        principalInterceptor.setPrincipalName("CN=alice,OU=eng,O=apache.org");
        service.getInInterceptors().add(inhandler);
        service.getInInterceptors().add(principalInterceptor);

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

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.ENCRYPTION);
        actions.add(XMLSecurityConstants.SIGNATURE);
        properties.setActions(actions);
        properties.setEncryptionUser("myalias");
        properties.setSignatureUser("myalias");
        properties.setEncryptionSymAlgorithm(XMLSecurityConstants.NS_XENC_AES128);

        Properties outCryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(outCryptoProperties);
        properties.setEncryptionCryptoProperties(outCryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testEncryptSignatureConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inConfig = new HashMap<>();
        inConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inConfig.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        inConfig.put(ConfigurationConstants.DEC_PROP_FILE, "insecurity.properties");
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inConfig);
        service.getInInterceptors().add(inhandler);

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

        WSSSecurityProperties properties = new WSSSecurityProperties();
        actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.SIGNATURE);
        properties.setActions(actions);
        properties.setSignatureUser("myalias");

        Properties clientOutCryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(clientOutCryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        WSSSecurityProperties staxInProperties = new WSSSecurityProperties();
        staxInProperties.setCallbackHandler(new TestPwdCallback());
        Properties staxInCryptoProperties =
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        staxInProperties.setSignatureVerificationCryptoProperties(staxInCryptoProperties);
        staxInProperties.setEnableSignatureConfirmationVerification(true);
        WSS4JStaxInInterceptor inhandler2 = new WSS4JStaxInInterceptor(staxInProperties);
        client.getInInterceptors().add(inhandler2);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignatureConfirmationConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inConfig = new HashMap<>();
        inConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inConfig.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inConfig);
        service.getInInterceptors().add(inhandler);

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(
            ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE
        );
        outConfig.put(ConfigurationConstants.SIGNATURE_USER, "myalias");
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        outConfig.put(ConfigurationConstants.ENABLE_SIGNATURE_CONFIRMATION, "true");
        outConfig.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        service.getOutInterceptors().add(ohandler);

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

    private static final class UnknownUserPasswordCallbackHandler implements CallbackHandler {

        private static Map<String, String> passwords = new HashMap<>();

        static {
            passwords.put("Alice", "AlicePassword");
            passwords.put("username", "unknownPassword");
        }

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                WSPasswordCallback pc = (WSPasswordCallback)callbacks[i];

                String pass = passwords.get(pc.getIdentifier());
                if (pass != null) {
                    pc.setPassword(pass);
                }
            }
        }
    }
}
