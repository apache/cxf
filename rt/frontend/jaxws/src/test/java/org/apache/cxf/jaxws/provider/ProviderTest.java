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
package org.apache.cxf.jaxws.provider;

import org.w3c.dom.Node;

import org.apache.cxf.jaxws.AbstractJaxWsTest;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.junit.Test;

public class ProviderTest extends AbstractJaxWsTest {
    @Test
    public void testInvocation() throws Exception {
        EndpointImpl ep = new EndpointImpl(getBus(), new PayloadProvider(), (String) null);
        ep.publish("local://localhost:9000/Provider");
        
        Node response = invoke("local://localhost:9000/Provider",
                               LocalTransportFactory.TRANSPORT_ID, 
                               "/org/apache/cxf/jaxws/sayHi.xml");

        assertNotNull(response);
        assertNoFault(response);

        addNamespace("j", "http://service.jaxws.cxf.apache.org/");
        assertValid("//s:Body/j:sayHi", response);
    }
    
    @Test
    public void testCXF1852() throws Exception {
        EndpointImpl ep = new EndpointImpl(getBus(), new PayloadProvider2(), (String) null);
        ep.publish("local://localhost:9001/Provider2");
        
        Node response = invoke("local://localhost:9001/Provider2",
                               LocalTransportFactory.TRANSPORT_ID, 
                               "/org/apache/cxf/jaxws/sayHi.xml");

        assertNotNull(response);
        assertNoFault(response);

        addNamespace("j", "http://service.jaxws.cxf.apache.org/");
        assertValid("//s:Body/j:sayHi", response);        
    }
    
    public static class PayloadProvider2 extends PayloadProvider {
        
    }
}
