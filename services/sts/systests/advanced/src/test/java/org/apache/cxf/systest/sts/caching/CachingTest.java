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
package org.apache.cxf.systest.sts.caching;

import java.net.URL;
import java.util.Date;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSClient;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * Test various Caching properties relating to the STSClient and also at the validation side.
 */
public class CachingTest extends AbstractBusClientServerTestBase {
    
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

    // @Ignore'd because failing too often on slow Jenkins machines
    @org.junit.Test
    @org.junit.Ignore
    public void testSTSClientCaching() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CachingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = CachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1Port");
        DoubleItPortType port = 
            service.getPort(portQName, DoubleItPortType.class);
        ((BindingProvider)port).getRequestContext().put("thread.local.request.context", "true");
        updateAddressPort(port, PORT);
        
        // Make a successful invocation
        doubleIt(port, 25);
        
        // Change the STSClient so that it can no longer find the STS
        BindingProvider p = (BindingProvider)port;
        clearSTSClient(p, bus);
        
        // This should succeed as the token is cached
        doubleIt(port, 30);
        
        // This should fail as the cached token is manually removed
        Client client = ClientProxy.getClient(port);
        Endpoint ep = client.getEndpoint();
        ep.remove(SecurityConstants.TOKEN_ID);
        ep.remove(SecurityConstants.TOKEN);

        try {
            doubleIt(port, 35);
            fail("Expected failure on clearing the cache");
        } catch (SOAPFaultException ex) {
            // Expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testDisableProxyCaching() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CachingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = CachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1Port2");
        DoubleItPortType port = 
            service.getPort(portQName, DoubleItPortType.class);
        ((BindingProvider)port).getRequestContext().put("thread.local.request.context", "true");
        updateAddressPort(port, PORT);
        
        // Disable storing tokens per-proxy
        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, "false"
        );
        
        // Make a successful invocation
        doubleIt(port, 25);
        
        // Change the STSClient so that it can no longer find the STS
        BindingProvider p = (BindingProvider)port;
        clearSTSClient(p, bus);
        
        // This should fail as it can't get the token
        try {
            doubleIt(port, 35);
            fail("Expected failure");
        } catch (SOAPFaultException ex) {
            // Expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testImminentExpiry() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CachingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = CachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1Port");
        DoubleItPortType port = 
            service.getPort(portQName, DoubleItPortType.class);
        ((BindingProvider)port).getRequestContext().put("thread.local.request.context", "true");
        updateAddressPort(port, PORT);
        
        // Make a successful invocation
        doubleIt(port, 25);
        
        Client client = ClientProxy.getClient(port);
        Endpoint ep = client.getEndpoint();
        String id = (String)ep.get(SecurityConstants.TOKEN_ID);
        TokenStore store = (TokenStore)ep.getEndpointInfo().getProperty(TokenStore.class.getName());
        SecurityToken tok = store.getToken(id);
        assertNotNull(tok);
        
        // Make the token "about to expire"
        Date expiredDate = new Date();
        expiredDate.setTime(expiredDate.getTime() + 5000L);
        tok.setExpires(expiredDate);
        assertTrue(tok.isAboutToExpire(10L));
        
        doubleIt(port, 25);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    private void clearSTSClient(BindingProvider p, Bus bus) throws BusException, EndpointException {
        p.getRequestContext().put(SecurityConstants.STS_CLIENT, new STSClient(bus));
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2, resp);
    }
}
