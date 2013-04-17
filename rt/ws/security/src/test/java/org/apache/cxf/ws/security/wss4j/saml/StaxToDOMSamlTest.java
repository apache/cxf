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

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
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
import org.apache.wss4j.dom.WSSecurityEngine;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.junit.Test;


/**
 * In these test-cases, the client is using StaX and the service is using DOM.
 */
@org.junit.Ignore
public class StaxToDOMSamlTest extends AbstractSecurityTest {
    
    @Test
    public void testSaml1() throws Exception {
        // Create + configure service
        Service service = createService();
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_UNSIGNED);
        final Map<QName, Object> customMap = new HashMap<QName, Object>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSSecurityEngine.SAML_TOKEN, validator);
        customMap.put(WSSecurityEngine.SAML2_TOKEN, validator);
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
        properties.setOutAction(new XMLSecurityConstants.Action[]{WSSConstants.SAML_TOKEN_UNSIGNED});
        properties.setCallbackHandler(new SAML1CallbackHandler());
        
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
        
        /*
        // TODO
        final List<WSHandlerResult> handlerResults = 
            CastUtils.cast((List<?>)service.get(WSHandlerConstants.RECV_RESULTS));
        
        WSSecurityEngineResult actionResult =
            WSSecurityUtil.fetchActionResult(handlerResults.get(0).getResults(), WSConstants.ST_UNSIGNED);
        SamlAssertionWrapper receivedAssertion = 
            (SamlAssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml1() != null);
        assert !receivedAssertion.isSigned();
        */
    }
    
    @Test
    public void testSaml2() throws Exception {
        // Create + configure service
        Service service = createService();
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_UNSIGNED);
        final Map<QName, Object> customMap = new HashMap<QName, Object>();
        CustomSamlValidator validator = new CustomSamlValidator();
        validator.setRequireSAML1Assertion(false);
        customMap.put(WSSecurityEngine.SAML_TOKEN, validator);
        customMap.put(WSSecurityEngine.SAML2_TOKEN, validator);
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
        properties.setOutAction(new XMLSecurityConstants.Action[]{WSSConstants.SAML_TOKEN_UNSIGNED});
        properties.setCallbackHandler(new SAML2CallbackHandler());
        
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
        
        /*
        // TODO
        for (String key : service.keySet()) {
            System.out.println("KEY: "+  key);
        }
        final List<WSHandlerResult> handlerResults = 
            CastUtils.cast((List<?>)service.get(WSHandlerConstants.RECV_RESULTS));
        
        WSSecurityEngineResult actionResult =
            WSSecurityUtil.fetchActionResult(handlerResults.get(0).getResults(), WSConstants.ST_UNSIGNED);
        SamlAssertionWrapper receivedAssertion = 
            (SamlAssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml2() != null);
        assert !receivedAssertion.isSigned();
        */
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
