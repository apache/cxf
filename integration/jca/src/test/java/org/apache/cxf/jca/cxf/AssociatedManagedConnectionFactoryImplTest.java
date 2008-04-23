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

//import java.lang.reflect.Method;
import java.util.Properties;

import javax.resource.ResourceException;

//import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ResourceAdapter;


import org.apache.cxf.Bus;
//import org.apache.cxf.jca.cxf.test.DummyBus;
import org.easymock.classextension.EasyMock;
import org.junit.Test;

public class AssociatedManagedConnectionFactoryImplTest extends ManagedConnectionFactoryImplTest {
    
    
    @Test
    public void testSetResourceAdapter() throws Exception {
        TestableAssociatedManagedConnectionFactoryImpl mci = 
            new TestableAssociatedManagedConnectionFactoryImpl();
        ResourceAdapterImpl rai = new ResourceAdapterImpl();
        mci.setResourceAdapter(rai);
        assertEquals("ResourceAdapter is set", mci.getResourceAdapter(), rai);
    }

    @Test
    public void testSetWrongResourceAdapterThrowException() throws Exception {
        TestableAssociatedManagedConnectionFactoryImpl mci =
            new TestableAssociatedManagedConnectionFactoryImpl();
        ResourceAdapter rai = EasyMock.createMock(ResourceAdapter.class);
        try {
            mci.setResourceAdapter(rai);
            fail("exception expected");
        } catch (ResourceException re) {
            assertTrue("wrong ResourceAdapter set", re.getMessage().indexOf("ResourceAdapterImpl") != -1);
        }
    }

    @Test
    public void testRegisterBusThrowExceptionIfResourceAdapterNotSet() throws Exception {
        TestableAssociatedManagedConnectionFactoryImpl mci =
            new TestableAssociatedManagedConnectionFactoryImpl();
        try {
            mci.registerBus();
            fail("exception expected");
        } catch (ResourceException re) {
            assertTrue("ResourceAdapter not set", re.getMessage().indexOf("null") != -1);
        }
    }
    /*
    public void testBusInitializedAndRegisteredToResourceAdapter() throws ResourceException, Exception {
        DummyBus.reset();     
        System.setProperty("test.bus.class", DummyBus.class.getName());
        TestableAssociatedManagedConnectionFactoryImpl mci =
            new TestableAssociatedManagedConnectionFactoryImpl();
        DummyResourceAdapterImpl rai = new DummyResourceAdapterImpl();
        mci.setResourceAdapter(rai);
        ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        try {
            // do this for MockObject creation
            Thread.currentThread().setContextClassLoader(mci.getClass().getClassLoader());

            Class dummyBusClass = Class.forName(DummyBus.class.getName(), true, mci.getClass()
                .getClassLoader());           
            Method initializeCount = dummyBusClass.getMethod("getInitializeCount", new Class[]{});
            ConnectionManager cm = 
                (ConnectionManager)EasyMock.createMock(
                    Class.forName(ConnectionManager.class.getName(), true, mci.getClass().getClassLoader()));

            mci.createConnectionFactory(cm);
            assertEquals("bus should be initialized once", 1, 
                         initializeCount.invoke(null, new Object[]{}));
            assertEquals("bus registered once after first call", 1, rai.registeredCount);
        } finally {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }
    */
    @Test
    public void testMergeNonDuplicateResourceAdapterProps() throws ResourceException {
        Properties props = new Properties();
        props.setProperty("key1", "value1");
        ResourceAdapterImpl rai = new ResourceAdapterImpl(props);

        TestableAssociatedManagedConnectionFactoryImpl mci =
            new TestableAssociatedManagedConnectionFactoryImpl();
       

        assertEquals("before associate, one props", 0, mci.getPluginProps().size());
        assertTrue("before associate, key1 not set", !mci.getPluginProps().containsKey("key1"));

        mci.setResourceAdapter(rai);
        assertEquals("after associate, two props", 1, mci.getPluginProps().size());
        assertTrue("after associate, key1 is set", mci.getPluginProps().containsKey("key1"));
    }

    
    protected ManagedConnectionFactoryImpl createManagedConnectionFactoryImpl() {
        TestableAssociatedManagedConnectionFactoryImpl mci =
            new TestableAssociatedManagedConnectionFactoryImpl();
        try {
            mci.setResourceAdapter(new DummyResourceAdapterImpl());
        } catch (Exception e) {
            System.out.println("failed to setResourceAdapter" + e);
        }
        return mci;
    }


}

class DummyResourceAdapterImpl extends ResourceAdapterImpl {
    int registeredCount;

    DummyResourceAdapterImpl() {
        super();
    }

    public void registerBus(Bus bus) {
        registeredCount++;
    }
}

class TestableAssociatedManagedConnectionFactoryImpl extends AssociatedManagedConnectionFactoryImpl {
   
}
