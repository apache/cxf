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
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test StaX Security actions on the inbound side
 */
public class StaxRoundTripActionTest extends AbstractSecurityTest {

    @Test
    public void testUsernameToken() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(WSSConstants.USERNAMETOKEN);
        inProperties.setActions(actions);
        inProperties.setCallbackHandler(new TestPwdCallback());
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
        actions = new ArrayList<>();
        actions.add(WSSConstants.USERNAMETOKEN);
        properties.setActions(actions);
        properties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_TEXT);
        properties.setTokenUser("username");
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));

        actions = new ArrayList<>();
        actions.add(WSSConstants.USERNAMETOKEN);
        actions.add(XMLSecurityConstants.ENCRYPTION);
        inProperties.setActions(actions);

        try {
            echo.echo("test");
            fail("Failure expected on the wrong action");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
            assertTrue(ex.getMessage().contains(WSSecurityException.UNIFIED_SECURITY_ERR));
        }
    }

    @Test
    public void testUsernameTokenConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inConfig = new HashMap<>();
        inConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.USERNAME_TOKEN);
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

        service.getInInterceptors().remove(inhandler);
        inConfig.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.USERNAME_TOKEN + " " + ConfigurationConstants.ENCRYPTION
        );
        inhandler = new WSS4JStaxInInterceptor(inConfig);
        service.getInInterceptors().add(inhandler);

        try {
            echo.echo("test");
            fail("Failure expected on the wrong action");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
            assertTrue(ex.getMessage().contains(WSSecurityException.UNIFIED_SECURITY_ERR));
        }
    }

    @Test
    public void testEncrypt() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.ENCRYPTION);
        inProperties.setActions(actions);
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
        actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.ENCRYPTION);
        properties.setActions(actions);
        properties.setEncryptionUser("myalias");
        properties.setEncryptionSymAlgorithm(XMLSecurityConstants.NS_XENC_AES128);

        Properties outCryptoProperties =
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setEncryptionCryptoProperties(outCryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));

        actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.ENCRYPTION);
        actions.add(XMLSecurityConstants.SIGNATURE);
        inProperties.setActions(actions);

        try {
            echo.echo("test");
            fail("Failure expected on the wrong action");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
            assertTrue(ex.getMessage().contains(WSSecurityException.UNIFIED_SECURITY_ERR));
        }
    }

    @Test
    public void testEncryptConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inConfig = new HashMap<>();
        inConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION);
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
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        outConfig.put(ConfigurationConstants.ENC_PROP_FILE, "outsecurity.properties");
        outConfig.put(ConfigurationConstants.ENC_SYM_ALGO, XMLSecurityConstants.NS_XENC_AES128);
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));

        service.getInInterceptors().remove(inhandler);
        inConfig.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.ENCRYPTION + " " + ConfigurationConstants.SIGNATURE
        );
        inhandler = new WSS4JStaxInInterceptor(inConfig);
        service.getInInterceptors().add(inhandler);

        try {
            echo.echo("test");
            fail("Failure expected on the wrong action");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
            assertTrue(ex.getMessage().contains(WSSecurityException.UNIFIED_SECURITY_ERR));
        }
    }

    @Test
    public void testEncryptUsernameToken() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.ENCRYPTION);
        actions.add(WSSConstants.USERNAMETOKEN);
        inProperties.setActions(actions);
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
        actions = new ArrayList<>();
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
        inConfig.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.ENCRYPTION + " " + ConfigurationConstants.USERNAME_TOKEN
        );
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
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.SIGNATURE);
        inProperties.setActions(actions);
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
        actions = new ArrayList<>();
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

        actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.SIGNATURE);
        actions.add(XMLSecurityConstants.ENCRYPTION);
        inProperties.setActions(actions);

        try {
            echo.echo("test");
            fail("Failure expected on the wrong action");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
            assertTrue(ex.getMessage().contains(WSSecurityException.UNIFIED_SECURITY_ERR));
        }
    }

    @Test
    public void testSignatureConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inConfig = new HashMap<>();
        inConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
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

        service.getInInterceptors().remove(inhandler);
        inConfig.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SIGNATURE + " " + ConfigurationConstants.ENCRYPTION
        );
        inhandler = new WSS4JStaxInInterceptor(inConfig);
        service.getInInterceptors().add(inhandler);

        try {
            echo.echo("test");
            fail("Failure expected on the wrong action");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
            assertTrue(ex.getMessage().contains(WSSecurityException.UNIFIED_SECURITY_ERR));
        }
    }

    @Test
    public void testTimestamp() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(WSSConstants.TIMESTAMP);
        inProperties.setActions(actions);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);

        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        actions = new ArrayList<>();
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
        inConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.TIMESTAMP);
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
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(WSSConstants.TIMESTAMP);
        actions.add(XMLSecurityConstants.SIGNATURE);
        inProperties.setActions(actions);
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
        actions = new ArrayList<>();
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
        inConfig.put(
            ConfigurationConstants.ACTION,
                      ConfigurationConstants.TIMESTAMP + " " + ConfigurationConstants.SIGNATURE
        );
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
    public void testEncryptSignature() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.ENCRYPTION);
        actions.add(XMLSecurityConstants.SIGNATURE);
        inProperties.setActions(actions);
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
        actions = new ArrayList<>();
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
        inConfig.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.ENCRYPTION + " " + ConfigurationConstants.SIGNATURE
        );
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
