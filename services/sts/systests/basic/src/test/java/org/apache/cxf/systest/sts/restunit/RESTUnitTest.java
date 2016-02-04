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
package org.apache.cxf.systest.sts.restunit;

import java.net.URL;
import java.util.List;

import javax.security.auth.callback.CallbackHandler;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.processor.Processor;
import org.apache.wss4j.dom.processor.SAMLTokenProcessor;
import org.junit.BeforeClass;

/**
 * Some unit tests for the CXF STSClient Issue Binding.
 */
public class RESTUnitTest extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSRESTServer.class);
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(STSRESTServer.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }
    
    @org.junit.Test
    public void testIssueSAML2Token() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = RESTUnitTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, "alice", "clarinet", busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.path("saml2.0");
        
        Response response = client.get();
        RequestSecurityTokenResponseType securityResponse = 
            response.readEntity(RequestSecurityTokenResponseType.class);
        
        RequestedSecurityTokenType requestedSecurityToken = null;
        for (Object obj : securityResponse.getAny()) {
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>)obj;
                if ("RequestedSecurityToken".equals(jaxbElement.getName().getLocalPart())) {
                    requestedSecurityToken = (RequestedSecurityTokenType)jaxbElement.getValue();
                    break;
                }
            }
        }
        assertNotNull(requestedSecurityToken);
        
        // Process the token
        List<WSSecurityEngineResult> results = processToken(requestedSecurityToken);

        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion = 
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(assertion != null);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
        assertTrue(assertion.isSigned());

        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testIssueJWTToken() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = RESTUnitTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, "alice", "clarinet", busFile.toString());

        client.type("application/json").accept("application/json");
        client.path("jwt");
        
        client.get();
    }
    
    private List<WSSecurityEngineResult> processToken(RequestedSecurityTokenType securityResponse)
        throws Exception {
        RequestData requestData = new RequestData();
        requestData.setDisableBSPEnforcement(true);
        CallbackHandler callbackHandler = new org.apache.cxf.systest.sts.common.CommonCallbackHandler();
        requestData.setCallbackHandler(callbackHandler);
        Crypto crypto = CryptoFactory.getInstance("serviceKeystore.properties");
        requestData.setDecCrypto(crypto);
        requestData.setSigVerCrypto(crypto);
        
        Processor processor = new SAMLTokenProcessor();
        Element securityTokenElem = (Element)securityResponse.getAny();
        return processor.handleToken(
            securityTokenElem, requestData, new WSDocInfo(securityTokenElem.getOwnerDocument())
        );
    }
    
}
