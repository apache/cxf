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
package org.apache.cxf.systest.sts.custom_onbehalfof;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.systest.sts.deployment.DoubleItServer;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * In these test cases, a CXF client requests a Security Token from an STS, passing a username that
 * it has obtained from an unknown client as an "OnBehalfOf" element. This username is obtained
 * by parsing the SecurityConstants.USERNAME property. The client then invokes on the service
 * provider using the returned (custom BinarySecurityToken) token from the STS.
 *
 * In the first test-case, the service provider dispatches the received BinarySecurityToken to the STS
 * for validation, and receives a transformed SAML Token in response. In the second test-case, the
 * service just validates the Token locally.
 */
public class CustomOnBehalfOfTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            CustomOnBehalfOfTest.class.getResource("cxf-service.xml"))));
        assertTrue(launchServer(new STSServer(
            CustomOnBehalfOfTest.class.getResource("cxf-sts.xml"))));
    }

    @org.junit.Test
    public void testUsernameOnBehalfOfSTS() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = CustomOnBehalfOfTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportCustomBSTPort");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        // Transport port
        ((BindingProvider)transportPort).getRequestContext().put(
            SecurityConstants.USERNAME, "alice"
        );
        doubleIt(transportPort, 25);

        ((java.io.Closeable)transportPort).close();
    }

    @org.junit.Test
    public void testUsernameOnBehalfOfLocal() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = CustomOnBehalfOfTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportCustomBSTLocalPort");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        // Transport port
        ((BindingProvider)transportPort).getRequestContext().put(
            SecurityConstants.USERNAME, "alice"
        );
        doubleIt(transportPort, 25);

        ((java.io.Closeable)transportPort).close();
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
