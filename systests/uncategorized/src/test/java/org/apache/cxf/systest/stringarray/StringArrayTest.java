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
package org.apache.cxf.systest.stringarray;

import java.io.StringWriter;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.stringarray.SOAPServiceRPCLit;
import org.apache.stringarray.StringListTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class StringArrayTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }
    
    @AfterClass
    public static void stopAllServers() throws Exception {
        
    }
    
    @Test
    public void testStringArrayList() throws Exception {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus();
        BusFactory.setDefaultBus(bus);
        setBus(bus);
        StringWriter swin = new java.io.StringWriter();
        java.io.PrintWriter pwin = new java.io.PrintWriter(swin);
        LoggingInInterceptor logIn = new LoggingInInterceptor(pwin);
        
        StringWriter swout = new java.io.StringWriter();
        java.io.PrintWriter pwout = new java.io.PrintWriter(swout);
        LoggingOutInterceptor logOut = new LoggingOutInterceptor(pwout);
        
        
        getBus().getInInterceptors().add(logIn);
        getBus().getOutInterceptors().add(logOut);
        SOAPServiceRPCLit service = new SOAPServiceRPCLit();
        StringListTest port = service.getSoapPortRPCLit();
        updateAddressPort(port, PORT);
        String[] strs = new String[]{"org", "apache", "cxf"};
        String[] res =  port.stringListTest(strs);
        assertArrayEquals(strs, res);      

        assertTrue("Request message is not marshalled correctly and @XmlList does not take effect",
                     swout.toString().indexOf("<in>org apache cxf</in>") > -1);
        assertTrue("Response message is not marshalled correctly and @XmlList does not take effect",
                     swin.toString().indexOf("<out>org apache cxf</out>") > -1);
    }
    
}
