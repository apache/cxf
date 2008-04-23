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

package org.apache.cxf.systest.type_substitution;


import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class AppleFindClientServerTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(AppleServer.class));
    }

    @Test
    public void testBasicConnection() throws Exception {
        QName serviceName = new QName("http://type_substitution.systest.cxf.apache.org/",
                                      "AppleFinder");
        QName portName = new QName("http://type_substitution.systest.cxf.apache.org/", "AppleFinderPort");

        Service service = Service.create(serviceName);
        String endpointAddress = "http://localhost:9052/appleFind";

        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, endpointAddress);

       
        
        AppleFinder finder = service.getPort(AppleFinder.class);
        assertEquals(2, finder.getApple("Fuji").size());
    }

}
