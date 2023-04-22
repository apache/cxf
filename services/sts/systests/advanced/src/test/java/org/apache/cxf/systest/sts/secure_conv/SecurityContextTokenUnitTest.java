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

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.systest.sts.common.TestParam;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.systest.sts.deployment.StaxSTSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertTrue;

/**
 * In this test case, a CXF client requests a SecurityContextToken from an STS.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class SecurityContextTokenUnitTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STAX_STSPORT = allocatePort(StaxSTSServer.class);

    final TestParam test;

    public SecurityContextTokenUnitTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new StaxSTSServer(
            SecurityContextTokenUnitTest.class.getResource("cxf-sts.xml"),
            SecurityContextTokenUnitTest.class.getResource("stax-cxf-sts.xml"))));
    }

    @Parameters(name = "{0}")
    public static TestParam[] data() {
        return new TestParam[] {new TestParam("", false, STSPORT),
                                new TestParam("", false, STAX_STSPORT),
        };
    }

    @org.junit.Test
    public void testSecurityContextToken() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        String wsdlLocation =
            "https://localhost:" + test.getStsPort() + "/SecurityTokenService/TransportSCT?wsdl";
        SecurityToken token =
            requestSecurityToken(bus, wsdlLocation, true);
        assertTrue(token.getSecret() != null && token.getSecret().length > 0);
    }

    @org.junit.Test
    public void testSecurityContextTokenNoEntropy() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        String wsdlLocation =
            "https://localhost:" + test.getStsPort() + "/SecurityTokenService/TransportSCT?wsdl";
        SecurityToken token =
            requestSecurityToken(bus, wsdlLocation, false);
        assertTrue(token.getSecret() != null && token.getSecret().length > 0);
    }

    @org.junit.Test
    public void testSecurityContextTokenEncrypted() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        String wsdlLocation =
            "https://localhost:" + test.getStsPort() + "/SecurityTokenService/TransportSCTEncrypted?wsdl";
        SecurityToken token =
            requestSecurityToken(bus, wsdlLocation, true);
        assertTrue(token.getSecret() != null && token.getSecret().length > 0);
    }

    @org.junit.Test
    public void testSecurityContextTokenNoEntropyEncrypted() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        String wsdlLocation =
            "https://localhost:" + test.getStsPort() + "/SecurityTokenService/TransportSCTEncrypted?wsdl";
        SecurityToken token =
            requestSecurityToken(bus, wsdlLocation, false);
        assertTrue(token.getSecret() != null && token.getSecret().length > 0);
    }

    private static SecurityToken requestSecurityToken(
        Bus bus, String wsdlLocation, boolean enableEntropy
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
        stsClient.setSecureConv(true);
        stsClient.setRequiresEntropy(enableEntropy);
        stsClient.setKeySize(128);
        stsClient.setAddressingNamespace("http://www.w3.org/2005/08/addressing");

        return stsClient.requestSecurityToken("http://localhost:8081/doubleit/services/doubleitsymmetric");
    }

}
