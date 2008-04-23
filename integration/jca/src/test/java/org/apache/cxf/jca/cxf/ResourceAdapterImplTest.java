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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Properties;

import javax.resource.ResourceException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;


import org.apache.cxf.Bus;
import org.apache.cxf.jca.core.resourceadapter.ResourceBean;
import org.easymock.classextension.EasyMock;
import org.junit.Assert;
import org.junit.Test;


public class ResourceAdapterImplTest extends Assert {

    public ResourceAdapterImplTest() {
        
    }

    @Test
    public void testConstructorWithoutProperties() throws Exception {
        ResourceAdapterImpl rai = new ResourceAdapterImpl();
        assertTrue("constructed without props", rai instanceof ResourceAdapterImpl);
        assertTrue("constructed without props", rai instanceof ResourceBean);
        assertTrue("constructed without props", rai instanceof ResourceAdapter);
        assertTrue("constructed without props", rai instanceof Serializable);
    }

    @Test
    public void testConstructorWithProperties() throws Exception {
        Properties props = new Properties();
        ResourceAdapterImpl rai = new ResourceAdapterImpl(props);
        assertTrue("constructed with props", rai instanceof ResourceAdapterImpl);
        assertTrue("constructed with props", rai instanceof ResourceBean);
        assertTrue("constructed with props", rai instanceof ResourceAdapter);
        assertTrue("constructed with props", rai instanceof Serializable);
    }

    @Test
    public void testSerializability() throws Exception {
        final String key = "key";
        final String value = "value";
        Properties props = new Properties();
        props.setProperty(key, value);
        ResourceAdapterImpl rai = new ResourceAdapterImpl(props);

        assertTrue("before serialized, key is set", rai.getPluginProps().containsKey(key));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(rai);
        byte[] buf = baos.toByteArray();
        oos.close();
        baos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        ObjectInputStream ois = new ObjectInputStream(bais);
        ResourceAdapterImpl rai2 = (ResourceAdapterImpl)ois.readObject();
        ois.close();
        bais.close();

        assertNotNull("deserialized is not null", rai2);
        assertTrue("props not empty", !rai2.getPluginProps().isEmpty());
        assertTrue("props contains key", rai2.getPluginProps().containsKey(key));
        assertEquals("no change after serialized and reconstitued ", value, rai2.getPluginProps()
            .getProperty(key));
    }

    @Test
    public void testRegisterBusOfNull() throws Exception {
        ResourceAdapterImpl rai = new ResourceAdapterImpl();
        rai.registerBus(null);
        assertNotNull("bus cache is not null", rai.getBusCache());
        assertTrue("bus null registered", rai.getBusCache().contains(null));
    }

    @Test
    public void testRegisterBusNotNull() throws Exception {
        ResourceAdapterImpl rai = new ResourceAdapterImpl();
        Bus bus = EasyMock.createMock(Bus.class);
        rai.registerBus(bus);
        assertNotNull("bus cache is not null", rai.getBusCache());
        assertTrue("bus registered", rai.getBusCache().contains(bus));
    }

    @Test
    public void testStartWithNullBootstrapContextThrowException() throws Exception {
        ResourceAdapterImpl rai = new ResourceAdapterImpl();
        try {
            rai.start(null);
            fail("Exception expected");
        } catch (ResourceException re) {
            assertTrue("error message contains BootstrapContext",
                       re.getMessage().indexOf("BootstrapContext") != -1);
            assertNull("BootstrapContext is null", rai.getBootstrapContext());
        }
    }

    @Test
    public void testGetCorrectBootstrapContext() throws Exception {
        ResourceAdapterImpl rai = new ResourceAdapterImpl();
        BootstrapContext bc = EasyMock.createMock(BootstrapContext.class);
        assertNotNull("BootstrapContext not null", bc);
        rai.start(bc);
        assertEquals("BootstrapContext set", rai.getBootstrapContext(), bc);
    }

    @Test
    public void testStopWithEmptyBusCache() throws Exception {
        ResourceAdapterImpl rai = new ResourceAdapterImpl();
        rai.setBusCache(new HashSet<Bus>());
        try {
            assertNotNull("bus cache is not null", rai.getBusCache());
            assertTrue("bus cache is empty", rai.getBusCache().isEmpty());
            rai.stop();
        } catch (Exception e) {
            fail("no exception expected");
        }
    }

    @Test
    public void testStopWithNonEmptyBusCache() throws Exception {
        ResourceAdapterImpl rai = new ResourceAdapterImpl();
        rai.setBusCache(new HashSet<Bus>());
        Bus bus = EasyMock.createMock(Bus.class);
        bus.shutdown(true);
        EasyMock.expectLastCall();
        EasyMock.replay(bus);        
        rai.registerBus(bus);
        rai.stop();
        EasyMock.verify(bus);
    }

    
}
