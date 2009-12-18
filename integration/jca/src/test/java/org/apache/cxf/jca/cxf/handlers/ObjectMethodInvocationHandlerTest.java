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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.cxf.jca.cxf.CXFInvocationHandler;
import org.apache.cxf.jca.cxf.CXFInvocationHandlerData;
import org.junit.Before;
import org.junit.Test;



public class ObjectMethodInvocationHandlerTest extends AbstractInvocationHandlerTest {

    ObjectMethodInvocationHandler handler; 
    CXFInvocationHandlerData data;

    TestTarget testTarget = new TestTarget(); 
    DummyHandler dummyHandler = new DummyHandler();
    
    public ObjectMethodInvocationHandlerTest() {
        super();
    }

    @Before
    public void setUp() { 
        super.setUp(); 
        target.lastMethod = null; 
        dummyHandler.invokeCalled = false;         
        data = new CXFInvocationHandlerDataImpl();
        data.setTarget(target);
        handler = new ObjectMethodInvocationHandler(data);
        handler.setNext((CXFInvocationHandler)dummyHandler); 
    } 

    @Test
    public void testToString() throws Throwable  { 

        Method toString = Object.class.getMethod("toString", new Class[0]);
        
        Object result = handler.invoke(testTarget, toString, null); 
        assertTrue("object method must not be passed to next handler in chain", 
                   !dummyHandler.invokeCalled); 
        assertTrue("object must be a String", result instanceof String);
        assertTrue("checking toString method ", ((String)result).startsWith("ConnectionHandle"));
    } 

    @Test
    public void testHashCode() throws Throwable { 

        Method hashCode = Object.class.getMethod("hashCode", new Class[0]); 
        doObjectMethodTest(hashCode); 
    } 

    @Test
    public void testEqualsDoesNotCallNext() throws Throwable { 

        Method equals = Object.class.getMethod("equals", new Class[] {Object.class}); 
        handler.invoke(testTarget, equals, new Object[] {this}); 
        assertTrue("object method must not be passed to next handler in chain", 
                   !dummyHandler.invokeCalled); 
    } 
    
    @Test
    public void testNonObjecMethod() throws Throwable { 

        DummyHandler dummyHandler1 = new DummyHandler(); 
        handler.setNext((CXFInvocationHandler)dummyHandler1); 

        final Method method = TestTarget.class.getMethod("testMethod", new Class[0]); 
        
        handler.invoke(testTarget, method, new Object[0]); 

        assertTrue("non object method must be passed to next handler in chain", dummyHandler1.invokeCalled); 
    }

    @Test
    public void testEqualsThroughProxies() { 

        Class[] interfaces = {TestInterface.class};
        CXFInvocationHandlerData data1 = new CXFInvocationHandlerDataImpl();
        CXFInvocationHandlerData data2 = new CXFInvocationHandlerDataImpl();
        data1.setTarget(new TestTarget());
        data2.setTarget(new TestTarget());
        ObjectMethodInvocationHandler handler1 = new ObjectMethodInvocationHandler(data1); 
        handler1.setNext((CXFInvocationHandler)mockHandler); 
        ObjectMethodInvocationHandler handler2 = new ObjectMethodInvocationHandler(data2); 
        handler2.setNext((CXFInvocationHandler)mockHandler); 

        TestInterface proxy1 = 
            (TestInterface)Proxy.newProxyInstance(TestInterface.class.getClassLoader(), interfaces, handler1);
        TestInterface proxy2 = 
            (TestInterface)Proxy.newProxyInstance(TestInterface.class.getClassLoader(), interfaces, handler2);

        assertEquals(proxy1, proxy1); 
        assertTrue(!proxy1.equals(proxy2)); 
    } 


    protected void doObjectMethodTest(Method method) throws Throwable { 
        doObjectMethodTest(method, null); 
    } 

    protected void doObjectMethodTest(Method method, Object[] args) throws Throwable { 

        handler.invoke(testTarget, method, args); 

        assertTrue("object method must not be passed to next handler in chain",
                   !dummyHandler.invokeCalled); 
        assertEquals(method + " must be invoked directly on target object",
                     method.getName(), target.lastMethod.getName()); 
    }    

    public CXFInvocationHandler getHandler() { 
        return handler;
    } 

      
}

