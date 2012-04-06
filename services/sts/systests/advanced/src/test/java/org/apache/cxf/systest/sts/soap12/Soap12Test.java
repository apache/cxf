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
package org.apache.cxf.systest.sts.soap12;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;

import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * This is a test for invoking on an STS using SOAP 1.2 via the TransportBinding. The CXF client gets a 
 * token from the STS over TLS, and then sends it to the CXF endpoint over TLS.
 */
public class Soap12Test extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSServer.class);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    private static final String SAML1_TOKEN_TYPE = 
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";
    private static final String PUBLIC_KEY_KEYTYPE = 
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey";
    private static final String BEARER_KEYTYPE = 
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer";
    
    private static final String PORT = allocatePort(Server.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(Server.class, true)
        );
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
    public void testSAML2() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = Soap12Test.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = Soap12Test.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2Port");
        DoubleItPortType transportSaml2Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml2Port, PORT);
        
        doubleIt(transportSaml2Port, 30);
    }
    
    /**
     * Test the endpoint address sent to the STS as part of AppliesTo. If the STS does not
     * recognise the endpoint address it does not issue a token.
     */
    @org.junit.Test
    public void testFaultCode() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = Soap12Test.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        try {
            String badAddress = 
                "https://localhost:" + PORT + "/doubleit/services/doubleitbadtransportsaml1";
            requestSecurityToken(SAML1_TOKEN_TYPE, BEARER_KEYTYPE, bus, badAddress);
            fail("Failure expected on a bad endpoint address");
        } catch (SoapFault ex) {
            // expected
        }
    }
    
    private SecurityToken requestSecurityToken(
        String tokenType, 
        String keyType, 
        Bus bus,
        String endpointAddress
    ) throws Exception {
        return requestSecurityToken(tokenType, keyType, null, bus, endpointAddress, null);
    }

    private SecurityToken requestSecurityToken(
        String tokenType, 
        String keyType,
        Element supportingToken,
        Bus bus,
        String endpointAddress,
        String context
    ) throws Exception {
        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation(
            "https://localhost:" + STSPORT + "/SecurityTokenService/TransportSoap12?wsdl"
        );
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Soap12_Port");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(
            SecurityConstants.CALLBACK_HANDLER, 
            "org.apache.cxf.systest.sts.common.CommonCallbackHandler"
        );
        properties.put(SecurityConstants.ENCRYPT_PROPERTIES, "clientKeystore.properties");
        properties.put(SecurityConstants.ENCRYPT_USERNAME, "mystskey");

        if (PUBLIC_KEY_KEYTYPE.equals(keyType)) {
            properties.put(SecurityConstants.STS_TOKEN_USERNAME, "myclientkey");
            properties.put(SecurityConstants.STS_TOKEN_PROPERTIES, "clientKeystore.properties");
            stsClient.setUseCertificateForConfirmationKeyInfo(true);
        }
        if (supportingToken != null) {
            stsClient.setOnBehalfOf(supportingToken);
        }
        if (context != null) {
            stsClient.setContext(context);
        }

        stsClient.setProperties(properties);
        stsClient.setTokenType(tokenType);
        stsClient.setKeyType(keyType);
        stsClient.setAddressingNamespace("http://www.w3.org/2005/08/addressing");

        return stsClient.requestSecurityToken(endpointAddress);
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }
}
