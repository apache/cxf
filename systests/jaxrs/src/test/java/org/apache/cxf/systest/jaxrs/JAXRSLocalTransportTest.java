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

package org.apache.cxf.systest.jaxrs;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.local.LocalConduit;
import org.apache.cxf.transport.local.LocalTransportFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JAXRSLocalTransportTest extends AbstractBusClientServerTestBase {
 
    private Server localServer;
    
    @Before
    public void setUp() {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(BookStore.class);
        sf.setResourceProvider(BookStore.class,
                               new SingletonResourceProvider(new BookStore(), true));
        sf.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        sf.setAddress("local://books");
        localServer = sf.create();
    }
    
    @After
    public void tearDown() {
        if (localServer != null) {
            localServer.stop();
        }
    }
    
    @Test
    public void testProxy() throws Exception {
        BookStore localProxy = 
            JAXRSClientFactory.create("local://books", BookStore.class);
        
        WebClient.getConfig(localProxy).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        
        Book book = localProxy.getBook("123");
        assertEquals(123L, book.getId());
    }
}
