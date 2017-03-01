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
package org.apache.cxf.systest.sts.custom;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.common.TokenTestUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.trust.STSClient;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * This test sends a custom parameter indicating the "realm" of the user, which is interpreted by the
 * STS's CustomUTValidator.
 */
public class CustomParameterTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

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
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    // Here the custom parameter in the RST is parsed by the CustomUTValidator
    @org.junit.Test
    public void testCustomParameterInRSTValidator() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CustomParameterTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = CustomParameterTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportCustomParameterPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportClaimsPort, PORT);

        TokenTestUtils.updateSTSPort((BindingProvider)transportClaimsPort, STSPORT);

        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation("https://localhost:" + STSPORT + "/SecurityTokenService/UT?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}UT_Port");

        Map<String, Object> properties = new HashMap<>();
        properties.put("security.username", "alice");
        properties.put("security.callback-handler", "org.apache.cxf.systest.sts.common.CommonCallbackHandler");
        properties.put("security.sts.token.username", "myclientkey");
        properties.put("security.sts.token.properties", "clientKeystore.properties");
        properties.put("security.sts.token.usecert", "true");
        stsClient.setProperties(properties);

        ((BindingProvider)transportClaimsPort).getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);

        // Successful test

        // Add custom content to the RST
        stsClient.setCustomContent("<realm xmlns=\"http://cxf.apache.org/custom\">custom-realm</realm>");
        doubleIt(transportClaimsPort, 25);

        ((java.io.Closeable)transportClaimsPort).close();
        bus.shutdown(true);
    }

    // Here the custom parameter in the RST is parsed by the CustomUTValidator
    @org.junit.Test
    public void testCustomParameterInRST2Validator() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CustomParameterTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = CustomParameterTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportCustomParameterPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportClaimsPort, PORT);

        TokenTestUtils.updateSTSPort((BindingProvider)transportClaimsPort, STSPORT);

        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation("https://localhost:" + STSPORT + "/SecurityTokenService/UT?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}UT_Port");

        Map<String, Object> properties = new HashMap<>();
        properties.put("security.username", "alice");
        properties.put("security.callback-handler", "org.apache.cxf.systest.sts.common.CommonCallbackHandler");
        properties.put("security.sts.token.username", "myclientkey");
        properties.put("security.sts.token.properties", "clientKeystore.properties");
        properties.put("security.sts.token.usecert", "true");
        stsClient.setProperties(properties);

        ((BindingProvider)transportClaimsPort).getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);

        // Failing test

        // Add custom content to the RST
        stsClient.setCustomContent("<realm xmlns=\"http://cxf.apache.org/custom\">custom-unknown-realm</realm>");
        try {
            doubleIt(transportClaimsPort, 25);
            fail("Failure expected on the wrong realm");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)transportClaimsPort).close();
        bus.shutdown(true);
    }

    // Here the custom parameter in the RST is parsed by the CustomClaimsHandler
    @org.junit.Test
    public void testCustomParameterInRSTClaimsHandler() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CustomParameterTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = CustomParameterTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportCustomParameterClaimsPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportClaimsPort, PORT);

        TokenTestUtils.updateSTSPort((BindingProvider)transportClaimsPort, STSPORT);

        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation("https://localhost:" + STSPORT + "/SecurityTokenService/Transport?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");

        Map<String, Object> properties = new HashMap<>();
        properties.put("security.username", "alice");
        properties.put("security.callback-handler", "org.apache.cxf.systest.sts.common.CommonCallbackHandler");
        properties.put("security.sts.token.username", "myclientkey");
        properties.put("security.sts.token.properties", "clientKeystore.properties");
        properties.put("security.sts.token.usecert", "true");
        stsClient.setProperties(properties);

        ((BindingProvider)transportClaimsPort).getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);

        // Successful test

        // Add custom content to the RST
        stsClient.setCustomContent("<realm xmlns=\"http://cxf.apache.org/custom\">custom-realm</realm>");
        doubleIt(transportClaimsPort, 25);

        ((java.io.Closeable)transportClaimsPort).close();
        bus.shutdown(true);
    }

    // Here the custom parameter in the RST is parsed by the CustomClaimsHandler
    @org.junit.Test
    public void testCustomParameterInRSTClaimsHandler2() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CustomParameterTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = CustomParameterTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportCustomParameterClaimsPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportClaimsPort, PORT);

        TokenTestUtils.updateSTSPort((BindingProvider)transportClaimsPort, STSPORT);

        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation("https://localhost:" + STSPORT + "/SecurityTokenService/Transport?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");

        Map<String, Object> properties = new HashMap<>();
        properties.put("security.username", "alice");
        properties.put("security.callback-handler", "org.apache.cxf.systest.sts.common.CommonCallbackHandler");
        properties.put("security.sts.token.username", "myclientkey");
        properties.put("security.sts.token.properties", "clientKeystore.properties");
        properties.put("security.sts.token.usecert", "true");
        stsClient.setProperties(properties);

        ((BindingProvider)transportClaimsPort).getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);

        // Failing test

        // Add custom content to the RST
        stsClient.setCustomContent("<realm xmlns=\"http://cxf.apache.org/custom\">custom-unknown-realm</realm>");
        try {
            doubleIt(transportClaimsPort, 25);
            fail("Failure expected on the wrong realm");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)transportClaimsPort).close();
        bus.shutdown(true);
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2, resp);
    }
}
