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
package org.apache.cxf.systest.sts.jwt;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;

import org.junit.BeforeClass;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests to get a JWT token from the STS
 */
public class JWTUnitTest extends AbstractBusClientServerTestBase {

    public static final String JWT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";
    static final String STSPORT = allocatePort(STSServer.class);
    private static final String DEFAULT_ADDRESS =
        "https://localhost:8081/doubleit/services/doubleittransportsaml1";

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new STSServer()));
    }

    @org.junit.Test
    public void testIssueJWTToken() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        // Issue the token
        SecurityToken token =
            requestSecurityToken(JWT_TOKEN_TYPE, bus, DEFAULT_ADDRESS, null, null);
        assertNotNull(token);
        assertNotNull(token.getToken());

        // Validate the token
        token = validateSecurityToken(token, bus, null, null);
        assertNotNull(token);
        assertNotNull(token.getToken());
    }

    private SecurityToken requestSecurityToken(
        String tokenType,
        Bus bus,
        String endpointAddress,
        Map<String, Object> msgProperties,
        String wsdlPort
    ) throws Exception {
        STSClient stsClient = new STSClient(bus);
        String port = STSPORT;

        stsClient.setWsdlLocation("https://localhost:" + port + "/SecurityTokenService/Transport?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        if (wsdlPort != null) {
            stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}" + wsdlPort);
        } else {
            stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");
        }

        Map<String, Object> properties = msgProperties;
        if (properties == null) {
            properties = new HashMap<>();
            properties.put(SecurityConstants.USERNAME, "alice");
            properties.put(
                           SecurityConstants.CALLBACK_HANDLER,
                           "org.apache.cxf.systest.sts.common.CommonCallbackHandler"
            );
        }

        stsClient.setProperties(properties);
        stsClient.setTokenType(tokenType);
        stsClient.setSendKeyType(false);

        return stsClient.requestSecurityToken(endpointAddress);
    }

    private SecurityToken validateSecurityToken(
        SecurityToken token,
        Bus bus,
        Map<String, Object> msgProperties,
        String wsdlPort
    ) throws Exception {
        STSClient stsClient = new STSClient(bus);
        String port = STSPORT;

        stsClient.setWsdlLocation("https://localhost:" + port + "/SecurityTokenService/Transport?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        if (wsdlPort != null) {
            stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}" + wsdlPort);
        } else {
            stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");
        }

        Map<String, Object> properties = msgProperties;
        if (properties == null) {
            properties = new HashMap<>();
            properties.put(SecurityConstants.USERNAME, "alice");
            properties.put(
                SecurityConstants.CALLBACK_HANDLER,
                "org.apache.cxf.systest.sts.common.CommonCallbackHandler"
            );
        }

        stsClient.setProperties(properties);
        stsClient.setSendKeyType(false);

        return stsClient.validateSecurityToken(token).get(0);
    }
}
