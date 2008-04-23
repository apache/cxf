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
package org.apache.cxf.ws.policy.spring;

import java.util.List;

import junit.framework.Assert;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.PolicyEngineImpl;
import org.apache.cxf.ws.policy.WSPolicyFeature;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.apache.neethi.Policy;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class PolicyFeatureTest extends Assert {
    private Bus bus;
    @After
    public void tearDown() {
        bus.shutdown(true);        
        BusFactory.setDefaultBus(null);
    }
    
    @Test
    public void testServerFactory() {
        bus = new CXFBusFactory().createBus();
        PolicyEngineImpl pei = new PolicyEngineImpl();
        bus.setExtension(pei, PolicyEngine.class);
        pei.setBus(bus);
        
        Policy p = new Policy();
        p.setId("test");
        
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.getFeatures().add(new WSPolicyFeature(p));
        sf.setServiceBean(new GreeterImpl());
        sf.setAddress("http://localhost/test");
        sf.setStart(false);
        sf.setBus(bus);
        Server server = sf.create();
        
        List<ServiceInfo> sis = server.getEndpoint().getService().getServiceInfos();
        ServiceInfo info = sis.get(0);
        
        Policy p2 = info.getExtensor(Policy.class);
        assertEquals(p, p2);
    }
    

    @Test
    public void testServerFactoryWith2007Xml() {
        bus = new SpringBusFactory().createBus("/org/apache/cxf/ws/policy/spring/server.xml");
        
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceBean(new GreeterImpl());
        sf.setAddress("http://localhost/test");
        
        sf.setBus(bus);
        
        Configurer c = bus.getExtension(Configurer.class);
        c.configureBean("test", sf);
        sf.setStart(false);
        
        List<AbstractFeature> features = sf.getFeatures();
        assertEquals(1, features.size());
        
        Server server = sf.create();
        
        PolicyEngine pe = bus.getExtension(PolicyEngine.class);
        assertNotNull(pe);
        
        List<ServiceInfo> sis = server.getEndpoint().getService().getServiceInfos();
        ServiceInfo info = sis.get(0);
        
        Policy p2 = info.getExtensor(Policy.class);
        assertNotNull(p2);
    }

    @Test
    public void testServerFactoryWith2004Xml() {
        bus = 
            new SpringBusFactory().createBus("/org/apache/cxf/ws/policy/spring/server.xml");
        
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceBean(new GreeterImpl());
        sf.setAddress("http://localhost/test");        
        sf.setBus(bus);
        
        Configurer c = bus.getExtension(Configurer.class);
        c.configureBean("test2004", sf);
        
        List<AbstractFeature> features = sf.getFeatures();
        assertEquals(1, features.size());
        sf.setStart(false);
        
        Server server = sf.create();
        
        PolicyEngine pe = bus.getExtension(PolicyEngine.class);
        assertNotNull(pe);
        
        List<ServiceInfo> sis = server.getEndpoint().getService().getServiceInfos();
        ServiceInfo info = sis.get(0);
        
        Policy p2 = info.getExtensor(Policy.class);
        assertNotNull(p2);
    }
    
    @Test
    public void testPolicyReference() {
        bus = 
            new SpringBusFactory().createBus("/org/apache/cxf/ws/policy/spring/server.xml");
        
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceBean(new GreeterImpl());
        sf.setAddress("http://localhost/test");        
        sf.setBus(bus);
        
        Configurer c = bus.getExtension(Configurer.class);
        c.configureBean("testExternal", sf);
        
        List<AbstractFeature> features = sf.getFeatures();
        assertEquals(1, features.size());
        sf.setStart(false);
        Server server = sf.create();
        
        PolicyEngine pe = bus.getExtension(PolicyEngine.class);
        assertNotNull(pe);
        
        List<ServiceInfo> sis = server.getEndpoint().getService().getServiceInfos();
        ServiceInfo info = sis.get(0);
        
        Policy p = info.getExtensor(Policy.class);
        assertNotNull(p);
        assertEquals("External", p.getId());
    }
}
