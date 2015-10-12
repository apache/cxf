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
package org.apache.cxf.systest.sts.kerberos;

import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.common.TokenTestUtils;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.auth.SpnegoAuthSupplier;
import org.example.contract.doubleit.DoubleItPortType;
import org.ietf.jgss.GSSName;
import org.junit.BeforeClass;

/**
 * This tests credential delegation. The client enables credential delegation + sends a Kerberos
 * token to an Intermediary via WS-Security. The Intermediary validates the token, and then
 * uses the delgated credential to obtain a ticket to in turn retrieve a SAML token from the
 * STS. The SAML token is used to secure access to the backend service.
 * 
 * The tests are @Ignored by default, as a KDC is needed. To replicate the test scenario, set up a KDC with 
 * user principal "alice" (keytab in "/etc/alice.keytab"), and host service "bob@service.ws.apache.org" 
 * (keytab in "/etc/bob.keytab").
 */
@org.junit.Ignore
public class KerberosDelegationTokenTest extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSServer.class);
    static final String PORT = allocatePort(Server.class);
    static final String INTERMEDIARY_PORT = allocatePort(Intermediary.class);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

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
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(Intermediary.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void testKerberosToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosDelegationTokenTest.class.getResource("cxf-intermediary-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = KerberosDelegationTokenTest.class.getResource("DoubleItIntermediary.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportKerberosPort");
        DoubleItPortType transportSaml2Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml2Port, INTERMEDIARY_PORT);
        
        TokenTestUtils.updateSTSPort((BindingProvider)transportSaml2Port, STSPORT);

        doubleIt(transportSaml2Port, 25);
        
        ((java.io.Closeable)transportSaml2Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testKerberosTokenJAXRS() throws Exception {
        
        final String configLocation = "org/apache/cxf/systest/sts/kerberos/cxf-intermediary-jaxrs-client.xml";
        final String address = "https://localhost:" + INTERMEDIARY_PORT + "/doubleit/services/doubleit-rs";
        final int numToDouble = 35;  

        WebClient client = WebClient.create(address, configLocation);
        client.type("text/plain").accept("text/plain");
        
        Map<String, Object> requestContext = WebClient.getConfig(client).getRequestContext();
        requestContext.put("auth.spnego.useKerberosOid", "true");
        requestContext.put("auth.spnego.requireCredDelegation", "true");
        
        SpnegoAuthSupplier authSupplier = new SpnegoAuthSupplier();
        authSupplier.setServicePrincipalName("bob@service.ws.apache.org");
        authSupplier.setServiceNameType(GSSName.NT_HOSTBASED_SERVICE);
        WebClient.getConfig(client).getHttpConduit().setAuthSupplier(authSupplier);
        
        int resp = client.post(numToDouble, Integer.class);
        org.junit.Assert.assertEquals(2 * numToDouble, resp);
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2, resp);
    }
}
