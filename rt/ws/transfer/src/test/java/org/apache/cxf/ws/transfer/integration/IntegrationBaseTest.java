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

package org.apache.cxf.ws.transfer.integration;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.ws.transfer.manager.ResourceManager;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactory;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactoryImpl;
import org.apache.cxf.ws.transfer.resourcefactory.resolver.SimpleResourceResolver;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 *
 * @author erich
 */
public class IntegrationBaseTest {
    
    public static final String RESOURCE_FACTORY_ADDRESS = "local://ResourceFactory";
    
    public static final String RESOURCE_ADDRESS = "local://ResourceLocal";
    
    protected static Bus bus;
    
    protected static Server resourceFactory;
    
    protected static ResourceManager manager;
    
    @BeforeClass
    public static void beforeClass() {
        bus = BusFactory.getDefaultBus();
    }
    
    @AfterClass
    public static void afterClass() {
        bus.shutdown(true);
        bus = null;
    }
    
    protected void createResourceFactory() {
        ResourceFactoryImpl implementor = new ResourceFactoryImpl();
        implementor.setResourceResolver(new SimpleResourceResolver(RESOURCE_ADDRESS, manager));
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceClass(ResourceFactory.class);
        factory.setAddress(RESOURCE_FACTORY_ADDRESS);
        factory.setServiceBean(implementor);
        resourceFactory = factory.create();
    }
    
}
