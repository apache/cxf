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
package org.apache.cxf.systest.jms.security;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.hello_world_jms.HelloWorldPortType;
import org.apache.cxf.hello_world_jms.HelloWorldService;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.saml.bean.AudienceRestrictionBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.builder.SAML2Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;


/**
 * Some WS-Security over JMS tests
 */
public class JMSWSSecurityTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(JMSWSSecurityTest.class);

    private static EmbeddedJMSBrokerLauncher broker;
    private List<String> wsdlStrings = new ArrayList<>();

    @BeforeClass
    public static void startServers() throws Exception {
        broker = new EmbeddedJMSBrokerLauncher("tcp://localhost:" + PORT);
        launchServer(broker);
        launchServer(new Server(broker));
        createStaticBus();
    }

    @Before
    public void setUp() throws Exception {
        assertSame(getStaticBus(), BusFactory.getThreadDefaultBus(false));
    }

    @After
    public void tearDown() throws Exception {
        wsdlStrings.clear();
    }

    public URL getWSDLURL(String s) throws Exception {
        URL u = getClass().getResource(s);
        if (u == null) {
            throw new IllegalArgumentException("WSDL classpath resource not found " + s);
        }
        String wsdlString = u.toString().intern();
        wsdlStrings.add(wsdlString);
        broker.updateWsdl(getBus(), wsdlString);
        return u;
    }

    @Test
    public void testUnsignedSAML2Token() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldService service = new HelloWorldService(wsdl, serviceName);

        String response = new String("Bonjour");
        HelloWorldPortType greeter = service.getPort(portName, HelloWorldPortType.class);

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setSignAssertion(true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_BEARER);

        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        outProperties.put(ConfigurationConstants.SAML_CALLBACK_REF, callbackHandler);

        WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(outProperties);
        Client client = ClientProxy.getClient(greeter);
        client.getOutInterceptors().add(outInterceptor);

        String reply = greeter.sayHi();
        assertNotNull("no response received from service", reply);
        assertEquals(response, reply);

        ((java.io.Closeable)greeter).close();
    }

    @Test
    public void testUnsignedSAML2AudienceRestrictionTokenURI() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldService service = new HelloWorldService(wsdl, serviceName);

        String response = new String("Bonjour");
        HelloWorldPortType greeter = service.getPort(portName, HelloWorldPortType.class);

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setSignAssertion(true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_BEARER);

        ConditionsBean conditions = new ConditionsBean();
        conditions.setTokenPeriodMinutes(5);
        List<String> audiences = new ArrayList<>();
        audiences.add("jms:jndi:dynamicQueues/test.jmstransport.text");
        AudienceRestrictionBean audienceRestrictionBean = new AudienceRestrictionBean();
        audienceRestrictionBean.setAudienceURIs(audiences);
        conditions.setAudienceRestrictions(Collections.singletonList(audienceRestrictionBean));

        callbackHandler.setConditions(conditions);

        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        outProperties.put(ConfigurationConstants.SAML_CALLBACK_REF, callbackHandler);

        WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(outProperties);
        Client client = ClientProxy.getClient(greeter);
        client.getOutInterceptors().add(outInterceptor);

        String reply = greeter.sayHi();
        assertNotNull("no response received from service", reply);
        assertEquals(response, reply);

        ((java.io.Closeable)greeter).close();
    }

    @Test
    public void testUnsignedSAML2AudienceRestrictionTokenBadURI() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldService service = new HelloWorldService(wsdl, serviceName);

        HelloWorldPortType greeter = service.getPort(portName, HelloWorldPortType.class);

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setSignAssertion(true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_BEARER);

        ConditionsBean conditions = new ConditionsBean();
        conditions.setTokenPeriodMinutes(5);
        List<String> audiences = new ArrayList<>();
        audiences.add("jms:jndi:dynamicQueues/test.jmstransport.text.bad");
        AudienceRestrictionBean audienceRestrictionBean = new AudienceRestrictionBean();
        audienceRestrictionBean.setAudienceURIs(audiences);
        conditions.setAudienceRestrictions(Collections.singletonList(audienceRestrictionBean));

        callbackHandler.setConditions(conditions);

        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        outProperties.put(ConfigurationConstants.SAML_CALLBACK_REF, callbackHandler);

        WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(outProperties);
        Client client = ClientProxy.getClient(greeter);
        client.getOutInterceptors().add(outInterceptor);

        try {
            greeter.sayHi();
            fail("Failure expected on a bad audience restriction");
        } catch (SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)greeter).close();
    }

    @Test
    public void testUnsignedSAML2AudienceRestrictionTokenServiceName() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldService service = new HelloWorldService(wsdl, serviceName);

        String response = new String("Bonjour");
        HelloWorldPortType greeter = service.getPort(portName, HelloWorldPortType.class);

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setSignAssertion(true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_BEARER);

        ConditionsBean conditions = new ConditionsBean();
        conditions.setTokenPeriodMinutes(5);
        List<String> audiences = new ArrayList<>();
        audiences.add("{http://cxf.apache.org/hello_world_jms}HelloWorldService");
        AudienceRestrictionBean audienceRestrictionBean = new AudienceRestrictionBean();
        audienceRestrictionBean.setAudienceURIs(audiences);
        conditions.setAudienceRestrictions(Collections.singletonList(audienceRestrictionBean));

        callbackHandler.setConditions(conditions);

        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        outProperties.put(ConfigurationConstants.SAML_CALLBACK_REF, callbackHandler);

        WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(outProperties);
        Client client = ClientProxy.getClient(greeter);
        client.getOutInterceptors().add(outInterceptor);

        String reply = greeter.sayHi();
        assertNotNull("no response received from service", reply);
        assertEquals(response, reply);

        ((java.io.Closeable)greeter).close();
    }

    @Test
    public void testUnsignedSAML2AudienceRestrictionTokenBadServiceName() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldService service = new HelloWorldService(wsdl, serviceName);

        HelloWorldPortType greeter = service.getPort(portName, HelloWorldPortType.class);

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setSignAssertion(true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_BEARER);

        ConditionsBean conditions = new ConditionsBean();
        conditions.setTokenPeriodMinutes(5);
        List<String> audiences = new ArrayList<>();
        audiences.add("{http://cxf.apache.org/hello_world_jms}BadHelloWorldService");
        AudienceRestrictionBean audienceRestrictionBean = new AudienceRestrictionBean();
        audienceRestrictionBean.setAudienceURIs(audiences);
        conditions.setAudienceRestrictions(Collections.singletonList(audienceRestrictionBean));

        callbackHandler.setConditions(conditions);

        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        outProperties.put(ConfigurationConstants.SAML_CALLBACK_REF, callbackHandler);

        WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(outProperties);
        Client client = ClientProxy.getClient(greeter);
        client.getOutInterceptors().add(outInterceptor);

        try {
            greeter.sayHi();
            fail("Failure expected on a bad audience restriction");
        } catch (SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)greeter).close();
    }
}
