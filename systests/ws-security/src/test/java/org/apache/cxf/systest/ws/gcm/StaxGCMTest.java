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

package org.apache.cxf.systest.ws.gcm;

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
 * A set of tests for GCM algorithms using custom WS-SecurityPolicy expressions. It tests both 
 * DOM + StAX clients against the StAX server
 */
public class StaxGCMTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(StaxServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static boolean unrestrictedPoliciesInstalled = 
            SecurityTestUtil.checkUnrestrictedPoliciesInstalled();
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(StaxServer.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }
    
    @org.junit.Test
    public void testAESGCM128() throws Exception {
        //
        // This test fails with the IBM JDK 7
        // IBM JDK 7 appears to require a GCMParameter class to be used, which
        // only exists in JDK 7. The Sun JDK appears to be more lenient and 
        // allows us to use the existing IVParameterSpec class.
        //
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))
            && System.getProperty("java.version") != null
            &&  System.getProperty("java.version").startsWith("1.7")) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxGCMTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = StaxGCMTest.class.getResource("DoubleItGCM.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItGCM128Port");
        DoubleItPortType gcmPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(gcmPort, PORT);
        
        // DOM
        gcmPort.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(gcmPort);
        gcmPort.doubleIt(25);
        
        ((java.io.Closeable)gcmPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAESGCM192() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }
        
        //
        // This test fails with the IBM JDK 7
        // IBM JDK 7 appears to require a GCMParameter class to be used, which
        // only exists in JDK 7. The Sun JDK appears to be more lenient and 
        // allows us to use the existing IVParameterSpec class.
        //
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))
            && System.getProperty("java.version") != null
            &&  System.getProperty("java.version").startsWith("1.7")) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxGCMTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = StaxGCMTest.class.getResource("DoubleItGCM.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItGCM192Port");
        DoubleItPortType gcmPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(gcmPort, PORT);
        
        // DOM
        gcmPort.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(gcmPort);
        gcmPort.doubleIt(25);
        
        ((java.io.Closeable)gcmPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAESGCM256() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }
        
        //
        // This test fails with the IBM JDK 7
        // IBM JDK 7 appears to require a GCMParameter class to be used, which
        // only exists in JDK 7. The Sun JDK appears to be more lenient and 
        // allows us to use the existing IVParameterSpec class.
        //
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))
            && System.getProperty("java.version") != null
            &&  System.getProperty("java.version").startsWith("1.7")) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxGCMTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = StaxGCMTest.class.getResource("DoubleItGCM.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItGCM256Port");
        DoubleItPortType gcmPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(gcmPort, PORT);
        
        // DOM
        gcmPort.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(gcmPort);
        gcmPort.doubleIt(25);
        
        ((java.io.Closeable)gcmPort).close();
        bus.shutdown(true);
    }
    
    
}
