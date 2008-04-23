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

package org.apache.cxf.management.jmx.export;


import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;


import org.apache.cxf.management.jmx.export.runtime.ModelMBeanAssembler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;



public class ModelMBeanAssemblerTest extends  Assert {
    
    protected static final String AGE_ATTRIBUTE = "Age";

    protected static final String NAME_ATTRIBUTE = "Name";
    
    protected static AnnotationTestInstrumentation ati = new AnnotationTestInstrumentation();
   
    protected MBeanServer server;
    
    protected ObjectName ton;

    @Before
    public final void setUp() throws Exception {
        this.server = MBeanServerFactory.createMBeanServer();
        try {
            onSetUp();
        } catch (Exception e) {
            releaseServer();
            throw e;
        }
    }
    
    @After
    public void tearDown() throws Exception {
        releaseServer();
        onTearDown();
    }

    private void releaseServer() {
        MBeanServerFactory.releaseMBeanServer(this.getServer());
    }

    protected void onTearDown() throws Exception {
        // unregister the mbean client
        server.unregisterMBean(ton);
    }

    protected void onSetUp() throws Exception {
        
        try {
            ton = new ObjectName("org.apache.cxf:Type=testInstrumentation");
        } catch (MalformedObjectNameException e) {            
            e.printStackTrace();
        } catch (NullPointerException e) {            
            e.printStackTrace();
        }
       
        
        //create the mbean and register it
        ModelMBeanInfo mbi = getMBeanInfoFromAssembler();
        
        RequiredModelMBean rtMBean;
        
        rtMBean = (RequiredModelMBean)server.instantiate(
            "javax.management.modelmbean.RequiredModelMBean");
                       
        rtMBean.setModelMBeanInfo(mbi);
    
        rtMBean.setManagedResource(ati, "ObjectReference");
                           
        server.registerMBean(rtMBean, ton);
    }

    public MBeanServer getServer() {
        return server;
    }
    
    //the client get and set the ModelObject and setup the ManagerBean
    @Test
    public void testRegisterOperations() throws Exception {
        ModelMBeanInfo info = getMBeanInfoFromAssembler();
        assertEquals("Incorrect number of operations registered",
                      10, info.getOperations().length);
    }
    
    @Test
    public void testGetMBeanInfo() throws Exception {
        ModelMBeanInfo info = getMBeanInfoFromAssembler();
        assertNotNull("MBeanInfo should not be null", info);
    }

    @Test
    public void testGetMBeanAttributeInfo() throws Exception {
        ModelMBeanInfo info = getMBeanInfoFromAssembler();
        MBeanAttributeInfo[] inf = info.getAttributes();
        assertEquals("Invalid number of Attributes returned",
                       4, inf.length);

        for (int x = 0; x < inf.length; x++) {
            assertNotNull("MBeanAttributeInfo should not be null", inf[x]);
            assertNotNull("Description for MBeanAttributeInfo should not be null",
                            inf[x].getDescription());
        }
    }

    @Test
    public void testGetMBeanOperationInfo() throws Exception {
        ModelMBeanInfo info = getMBeanInfoFromAssembler();
        MBeanOperationInfo[] inf = info.getOperations();
        assertEquals("Invalid number of Operations returned",
                               10, inf.length);

        for (int x = 0; x < inf.length; x++) {
            assertNotNull("MBeanOperationInfo should not be null", inf[x]);
            assertNotNull("Description for MBeanOperationInfo should not be null",
                           inf[x].getDescription());
        }
    }

    @Test
    public void testDescriptionNotNull() throws Exception {
        ModelMBeanInfo info = getMBeanInfoFromAssembler();
        assertNotNull("The MBean description should not be null",
                                info.getDescription());
    }

    @Test
    public void testSetAttribute() throws Exception {        
        getServer().setAttribute(ton, new Attribute(AGE_ATTRIBUTE, 12));
        assertEquals("The Age should be ", 12, ati.getAge());
        getServer().setAttribute(ton, new Attribute(NAME_ATTRIBUTE, "Rob Harrop"));                
        assertEquals("The name should be ", "Rob Harrop", ati.getName());
    }

    @Test
    public void testGetAttribute() throws Exception {        
        ati.setName("John Smith");
        Object val = getServer().getAttribute(ton, NAME_ATTRIBUTE);        
        assertEquals("Incorrect result", "John Smith", val);
    } 

    @Test
    public void testOperationInvocation() throws Exception {       
        Object result = getServer().invoke(ton, "add",
                                new Object[] {new Integer(20), new Integer(30)}, new String[] {"int", "int"});
        assertEquals("Incorrect result", new Integer(50), result);
    }
    
    @Test
    public void testAttributeHasCorrespondingOperations() throws Exception {
        ModelMBeanInfo info = getMBeanInfoFromAssembler();

        ModelMBeanOperationInfo myOperation = info.getOperation("myOperation");
        assertNotNull("get operation should not be null", myOperation);
        assertEquals("Incorrect myOperation return type", "long", myOperation.getReturnType());
                
        ModelMBeanOperationInfo add = info.getOperation("add");                
        assertNotNull("set operation should not be null", add);
        assertEquals("Incorrect add method description", "Add Two Numbers Together", add.getDescription());
                
    }

    @Test
    public void testNotificationMetadata() throws Exception {
        ModelMBeanInfo info = getMBeanInfoFromAssembler();
        MBeanNotificationInfo[] notifications = info.getNotifications();
        assertEquals("Incorrect number of notifications", 1, notifications.length);
        assertEquals("Incorrect notification name", "My Notification", notifications[0].getName());

        String[] notifTypes = notifications[0].getNotifTypes();

        assertEquals("Incorrect number of notification types", 2, notifTypes.length);
        assertEquals("Notification type.foo not found", "type.foo", notifTypes[0]);
        assertEquals("Notification type.bar not found", "type.bar", notifTypes[1]);
    }

    
    protected ModelMBeanInfo getMBeanInfoFromAssembler() {
        ModelMBeanAssembler assembler = new ModelMBeanAssembler();
        return assembler.getModelMbeanInfo(AnnotationTestInstrumentation.class);        
    }
     
        
  

}
