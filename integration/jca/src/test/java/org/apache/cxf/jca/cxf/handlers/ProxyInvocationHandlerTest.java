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
package org.apache.cxf.jca.cxf.handlers;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jca.cxf.CXFInvocationHandler;
import org.apache.cxf.jca.cxf.CXFInvocationHandlerData;
import org.apache.cxf.jca.cxf.CXFManagedConnection;
import org.junit.Before;
import org.junit.Test;


public class  ProxyInvocationHandlerTest extends AbstractInvocationHandlerTest {

    ProxyInvocationHandler testObject;
    CXFInvocationHandlerData data;

    public ProxyInvocationHandlerTest() {
        super();
    }
    
    public ProxyInvocationHandlerTest(String name) {
        super(name);
    }

    @Before
    public void setUp() { 
        super.setUp(); 
        data = new CXFInvocationHandlerDataImpl();
        testObject = new ProxyInvocationHandler(data);
        testObject.getData().setManagedConnection((CXFManagedConnection)mci);
        assertTrue(testObject instanceof CXFInvocationHandlerBase); 
    } 


    public CXFInvocationHandler getHandler() { 
        return testObject;
    }

   
    @Test
    public void testInvokeSetsBusCurrent() throws Throwable {
        Bus oldBus = BusFactory.getDefaultBus();
        
        testObject.invoke(target, testMethod, new Object[] {});

        Bus  newBus = BusFactory.getDefaultBus();
       
        assertSame("Current Bus has been set and is as expected, val=" + newBus, newBus, mockBus);
         // set back the JVM current local variable        
        BusFactory.setDefaultBus(oldBus);
    }

}





