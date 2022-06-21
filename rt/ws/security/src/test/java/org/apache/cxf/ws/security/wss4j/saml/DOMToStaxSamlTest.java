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

import java.util.HashMap;
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
import org.apache.cxf.ws.security.wss4j.AbstractSecurityTest;
import org.apache.cxf.ws.security.wss4j.Echo;
import org.apache.cxf.ws.security.wss4j.EchoImpl;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxInInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * In these test-cases, the client is using DOM and the service is using StaX.
 */
public class DOMToStaxSamlTest extends AbstractSecurityTest {

    @Test
    public void testSaml1() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setValidateSamlSubjectConfirmation(false);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        properties.put(
            ConfigurationConstants.SAML_CALLBACK_REF, new SAML1CallbackHandler()
        );

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSaml1SignedSenderVouches() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
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
        properties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_SIGNED);
        properties.put(ConfigurationConstants.SAML_CALLBACK_REF, new SAML1CallbackHandler());

        properties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        properties.put(ConfigurationConstants.USER, "alice");
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, new PasswordCallbackHandler());
        properties.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSaml2() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setValidateSamlSubjectConfirmation(false);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        properties.put(
            ConfigurationConstants.SAML_CALLBACK_REF, new SAML2CallbackHandler()
        );

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSaml2SignedSenderVouches() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
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
        properties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_SIGNED);
        properties.put(ConfigurationConstants.SAML_CALLBACK_REF, new SAML2CallbackHandler());

        properties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        properties.put(ConfigurationConstants.USER, "alice");
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, new PasswordCallbackHandler());
        properties.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSaml1TokenHOK() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        Properties cryptoProperties =
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);

        CustomStaxSamlValidator validator = new CustomStaxSamlValidator();
        inProperties.addValidator(WSConstants.SAML_TOKEN, validator);
        inProperties.addValidator(WSConstants.SAML2_TOKEN, validator);

        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_SIGNED);
        SAML1CallbackHandler callbackHandler = new SAML1CallbackHandler();
        callbackHandler.setConfirmationMethod(SAML1Constants.CONF_HOLDER_KEY);
        callbackHandler.setSignAssertion(true);
        properties.put(ConfigurationConstants.SAML_CALLBACK_REF, callbackHandler);

        properties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        properties.put(ConfigurationConstants.USER, "alice");
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, new PasswordCallbackHandler());
        properties.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
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

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        Properties cryptoProperties =
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);

        CustomStaxSamlValidator validator = new CustomStaxSamlValidator();
        inProperties.addValidator(WSConstants.SAML_TOKEN, validator);
        inProperties.addValidator(WSConstants.SAML2_TOKEN, validator);

        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

        // Create + configure client
        Echo echo = createClientProxy();

        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_SIGNED);
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        callbackHandler.setSignAssertion(true);
        properties.put(ConfigurationConstants.SAML_CALLBACK_REF, callbackHandler);

        properties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        properties.put(ConfigurationConstants.USER, "alice");
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, new PasswordCallbackHandler());
        properties.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
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
