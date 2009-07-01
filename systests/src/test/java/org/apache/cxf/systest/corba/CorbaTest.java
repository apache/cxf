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

package org.apache.cxf.systest.corba;

import java.io.File;
import java.net.URL;

import javax.xml.namespace.QName;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.hello_world_corba.Greeter;
import org.apache.cxf.hello_world_corba.GreeterCORBAService;
import org.apache.cxf.hello_world_corba.PingMeFault;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class CorbaTest extends AbstractBusClientServerTestBase {

       
    private static final QName SERVICE_NAME = 
        new QName("http://cxf.apache.org/hello_world_corba", 
                  "GreeterCORBAService");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            launchServer(Server.class)
        );
    }
    
    @AfterClass
    public static void cleanupFile() throws Exception {
        File file = new File("./HelloWorld.ref");
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testClientServer() throws Exception {
        System.getProperties().remove("com.sun.CORBA.POA.ORBServerId");
        System.getProperties().remove("com.sun.CORBA.POA.ORBPersistentServerPort");
        URL wsdlUrl = this.getClass().getResource("/wsdl_systest/hello_world_corba.wsdl");
        new SpringBusFactory().createBus("org/apache/cxf/systest/corba/hello_world_client.xml");

        GreeterCORBAService gcs = new GreeterCORBAService(wsdlUrl, SERVICE_NAME);
        Greeter port = gcs.getGreeterCORBAPort();


        String output = port.greetMe("Betty");
        assertTrue("Unexpected returned string: " + output, "Hello Betty".equals(output));
    }
    
    @Test
    public void testException() throws Exception {
        System.getProperties().remove("com.sun.CORBA.POA.ORBServerId");
        System.getProperties().remove("com.sun.CORBA.POA.ORBPersistentServerPort");
        URL wsdlUrl = this.getClass().getResource("/wsdl_systest/hello_world_corba.wsdl");
        new SpringBusFactory().createBus("org/apache/cxf/systest/corba/hello_world_client.xml");

        GreeterCORBAService gcs = new GreeterCORBAService(wsdlUrl, SERVICE_NAME);
        Greeter port = gcs.getGreeterCORBAPort();
        try {
            port.pingMe("USER");
        } catch (PingMeFault pe) {
            assertTrue("Caught right exception ", pe != null);
            return;
        }
        fail("Didn't catch an exception");
    }

    
}
