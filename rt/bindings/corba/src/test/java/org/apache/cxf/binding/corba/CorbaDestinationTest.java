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
package org.apache.cxf.binding.corba;

import org.apache.cxf.binding.corba.utils.OrbConfig;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class CorbaDestinationTest {

    protected static TestUtils testUtils;
    EndpointInfo endpointInfo;
    OrbConfig orbConfig;


    @Before
    public void setUp() throws Exception {
        testUtils = new TestUtils();
        orbConfig = new OrbConfig();
    }

    @Test
    public void testDestination() throws Exception {
        endpointInfo = testUtils.setupServiceInfo("http://cxf.apache.org/bindings/corba/simple",
                        "/wsdl_corbabinding/simpleIdl.wsdl", "SimpleCORBAService",
                        "SimpleCORBAPort");
        CorbaDestination destination = new CorbaDestination(endpointInfo, orbConfig);

        EndpointReferenceType rtype = destination.getAddress();
        assertNotNull("EndpointReferenceType should not be null", rtype);
        BindingInfo bindingInfo = destination.getBindingInfo();
        assertNotNull("BindingInfo should not be null", bindingInfo);
        EndpointInfo e2 = destination.getEndPointInfo();
        assertNotNull("EndpointInfo should not be null", e2);

        Message m = new MessageImpl();
        CorbaServerConduit serverConduit = (CorbaServerConduit)destination.getBackChannel(m);
        assertNotNull("CorbaServerConduit should not be null", serverConduit);
    }

   /*
    @Test
    public void testSetMessageObserverActivate() throws Exception {
       endpointInfo = testUtils.setupServiceInfo("http://cxf.apache.org/bindings/corba/simple",
                        "/wsdl/simpleIdl.wsdl", "SimpleCORBAService",
                        "SimpleCORBAPort");
       CorbaDestination destination = new CorbaDestination(endpointInfo);
       String addr = destination.getAddressType().getLocation();
       assertEquals(addr, "corbaloc::localhost:40000/Simple");

       Bus bus = BusFactory.newInstance().getDefaultBus();
       Service service = new ServiceImpl();
       Endpoint endpoint = new EndpointImpl(bus, service, endpointInfo);
       MessageObserver observer = new ChainInitiationObserver(endpoint, bus);
       destination.setMessageObserver(observer);
       assertNotNull("orb should not be null",  destination.getOrb());

       try {
           File file = new File("endpoint.ior");
           assertEquals(true,file.exists());
       } finally {
           new File("endpoint.ior").deleteOnExit();
       }

       addr = destination.getAddressType().getLocation();
       addr = addr.substring(0,4);
       assertEquals(addr, "IOR:");
       destination.shutdown();
   }*/

}
