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
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.bsp.BSPRule;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * In these test-cases, the client is using DOM and the service is using StaX. The tests are
 * for different Signature Key Identifier methods.
 */
public class DOMToStaxSignatureIdentifierTest extends AbstractSecurityTest {

    @Test
    public void testSignatureDirectReference() throws Exception {
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
        properties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignatureIssuerSerial() throws Exception {
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
        properties.put(ConfigurationConstants.SIG_KEY_ID, "IssuerSerial");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignatureThumbprint() throws Exception {
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
        properties.put(ConfigurationConstants.SIG_KEY_ID, "Thumbprint");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignatureX509() throws Exception {
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
        properties.put(ConfigurationConstants.SIG_KEY_ID, "X509KeyIdentifier");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testSignatureKeyValue() throws Exception {
        // Create + configure service
        Service service = createService();

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties =
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);
        inProperties.addIgnoreBSPRule(BSPRule.R5417);
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
        properties.put(ConfigurationConstants.SIG_KEY_ID, "KeyValue");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor(properties);
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
