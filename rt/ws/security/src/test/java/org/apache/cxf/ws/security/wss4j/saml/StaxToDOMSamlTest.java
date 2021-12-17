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
package org.apache.cxf.ws.security.wss4j.saml;

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
import org.apache.cxf.ws.security.wss4j.AbstractSecurityTest;
import org.apache.cxf.ws.security.wss4j.Echo;
import org.apache.cxf.ws.security.wss4j.EchoImpl;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxOutInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * In these test-cases, the client is using StaX and the service is using DOM.
 */
public class StaxToDOMSamlTest extends AbstractSecurityTest {

    @Test
    public void testSaml1() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);
        inProperties.put(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, "false");

        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        service.put(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, "false");

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(WSSConstants.SAML_TOKEN_UNSIGNED);
        properties.setActions(actions);
        properties.setSamlCallbackHandler(new SAML1CallbackHandler());

        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSaml1Config() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);
        inProperties.put(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, "false");

        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        service.put(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, "false");

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        outConfig.put(ConfigurationConstants.SAML_CALLBACK_REF, new SAML1CallbackHandler());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSaml1SignedSenderVouches() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SAML_TOKEN_UNSIGNED + " " + ConfigurationConstants.SIGNATURE
        );
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(WSSConstants.SAML_TOKEN_SIGNED);
        properties.setActions(actions);
        properties.setSamlCallbackHandler(new SAML1CallbackHandler());
        properties.setCallbackHandler(new PasswordCallbackHandler());

        properties.setSignatureKeyIdentifier(
            WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE
        );

        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSaml1SignedSenderVouchesConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SAML_TOKEN_UNSIGNED + " " + ConfigurationConstants.SIGNATURE
        );
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_SIGNED);
        outConfig.put(ConfigurationConstants.SAML_CALLBACK_REF, new SAML1CallbackHandler());
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new PasswordCallbackHandler());
        outConfig.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSaml2() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        validator.setRequireSAML1Assertion(false);
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);
        inProperties.put(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, "false");

        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        service.put(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, "false");

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(WSSConstants.SAML_TOKEN_UNSIGNED);
        properties.setActions(actions);
        properties.setSamlCallbackHandler(new SAML2CallbackHandler());

        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSaml2Config() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        validator.setRequireSAML1Assertion(false);
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);
        inProperties.put(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, "false");

        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        service.put(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, "false");

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        outConfig.put(ConfigurationConstants.SAML_CALLBACK_REF, new SAML2CallbackHandler());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSaml2SignedSenderVouches() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SAML_TOKEN_UNSIGNED + " " + ConfigurationConstants.SIGNATURE
        );
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        validator.setRequireSAML1Assertion(false);
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(WSSConstants.SAML_TOKEN_SIGNED);
        properties.setActions(actions);
        properties.setSamlCallbackHandler(new SAML2CallbackHandler());
        properties.setCallbackHandler(new PasswordCallbackHandler());

        properties.setSignatureKeyIdentifier(
            WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE
        );

        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSaml2SignedSenderVouchesConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SAML_TOKEN_UNSIGNED + " " + ConfigurationConstants.SIGNATURE
        );
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        validator.setRequireSAML1Assertion(false);
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_SIGNED);
        outConfig.put(ConfigurationConstants.SAML_CALLBACK_REF, new SAML2CallbackHandler());
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new PasswordCallbackHandler());
        outConfig.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSaml1TokenHOK() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SAML_TOKEN_SIGNED + " " + ConfigurationConstants.SIGNATURE
        );
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(WSSConstants.SAML_TOKEN_SIGNED);
        properties.setActions(actions);
        SAML1CallbackHandler callbackHandler = new SAML1CallbackHandler();
        callbackHandler.setSignAssertion(true);
        callbackHandler.setConfirmationMethod(SAML1Constants.CONF_HOLDER_KEY);
        properties.setSamlCallbackHandler(callbackHandler);

        properties.setSignatureUser("alice");

        Properties cryptoProperties =
            CryptoFactory.getProperties("alice.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(cryptoProperties);
        properties.setSignatureKeyIdentifier(
            WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE
        );
        properties.setCallbackHandler(new PasswordCallbackHandler());

        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        try {
            echo.echo("test");
            fail("Failure expected on receiving sender vouches instead of HOK");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        validator.setRequireSenderVouches(false);
        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSaml1TokenHOKConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SAML_TOKEN_SIGNED + " " + ConfigurationConstants.SIGNATURE
        );
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_SIGNED);
        SAML1CallbackHandler callbackHandler = new SAML1CallbackHandler();
        callbackHandler.setSignAssertion(true);
        callbackHandler.setConfirmationMethod(SAML1Constants.CONF_HOLDER_KEY);
        outConfig.put(ConfigurationConstants.SAML_CALLBACK_REF, callbackHandler);
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new PasswordCallbackHandler());
        outConfig.put(ConfigurationConstants.SIGNATURE_USER, "alice");
        outConfig.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");
        outConfig.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        try {
            echo.echo("test");
            fail("Failure expected on receiving sender vouches instead of HOK");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        validator.setRequireSenderVouches(false);
        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSaml2TokenHOK() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SAML_TOKEN_SIGNED + " " + ConfigurationConstants.SIGNATURE
        );
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        WSSSecurityProperties properties = new WSSSecurityProperties();
        List<WSSConstants.Action> actions = new ArrayList<>();
        actions.add(WSSConstants.SAML_TOKEN_SIGNED);
        properties.setActions(actions);
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setSignAssertion(true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        properties.setSamlCallbackHandler(callbackHandler);
        properties.setCallbackHandler(new PasswordCallbackHandler());

        properties.setSignatureUser("alice");

        Properties cryptoProperties =
            CryptoFactory.getProperties("alice.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(cryptoProperties);
        properties.setSignatureKeyIdentifier(
            WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE
        );

        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        try {
            echo.echo("test");
            fail("Failure expected on receiving sender vouches instead of HOK");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }
        validator.setRequireSenderVouches(false);

        try {
            echo.echo("test");
            fail("Failure expected on receiving a SAML 1.1 Token instead of SAML 2.0");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }
        validator.setRequireSAML1Assertion(false);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSaml2TokenHOKConfig() throws Exception {
        // Create + configure service
        Service service = createService();

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SAML_TOKEN_SIGNED + " " + ConfigurationConstants.SIGNATURE
        );
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> outConfig = new HashMap<>();
        outConfig.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_SIGNED);
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setSignAssertion(true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        outConfig.put(ConfigurationConstants.SAML_CALLBACK_REF, callbackHandler);
        outConfig.put(ConfigurationConstants.PW_CALLBACK_REF, new PasswordCallbackHandler());
        outConfig.put(ConfigurationConstants.SIGNATURE_USER, "alice");
        outConfig.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");
        outConfig.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(outConfig);

        client.getOutInterceptors().add(ohandler);

        try {
            echo.echo("test");
            fail("Failure expected on receiving sender vouches instead of HOK");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }
        validator.setRequireSenderVouches(false);

        try {
            echo.echo("test");
            fail("Failure expected on receiving a SAML 1.1 Token instead of SAML 2.0");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }
        validator.setRequireSAML1Assertion(false);

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
