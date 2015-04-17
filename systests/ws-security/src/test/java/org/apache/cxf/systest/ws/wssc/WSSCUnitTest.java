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

package org.apache.cxf.systest.ws.wssc;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.common.TestParam;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.addressing.policy.MetadataConstants;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.DefaultSymmetricBinding;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.neethi.All;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.Header;
import org.apache.wss4j.policy.model.ProtectionToken;
import org.apache.wss4j.policy.model.SignedParts;
import org.apache.wss4j.policy.model.X509Token;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

/**
 * Some unit tests for SecureConversation.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class WSSCUnitTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(UnitServer.class);
    static final String PORT2 = allocatePort(UnitServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    final TestParam test;
    
    public WSSCUnitTest(TestParam type) {
        this.test = type;
    }
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(UnitServer.class, true)
        );
    }
    
    @Parameters(name = "{0}")
    public static Collection<TestParam[]> data() {
       
        return Arrays.asList(new TestParam[][] {{new TestParam(PORT, false)},
                                                {new TestParam(PORT, true)},
        });
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @Test
    public void testEndorsingSecureConveration() throws Exception {
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = WSSCUnitTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = WSSCUnitTest.class.getResource("DoubleItWSSC.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }
        
        port.doubleIt(25);
        
        ((java.io.Closeable)port).close();
    }
    
    @Test
    public void testEndorsingSecureConverationSP12() throws Exception {
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = WSSCUnitTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = WSSCUnitTest.class.getResource("DoubleItWSSC.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSP12Port");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }
        
        port.doubleIt(25);
        
        ((java.io.Closeable)port).close();
    }
    
    @Test
    public void testIssueUnitTest() throws Exception {
        
        if (test.isStreaming()) {
            return;
        }
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = WSSCUnitTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        STSClient stsClient = new STSClient(bus);
        stsClient.setSecureConv(true);
        stsClient.setLocation("https://localhost:" + PORT + "/" + "DoubleItTransport");
        
        // Add Addressing policy
        Policy p = new Policy();
        ExactlyOne ea = new ExactlyOne();
        p.addPolicyComponent(ea);
        All all = new All();
        all.addPolicyComponent(new PrimitiveAssertion(MetadataConstants.USING_ADDRESSING_2006_QNAME,
                                                      false));
        ea.addPolicyComponent(all);
        
        stsClient.setPolicy(p);
        
        stsClient.requestSecurityToken("http://localhost:" + PORT + "/" + "DoubleItTransport");
    }
    
    @Test
    public void testIssueAndCancelUnitTest() throws Exception {
        if (test.isStreaming()) {
            return;
        }
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = WSSCUnitTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        STSClient stsClient = new STSClient(bus);
        stsClient.setSecureConv(true);
        stsClient.setLocation("http://localhost:" + PORT2 + "/" + "DoubleItSymmetric");
        
        stsClient.setPolicy(createSymmetricBindingPolicy());
        
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("security.encryption.username", "bob");
        TokenCallbackHandler callbackHandler = new TokenCallbackHandler();
        properties.put("security.callback-handler", callbackHandler);
        properties.put("ws-security.signature.properties", "alice.properties");
        properties.put("ws-security.encryption.properties", "bob.properties");
        stsClient.setProperties(properties);
        
        SecurityToken securityToken = 
            stsClient.requestSecurityToken("http://localhost:" + PORT2 + "/" + "DoubleItSymmetric");
        assertNotNull(securityToken);
        callbackHandler.setSecurityToken(securityToken);
        
        assertTrue(stsClient.cancelSecurityToken(securityToken));
    }
    
    @Test
    public void testIssueAndRenewUnitTest() throws Exception {
        if (test.isStreaming()) {
            return;
        }
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = WSSCUnitTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        STSClient stsClient = new STSClient(bus);
        stsClient.setSecureConv(true);
        stsClient.setLocation("http://localhost:" + PORT2 + "/" + "DoubleItSymmetric");
        
        stsClient.setPolicy(createSymmetricBindingPolicy());
        
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("security.encryption.username", "bob");
        TokenCallbackHandler callbackHandler = new TokenCallbackHandler();
        properties.put("security.callback-handler", callbackHandler);
        properties.put("ws-security.signature.properties", "alice.properties");
        properties.put("ws-security.encryption.properties", "bob.properties");
        stsClient.setProperties(properties);
        
        SecurityToken securityToken = 
            stsClient.requestSecurityToken("http://localhost:" + PORT2 + "/" + "DoubleItSymmetric");
        assertNotNull(securityToken);
        callbackHandler.setSecurityToken(securityToken);
        
        assertNotNull(stsClient.renewSecurityToken(securityToken));
    }

    // mock up a SymmetricBinding policy to talk to the STS
    private Policy createSymmetricBindingPolicy() {
        // Add Addressing policy
        Policy p = new Policy();
        ExactlyOne ea = new ExactlyOne();
        p.addPolicyComponent(ea);
        All all = new All();
        all.addPolicyComponent(new PrimitiveAssertion(MetadataConstants.USING_ADDRESSING_2006_QNAME,
                                                      false));
        ea.addPolicyComponent(all);
        
        // X509 Token
        final X509Token x509Token = 
            new X509Token(
                SPConstants.SPVersion.SP12,
                SPConstants.IncludeTokenType.INCLUDE_TOKEN_NEVER,
                null,
                null,
                null,
                new Policy()
            );
        
        Policy x509Policy = new Policy();
        ExactlyOne x509PolicyEa = new ExactlyOne();
        x509Policy.addPolicyComponent(x509PolicyEa);
        All x509PolicyAll = new All();
        x509PolicyAll.addPolicyComponent(x509Token);
        x509PolicyEa.addPolicyComponent(x509PolicyAll);
        
        // AlgorithmSuite
        Policy algSuitePolicy = new Policy();
        ExactlyOne algSuitePolicyEa = new ExactlyOne();
        algSuitePolicy.addPolicyComponent(algSuitePolicyEa);
        All algSuitePolicyAll = new All();
        algSuitePolicyAll.addAssertion(
            new PrimitiveAssertion(new QName(SP12Constants.SP_NS, SP12Constants.ALGO_SUITE_BASIC128)));
        algSuitePolicyEa.addPolicyComponent(algSuitePolicyAll);
        AlgorithmSuite algorithmSuite = new AlgorithmSuite(SPConstants.SPVersion.SP12, algSuitePolicy);
        
        // Symmetric Binding
        Policy bindingPolicy = new Policy();
        ExactlyOne bindingPolicyEa = new ExactlyOne();
        bindingPolicy.addPolicyComponent(bindingPolicyEa);
        All bindingPolicyAll = new All();
        
        bindingPolicyAll.addPolicyComponent(new ProtectionToken(SPConstants.SPVersion.SP12, x509Policy));
        bindingPolicyAll.addPolicyComponent(algorithmSuite);
        bindingPolicyAll.addAssertion(
            new PrimitiveAssertion(SP12Constants.INCLUDE_TIMESTAMP));
        bindingPolicyAll.addAssertion(
            new PrimitiveAssertion(SP12Constants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY));
        bindingPolicyEa.addPolicyComponent(bindingPolicyAll);
        
        DefaultSymmetricBinding binding = 
            new DefaultSymmetricBinding(SPConstants.SPVersion.SP12, bindingPolicy);
        binding.setOnlySignEntireHeadersAndBody(true);
        binding.setProtectTokens(false);
        all.addPolicyComponent(binding);
        
        List<Header> headers = new ArrayList<Header>();
        SignedParts signedParts = 
            new SignedParts(SPConstants.SPVersion.SP12, true, null, headers, false);
        all.addPolicyComponent(signedParts);
        
        return p;
    }
    
    private static class TokenCallbackHandler implements CallbackHandler {
        
        private SecurityToken securityToken;
        
        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                WSPasswordCallback pc = (WSPasswordCallback)callbacks[i];
                if (securityToken != null && pc.getIdentifier().equals(securityToken.getId())) {
                    pc.setKey(securityToken.getSecret());
                } else {
                    new org.apache.cxf.systest.ws.common.KeystorePasswordCallback().handle(callbacks);
                }
                    
            }
        }

        public void setSecurityToken(SecurityToken securityToken) {
            this.securityToken = securityToken;
        }
        
    };
}
