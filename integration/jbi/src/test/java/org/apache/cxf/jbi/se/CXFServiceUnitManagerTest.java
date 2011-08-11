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
package org.apache.cxf.jbi.se;

import java.net.URL;
import java.util.logging.Logger;

import javax.jbi.component.ComponentContext;
import javax.jbi.management.DeploymentException;


import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CXFServiceUnitManagerTest extends Assert {
    
    private static final Logger LOG = LogUtils.getL7dLogger(CXFServiceUnitManagerTest.class);

    private CXFServiceUnitManager csuManager;
    private ComponentContext ctx = EasyMock.createMock(ComponentContext.class);
    private Bus bus;    
    
    @Before
    public void setUp() throws Exception {
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        
        bus = new SpringBusFactory().createBus();
        ComponentClassLoader componentClassLoader = 
            new ComponentClassLoader(new URL[0], getClass().getClassLoader());
        csuManager = new CXFServiceUnitManager(bus, ctx, componentClassLoader);
    }
    
    @After
    public void tearDown() throws Exception {
        bus.shutdown(false);
    }
    
    @Test
    public void testShutdownWithNull() throws Exception {
        
        try {
            csuManager.shutDown(null);
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.NAME.NULL", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    @Test
    public void testShutdownWithEmpty() throws Exception {
        
        try {
            csuManager.shutDown("");
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.NAME.EMPTY", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    @Test
    public void testShutdownWithUndeployedSU() {
        try {
            csuManager.shutDown("dummySU");
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("UNDEPLOYED.SU", LOG).toString() + "dummySU");
            return;
        }
        fail("should get DeploymentException");
    }
    

    @Test
    public void testDeployWithNullSUName() throws Exception {
        
        try {
            csuManager.deploy(null, "dummyRoot");
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.NAME.NULL", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    @Test
    public void testDeployWithEmptySUName() throws Exception {
        
        try {
            csuManager.deploy("", "dummyRoot");
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.NAME.EMPTY", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    @Test
    public void testDeployWithDuplicatedSU() {
        try {
            csuManager.deploy("dummySU", "dummyRoot");
            csuManager.init("dummySU", "dummyRoot");
            csuManager.deploy("dummySU", "dummyRoot");
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("DUPLICATED.SU", LOG).toString() + "dummySU");
            return;
        }
        fail("should get DeploymentException");
    }

    @Test
    public void testDeployWithNullSURoot() throws Exception {
        
        try {
            csuManager.deploy("dummySU", null);
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.ROOT.NULL", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    @Test
    public void testDeployWithEmptySURoot() throws Exception {
        
        try {
            csuManager.deploy("dummySU", "");
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.ROOT.EMPTY", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    @Test
    public void testUndeployWithNullSUName() throws Exception {
        
        try {
            csuManager.undeploy(null, "dummyRoot");
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.NAME.NULL", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    @Test
    public void testUndeployWithEmptySUName() throws Exception {
        
        try {
            csuManager.undeploy("", "dummyRoot");
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.NAME.EMPTY", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    
    @Test
    public void testUndeployWithNullSURoot() throws Exception {
        
        try {
            csuManager.undeploy("dummySU", null);
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.ROOT.NULL", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    @Test
    public void testUndeployWithEmptySURoot() throws Exception {
        
        try {
            csuManager.undeploy("dummySU", "");
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.ROOT.EMPTY", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    @Test
    public void testInitWithNullSUName() throws Exception {
        
        try {
            csuManager.init(null, "dummyRoot");
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.NAME.NULL", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    @Test
    public void testInitWithEmptySUName() throws Exception {
        
        try {
            csuManager.init("", "dummyRoot");
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.NAME.EMPTY", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    
    @Test
    public void testInitWithNullSURoot() throws Exception {
        
        try {
            csuManager.init("dummySU", null);
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.ROOT.NULL", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    @Test
    public void testInitWithEmptySURoot() throws Exception {
        
        try {
            csuManager.init("dummySU", "");
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.ROOT.EMPTY", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    @Test
    public void testStartWithNullSUName() throws Exception {
        
        try {
            csuManager.start(null);
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.NAME.NULL", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    @Test
    public void testStartWithEmptySUName() throws Exception {
        
        try {
            csuManager.start("");
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.NAME.EMPTY", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    @Test
    public void testStopWithNullSUName() throws Exception {
        
        try {
            csuManager.stop(null);
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.NAME.NULL", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    @Test
    public void testStopWithEmptySUName() throws Exception {
        
        try {
            csuManager.stop("");
        } catch (DeploymentException e) {
            assertEquals(e.getMessage(), new Message("SU.NAME.EMPTY", LOG).toString());
            return;
        }
        fail("should get DeploymentException");        
    }
    
    
}
