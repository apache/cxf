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

import java.io.PrintWriter;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.ws.transfer.manager.ResourceManager;
import org.apache.cxf.ws.transfer.resource.ResourceRemote;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactory;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactoryImpl;
import org.apache.cxf.ws.transfer.resourcefactory.resolver.SimpleResourceResolver;
import org.junit.After;
import org.junit.Before;

/**
 *
 * @author erich
 */
public class IntegrationBaseTest {
    
    public static final String RESOURCE_FACTORY_ADDRESS = "local://ResourceFactory";
    
    public static final String RESOURCE_ADDRESS = "local://ResourceLocal";
    
    public static final String RESOURCE_REMOTE_ADDRESS = "local://ResourceRemote";
    
    public static final String RESOURCE_REMOTE_MANAGER_ADDRESS = "local://ResourceRemote_factory";
    
    protected Bus bus;
    
    protected LoggingInInterceptor logInInterceptor;
    
    protected LoggingOutInterceptor logOutInterceptor;
    
    @Before
    public void before() {
        bus = BusFactory.getDefaultBus();
        logInInterceptor = new LoggingInInterceptor(new PrintWriter(System.out));
        logInInterceptor.setPrettyLogging(true);
        logOutInterceptor = new LoggingOutInterceptor(new PrintWriter(System.out));
        logOutInterceptor.setPrettyLogging(true);
    }
    
    @After
    public void after() {
        bus.shutdown(true);
        bus = null;
    }
    
    protected Server createLocalResourceFactory(ResourceManager manager) {
        ResourceFactoryImpl implementor = new ResourceFactoryImpl();
        implementor.setResourceResolver(new SimpleResourceResolver(RESOURCE_ADDRESS, manager));
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceClass(ResourceFactory.class);
        factory.setAddress(RESOURCE_FACTORY_ADDRESS);
        factory.setServiceBean(implementor);
        return factory.create();
    }
    
    protected Server createRemoteResourceFactory() {
        ResourceFactoryImpl implementor = new ResourceFactoryImpl();
        implementor.setResourceResolver(new SimpleResourceResolver(RESOURCE_REMOTE_ADDRESS, null));
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceClass(ResourceFactory.class);
        factory.setAddress(RESOURCE_FACTORY_ADDRESS);
        factory.setServiceBean(implementor);
        return factory.create();
    }
    
    protected Server createRemoteResource(ResourceManager manager) {
        ResourceRemote implementor = new ResourceRemote();
        implementor.setManager(manager);
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceClass(ResourceFactory.class);
        factory.setAddress(RESOURCE_REMOTE_MANAGER_ADDRESS);
        factory.setServiceBean(implementor);
        return factory.create();
    }
}
