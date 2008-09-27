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
package org.apache.cxf.jca.cxf;


import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import javax.resource.ResourceException;

import org.apache.cxf.Bus;
import org.apache.cxf.jca.core.resourceadapter.ResourceAdapterInternalException;
import org.apache.cxf.test.AbstractCXFTest;
import org.easymock.classextension.EasyMock;
import org.junit.Ignore;
import org.junit.Test;

public class JCABusFactoryTest extends AbstractCXFTest {
   
    
    @Test
    public void testSetAppserverClassLoader() {
        ClassLoader loader = new DummyClassLoader();
        JCABusFactory bf = new JCABusFactory(new ManagedConnectionFactoryImpl());
        bf.setAppserverClassLoader(loader);
        assertSame("Checking appserverClassLoader.", loader, bf.getAppserverClassLoader());
    } 

    @Test
    public void testLoadNonexistentProperties() throws Exception {
        ManagedConnectionFactoryImpl mcf = new ManagedConnectionFactoryImpl();
        JCABusFactory jcaBusFactory = new JCABusFactory(mcf);
        try {
            jcaBusFactory.loadProperties(new File("/rubbish_name.properties").toURI().toURL());
            fail("expect an exception .");
        } catch (ResourceException re) {
            assertTrue("Cause is FileNotFoundException, cause: " + re.getCause(),
                       re.getCause() instanceof FileNotFoundException);
        }
    }
    
    @Test
    public void testInvalidMonitorConfigNoPropsURL() throws Exception {
        ManagedConnectionFactoryImpl mcf = new ManagedConnectionFactoryImpl();
        mcf.setMonitorEJBServiceProperties(Boolean.TRUE);
        JCABusFactory jcaBusFactory = new JCABusFactory(mcf);
        try {
            Bus mockBus = EasyMock.createMock(Bus.class);
            jcaBusFactory.setBus(mockBus);
            jcaBusFactory.initializeServants();
            fail("exception expected");
        } catch (ResourceAdapterInternalException re) {
            assertTrue("EJBServiceProperties is not set.", re.getMessage()
                .indexOf("EJBServicePropertiesURL is not set") != -1);
        }
    }
    
    @Ignore
    @Test
    public void testInitServants() throws Exception {
        ManagedConnectionFactoryImpl mcf = new ManagedConnectionFactoryImpl();
        //get resource 
        URL propFile = getClass().getResource("resources/ejb_servants.properties");
        mcf.setEJBServicePropertiesURL(propFile.toString());
        JCABusFactory jcaBusFactory = new JCABusFactory(mcf);
        Bus mockBus = EasyMock.createMock(Bus.class);

        jcaBusFactory.setBus((Bus)mockBus);
        jcaBusFactory.initializeServants();
        
    }
    
   
}


class DummyClassLoader extends ClassLoader {
    public DummyClassLoader() {
        super();
    }
}
