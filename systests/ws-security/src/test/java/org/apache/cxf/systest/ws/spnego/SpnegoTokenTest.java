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

package org.apache.cxf.systest.ws.spnego;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * A set of tests for Spnego Tokens. The tests are @Ignore'd, as they require a running KDC. To run the
 * tests, set up a KDC of realm "WS.APACHE.ORG", with principal "alice" and service principal 
 * "bob/service.ws.apache.org". Create keytabs for both principals in "/etc/alice.keytab" and
 * "/etc/bob.keytab" (this can all be edited in src/test/resource/kerberos.jaas". Then disable the
 * @Ignore annotations and run the tests with:
 *  
 * mvn test -Pnochecks -Dtest=SpnegoTokenTest 
 *     -Djava.security.auth.login.config=src/test/resources/kerberos.jaas
 * 
 * It tests both DOM + StAX clients against the DOM server
 */
@org.junit.Ignore
public class SpnegoTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    private static boolean unrestrictedPoliciesInstalled = 
            SecurityTestUtil.checkUnrestrictedPoliciesInstalled();
    
    @BeforeClass
    public static void startServers() throws Exception {
        if (unrestrictedPoliciesInstalled) {
            assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(Server.class, true)
            );
        }
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        if (unrestrictedPoliciesInstalled) {
            SecurityTestUtil.cleanup();
            stopAllServers();
        }
    }

    @org.junit.Test
    public void testSpnegoOverSymmetric() throws Exception {
        
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SpnegoTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SpnegoTokenTest.class.getResource("DoubleItSpnego.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSpnegoSymmetricPort");
        DoubleItPortType spnegoPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(spnegoPort, PORT);
        
        // DOM
        spnegoPort.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(spnegoPort);
        spnegoPort.doubleIt(25);
        
        ((java.io.Closeable)spnegoPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSpnegoOverSymmetricDerived() throws Exception {
        
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SpnegoTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SpnegoTokenTest.class.getResource("DoubleItSpnego.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSpnegoSymmetricDerivedPort");
        DoubleItPortType spnegoPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(spnegoPort, PORT);
        
        // DOM
        spnegoPort.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(spnegoPort);
        spnegoPort.doubleIt(25);
        
        ((java.io.Closeable)spnegoPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSpnegoOverSymmetricEncryptBeforeSigning() throws Exception {
        
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SpnegoTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SpnegoTokenTest.class.getResource("DoubleItSpnego.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSpnegoSymmetricEncryptBeforeSigningPort");
        DoubleItPortType spnegoPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(spnegoPort, PORT);
        
        // DOM
        spnegoPort.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(spnegoPort);
        spnegoPort.doubleIt(25);
        
        ((java.io.Closeable)spnegoPort).close();
        bus.shutdown(true);
    }
    
}
