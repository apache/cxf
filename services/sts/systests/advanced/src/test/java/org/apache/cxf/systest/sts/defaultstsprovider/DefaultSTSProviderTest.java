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
package org.apache.cxf.systest.sts.defaultstsprovider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.wss4j.common.WSS4JConstants;

import org.junit.BeforeClass;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the DefaultSecurityTokenServiceProvider
 */
public class DefaultSTSProviderTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new STSServer(
            DefaultSTSProviderTest.class.getResource("cxf-sts.xml"))));
    }

    @org.junit.Test
    public void testIssueSAML2Token() throws Exception {
        createBus(getClass().getResource("cxf-client-unit.xml").toString());

        String wsdlLocation =
            "https://localhost:" + STSPORT + "/SecurityTokenService/Transport?wsdl";

        // Request the token
        SecurityToken token =
            requestSecurityToken(bus, wsdlLocation, WSS4JConstants.WSS_SAML2_TOKEN_TYPE, 2, true);
        assertNotNull(token);

        // Validate the token
        validateSecurityToken(bus, wsdlLocation, token);
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

        Map<String, Object> properties = new HashMap<>();
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

        Map<String, Object> properties = new HashMap<>();
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

}
