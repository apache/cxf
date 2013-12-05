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
package org.apache.cxf.systest.sts.renew;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.common.TestParam;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.wss4j.dom.WSConstants;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

/**
 * In this test case, a CXF client requests a SAML Token from an STS and then tries to renew it.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class SAMLRenewUnitTest extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSServer.class);
    static final String STAX_STSPORT = allocatePort(StaxSTSServer.class);
    
    final TestParam test;
    
    public SAMLRenewUnitTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(STSServer.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(StaxSTSServer.class, true)
        );
    }
    
    @Parameters(name = "{0}")
    public static Collection<TestParam[]> data() {
       
        return Arrays.asList(new TestParam[][] {{new TestParam("", false, STSPORT)},
                                                {new TestParam("", false, STAX_STSPORT)},
        });
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void testRenewSAML1Token() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SAMLRenewUnitTest.class.getResource("cxf-client-unit.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String wsdlLocation = 
            "https://localhost:" + test.getStsPort() + "/SecurityTokenService/Transport?wsdl";
        
        // Request the token
        SecurityToken token = 
            requestSecurityToken(bus, wsdlLocation, WSConstants.WSS_SAML_TOKEN_TYPE, 2, true);
        assertNotNull(token);
        // Sleep to expire the token
        Thread.sleep(2100);
        
        // Renew the token
        SecurityToken renewedToken = renewSecurityToken(bus, wsdlLocation, token, false);
        assertFalse(token.equals(renewedToken));
        
        // Try to validate old token -> fail.
        try {
            validateSecurityToken(bus, wsdlLocation, token);
            fail("Failure expected on trying to renew the old token");
        } catch (Exception ex) {
            // expected
        }
        
        // Validate the renewed token
        validateSecurityToken(bus, wsdlLocation, renewedToken);
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testRenewSAML2Token() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SAMLRenewUnitTest.class.getResource("cxf-client-unit.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String wsdlLocation = 
            "https://localhost:" + test.getStsPort() + "/SecurityTokenService/Transport?wsdl";
        
        // Request the token
        SecurityToken token = 
            requestSecurityToken(bus, wsdlLocation, WSConstants.WSS_SAML2_TOKEN_TYPE, 2, true);
        assertNotNull(token);
        // Sleep to expire the token
        Thread.sleep(2100);
        
        // Renew the token
        SecurityToken renewedToken = renewSecurityToken(bus, wsdlLocation, token, false);
        assertFalse(token.equals(renewedToken));
        
        // Try to validate old token -> fail.
        try {
            validateSecurityToken(bus, wsdlLocation, token);
            fail("Failure expected on trying to renew the old token");
        } catch (Exception ex) {
            // expected
        }
        
        // Validate the renewed token
        validateSecurityToken(bus, wsdlLocation, renewedToken);
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testRenewSAML2TokenFail() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SAMLRenewUnitTest.class.getResource("cxf-client-unit.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String wsdlLocation = 
            "https://localhost:" + test.getStsPort() + "/SecurityTokenService/Transport?wsdl";
        
        // Request the token
        SecurityToken token = 
            requestSecurityToken(bus, wsdlLocation, WSConstants.WSS_SAML2_TOKEN_TYPE, 2, false);
        assertNotNull(token);
        // Sleep to expire the token
        Thread.sleep(2100);
        
        // Renew the token - this will fail as we didn't send a Renewing @OK attribute
        try {
            renewSecurityToken(bus, wsdlLocation, token, false);
            fail("Failure expected on a different AppliesTo address");
        } catch (Exception ex) {
            // expected
        }
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testRenewValidSAML1Token() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SAMLRenewUnitTest.class.getResource("cxf-client-unit.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String wsdlLocation = 
            "https://localhost:" + test.getStsPort() + "/SecurityTokenService/Transport?wsdl";
        
        // Request the token
        SecurityToken token = 
            requestSecurityToken(bus, wsdlLocation, WSConstants.WSS_SAML_TOKEN_TYPE, 300, false);
        assertNotNull(token);
        
        // Validate the token
        List<SecurityToken> validatedTokens = validateSecurityToken(bus, wsdlLocation, token);
        assertFalse(validatedTokens.isEmpty());
        assertTrue(validatedTokens.get(0).equals(token));

        // Renew the token
        SecurityToken renewedToken = renewSecurityToken(bus, wsdlLocation, token, false);
        assertFalse(token.equals(renewedToken));
        
        // Validate the renewed token
        validateSecurityToken(bus, wsdlLocation, renewedToken);
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testRenewSAML2TokenDifferentAppliesTo() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SAMLRenewUnitTest.class.getResource("cxf-client-unit.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String wsdlLocation = 
            "https://localhost:" + test.getStsPort() + "/SecurityTokenService/Transport?wsdl";
        
        // Request the token
        SecurityToken token = 
            requestSecurityToken(bus, wsdlLocation, WSConstants.WSS_SAML2_TOKEN_TYPE, 2, true);
        assertNotNull(token);
        // Sleep to expire the token
        Thread.sleep(2100);
        
        // Renew the token
        token.setIssuerAddress("http://www.apache.org");
        try {
            renewSecurityToken(bus, wsdlLocation, token, true);
            fail("Failure expected on a different AppliesTo address");
        } catch (Exception ex) {
            // expected
        }
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testRenewDisabled() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SAMLRenewUnitTest.class.getResource("cxf-client-unit.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String wsdlLocation = 
            "https://localhost:" + test.getStsPort() + "/SecurityTokenService/Transport?wsdl";
        
        // Request the token
        SecurityToken token = 
            requestSecurityToken(bus, wsdlLocation, WSConstants.WSS_SAML_TOKEN_TYPE, 300, false, false);
        assertNotNull(token);
        
        // Validate the token
        List<SecurityToken> validatedTokens = validateSecurityToken(bus, wsdlLocation, token);
        assertFalse(validatedTokens.isEmpty());
        assertTrue(validatedTokens.get(0).equals(token));

        // Renew the token
        SecurityToken renewedToken = renewSecurityToken(bus, wsdlLocation, token, false);
        assertFalse(token.equals(renewedToken));
        
        // Validate the renewed token
        validateSecurityToken(bus, wsdlLocation, renewedToken);

        bus.shutdown(true);
    }
    
    private SecurityToken requestSecurityToken(
        Bus bus, String wsdlLocation, String tokenType, int ttl, boolean allowExpired
    ) throws Exception {
        return requestSecurityToken(bus, wsdlLocation, tokenType, ttl, allowExpired, true);
    }
    
    private SecurityToken requestSecurityToken(
        Bus bus, String wsdlLocation, String tokenType, int ttl, boolean allowExpired, boolean sendRenewing
    ) throws Exception {
        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation(wsdlLocation);
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");
        stsClient.setTokenType(tokenType);
        stsClient.setKeyType("http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(
            SecurityConstants.CALLBACK_HANDLER, 
            "org.apache.cxf.systest.sts.common.CommonCallbackHandler"
        );
        properties.put(SecurityConstants.STS_TOKEN_PROPERTIES, "serviceKeystore.properties");
        
        stsClient.setTtl(ttl);
        stsClient.setAllowRenewingAfterExpiry(allowExpired);
        stsClient.setEnableLifetime(true);

        stsClient.setProperties(properties);
        stsClient.setRequiresEntropy(true);
        stsClient.setKeySize(128);
        stsClient.setAddressingNamespace("http://www.w3.org/2005/08/addressing");
        stsClient.setSendRenewing(sendRenewing);

        return stsClient.requestSecurityToken("https://localhost:8081/doubleit/services/doubleittransport");
    }
    
    private List<SecurityToken> validateSecurityToken(
        Bus bus, String wsdlLocation, SecurityToken securityToken
    ) throws Exception {
        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation(wsdlLocation);
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(
            SecurityConstants.CALLBACK_HANDLER, 
            "org.apache.cxf.systest.sts.common.CommonCallbackHandler"
        );
        properties.put(SecurityConstants.STS_TOKEN_PROPERTIES, "serviceKeystore.properties");

        stsClient.setProperties(properties);
        stsClient.setAddressingNamespace("http://www.w3.org/2005/08/addressing");

        return stsClient.validateSecurityToken(securityToken);
    }
    
    private SecurityToken renewSecurityToken(
        Bus bus, String wsdlLocation, SecurityToken securityToken, boolean enableAppliesTo
    ) throws Exception {
        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation(wsdlLocation);
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(
            SecurityConstants.CALLBACK_HANDLER, 
            "org.apache.cxf.systest.sts.common.CommonCallbackHandler"
        );
        properties.put(SecurityConstants.STS_TOKEN_PROPERTIES, "serviceKeystore.properties");

        stsClient.setEnableAppliesTo(enableAppliesTo);
        // Request a token with a TTL of 60 minutes
        stsClient.setTtl(60 * 60);
        stsClient.setEnableLifetime(true);
        stsClient.setProperties(properties);
        stsClient.setAddressingNamespace("http://www.w3.org/2005/08/addressing");
        
        return stsClient.renewSecurityToken(securityToken);
    }
    
}
