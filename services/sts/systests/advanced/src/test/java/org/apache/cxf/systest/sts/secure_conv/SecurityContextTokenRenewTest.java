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
package org.apache.cxf.systest.sts.secure_conv;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;

import org.junit.BeforeClass;

/**
 * In this test case, a CXF client requests a SecurityContextToken from an STS and then renews it. When
 * renewing the token, the WSDL of the STS has an EndorsingSupportingToken consisting of the 
 * SecureConversationToken. The client must use the secret associated with the SecurityContextToken it gets 
 * back from the STS to sign the Timestamp.
 */
public class SecurityContextTokenRenewTest extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSServer.class);
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(STSServer.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() {
        SecurityTestUtil.cleanup();
    }

    @org.junit.Test
    @org.junit.Ignore
    public void testRenewSecurityContextToken() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SecurityContextTokenRenewTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String wsdlLocation = 
            "https://localhost:" + STSPORT + "/SecurityTokenService/TransportSCT?wsdl";
        SecurityToken token = 
            requestSecurityToken(bus, wsdlLocation, true);
        assertTrue(token.getSecret() != null && token.getSecret().length > 0);
        
        // Renew the SecurityContextToken - this should fail as the secret associated with the SCT
        // is not used to sign some part of the message
        String port = "{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port";
        try {
            renewSecurityToken(bus, wsdlLocation, port, true, token);
            fail("Failure expected on no proof-of-possession");
        } catch (Exception ex) {
            // expected
        }
        
        String endorsingPort = "{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Endorsing_Port";
        renewSecurityToken(bus, wsdlLocation, endorsingPort, true, token);
    }
    
    private SecurityToken requestSecurityToken(
        Bus bus, String wsdlLocation, boolean enableEntropy
    ) throws Exception {
        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation(wsdlLocation);
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(
            "ws-security.callback-handler", 
            "org.apache.cxf.systest.sts.common.CommonCallbackHandler"
        );
        properties.put("ws-security.sts.token.properties", "serviceKeystore.properties");

        stsClient.setProperties(properties);
        stsClient.setSecureConv(true);
        stsClient.setRequiresEntropy(enableEntropy);
        stsClient.setKeySize(128);
        stsClient.setAddressingNamespace("http://www.w3.org/2005/08/addressing");

        return stsClient.requestSecurityToken(null);
    }
    
    private void renewSecurityToken(
        Bus bus, String wsdlLocation, String port, boolean enableEntropy, SecurityToken securityToken
    ) throws Exception {
        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation(wsdlLocation);
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName(port);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(SecurityConstants.SIGNATURE_USERNAME, "myservicekey");
        properties.put(
            SecurityConstants.CALLBACK_HANDLER, 
            "org.apache.cxf.systest.sts.common.CommonCallbackHandler"
        );
        properties.put(SecurityConstants.STS_TOKEN_PROPERTIES, "serviceKeystore.properties");
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES, "serviceKeystore.properties");

        stsClient.setProperties(properties);
        stsClient.setSecureConv(true);
        stsClient.setRequiresEntropy(enableEntropy);
        stsClient.setAddressingNamespace("http://www.w3.org/2005/08/addressing");

        stsClient.renewSecurityToken(securityToken);
    }

    
}
