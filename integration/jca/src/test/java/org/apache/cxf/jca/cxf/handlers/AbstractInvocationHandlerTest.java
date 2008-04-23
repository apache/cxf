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


import org.apache.cxf.jca.cxf.CXFInvocationHandler;
import org.apache.cxf.jca.cxf.CXFManagedConnectionFactory;
import org.junit.Test;

public abstract class AbstractInvocationHandlerTest 
    extends HandlerTestBase {
    
    public AbstractInvocationHandlerTest() {
     
    }
    public AbstractInvocationHandlerTest(String name) {
        super(name);
    }

    // seach for the setNext method
    @Test
    public void testHandlerInvokesNext() throws Throwable {
        Object[] args = new Object[0];
                
        CXFInvocationHandler handler = getHandler();
        handler.setNext(mockHandler); 
        
        handler.invoke(target, testMethod, args);        
             
        assertTrue("target object must not be called", !target.methodInvoked);
    }

    @Test
    public void testTargetAttribute() {

        CXFInvocationHandler handler = getHandler();
        handler.getData().setTarget(target);
        assertSame("target must be retrievable after set",
                   target, handler.getData().getTarget());
    }

    @Test
    public void testBusAttribute() {

        CXFInvocationHandler handler = getHandler();
        handler.getData().setBus(mockBus);
        assertSame("bus must be retrievable after set", mockBus, handler.getData().getBus());
    }

    @Test
    public void testManagedConnectionAttribute() {

        CXFInvocationHandler handler = getHandler();

        handler.getData().setManagedConnection(mockManagedConnection);
        assertSame("bus must be retrievable after set", mockManagedConnection, handler.getData()
            .getManagedConnection());
    }

    protected CXFInvocationHandler getNextHandler() {
        return (CXFInvocationHandler)mockHandler;
    }

    protected abstract CXFInvocationHandler getHandler();

    protected CXFManagedConnectionFactory getTestManagedConnectionFactory() {
        return (CXFManagedConnectionFactory)mcf;
    }

    
}
