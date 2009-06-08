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


import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.corba.CorbaBindingFactory;
import org.apache.cxf.binding.corba.utils.OrbConfig;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.Test;


public class CorbaBindingFactoryConfigurerTest extends AbstractBusClientServerTestBase {

    
    public CorbaBindingFactoryConfigurerTest() {
    }

    
    
    @Test
    public void testOrbConfiguration() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL cxfConfig = null;
        
        cxfConfig = ClassLoaderUtils.getResource("corba_binding_factory_configurer.xml", this.getClass());
        
        bus = bf.createBus(cxfConfig);
        BusFactory.setDefaultBus(bus);
        
        BindingFactoryManager bfm = bus.getExtension(BindingFactoryManager.class);        
        CorbaBindingFactory factory = 
            (CorbaBindingFactory)bfm.getBindingFactory("http://cxf.apache.org/bindings/corba");
        OrbConfig orbConfig = (OrbConfig)factory.getOrbConfig();
        assertTrue("CorbaBindingFactoryConfigurer is null", orbConfig != null);
        Properties props  = orbConfig.getOrbProperties();
        assertTrue("probs is null", props != null);
        assertTrue("prob1 is not equal to value1", 
                "value1".equals(props.get("prop1")));
        assertTrue("prob2 is not equal to value2", 
                "value2".equals(props.get("prop2")));
        assertTrue("ORBClass is not equal to MyORBImpl", 
                "com.orbimplco.MyORBImpl".equals(props.get("org.omg.CORBA.ORBClass")));
        assertTrue("ORBSingletonClass is not equal to MyORBSingleton", 
                "com.orbimplco.MyORBSingleton".equals(props.get("org.omg.CORBA.ORBSingletonClass")));
        List <String> orbArgs = orbConfig.getOrbArgs();
        assertTrue("orbArgs is null", orbArgs != null);
        String domainNameId = orbArgs.get(0);
        assertTrue("domainNameId is not equal to -ORBdomain_name", 
                "-ORBdomain_name".equals(domainNameId));
        String domainNameValue = orbArgs.get(1);
        assertTrue("domainNameValue is not equal to test-domain", 
                "test-domain".equals(domainNameValue));
        String configDomainsDirId = orbArgs.get(2);
        assertTrue("configDomainsDirId is not equal to -ORBconfig_domains_dir", 
                "-ORBconfig_domains_dir".equals(configDomainsDirId));
        String configDomainsDirValue = orbArgs.get(3);
        assertTrue("configDomainsDirValue is not equal to src/test/resources", 
                "src/test/resources".equals(configDomainsDirValue));
        String orbNameId = orbArgs.get(4);
        assertTrue("orbNameId is not equal to -ORBname", 
                "-ORBname".equals(orbNameId));
        String orbNameValue = orbArgs.get(5);
        assertTrue("orbNameValue is not equal to test", 
                "test".equals(orbNameValue));
    }
}

