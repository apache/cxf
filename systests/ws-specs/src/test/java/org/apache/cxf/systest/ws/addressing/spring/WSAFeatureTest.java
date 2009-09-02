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
package org.apache.cxf.systest.ws.addressing.spring;

import java.util.List;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.test.AbstractCXFTest;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.ws.addressing.soap.MAPCodec;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.junit.Test;

public class WSAFeatureTest extends AbstractCXFTest {
    @Test
    public void testServerFactory() {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.getFeatures().add(new WSAddressingFeature());
        sf.setServiceBean(new GreeterImpl());
        sf.setAddress("http://localhost:9000/test");
        sf.setStart(false);
        sf.setBus(getBus());
        
        Server server = sf.create();
        
        Endpoint endpoint = server.getEndpoint();
        checkAddressInterceptors(endpoint.getInInterceptors());        
        
    }
    
    @Test
    public void testClientProxyFactory() {
        JaxWsProxyFactoryBean cf = new JaxWsProxyFactoryBean(); 
        cf.setAddress("http://localhost:9000/test");
        cf.getFeatures().add(new WSAddressingFeature());
        cf.setServiceClass(Greeter.class);
        Greeter greeter = (Greeter) cf.create();
        Client client = ClientProxy.getClient(greeter);
        checkAddressInterceptors(client.getInInterceptors());
    }
    
    private void checkAddressInterceptors(List<Interceptor> interceptors) {
        boolean hasAg = false;
        boolean hasCodec = false;
        
        for (Interceptor i : interceptors) {
            if (i instanceof MAPAggregator) {
                hasAg = true;
            } else if (i instanceof MAPCodec) {
                hasCodec = true;
            }
        }
        assertTrue(hasAg);
        assertTrue(hasCodec);
    }
    
}
