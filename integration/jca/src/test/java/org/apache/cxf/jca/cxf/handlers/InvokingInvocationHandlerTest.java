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

import java.lang.reflect.Proxy;


import org.apache.cxf.jca.cxf.CXFInvocationHandler;
import org.apache.cxf.jca.cxf.CXFInvocationHandlerData;
import org.junit.Before;
import org.junit.Test;

public class InvokingInvocationHandlerTest 
    extends AbstractInvocationHandlerTest {

    TestInterface test;
    TestInterface test2;
    TestTarget target;
    CXFInvocationHandler handler;
    CXFInvocationHandlerData data;

    public InvokingInvocationHandlerTest() {
        super();
    }
    @Before
    public void setUp() {
        super.setUp();
        target = new TestTarget();
        data = new CXFInvocationHandlerDataImpl();
        data.setTarget(target);
        handler = new InvokingInvocationHandler(data);
        Class[] interfaces = {TestInterface.class};

        test = (TestInterface)Proxy.newProxyInstance(TestInterface.class.getClassLoader(), interfaces,
                                                     handler);
        handler.getData().setTarget(target);

        CXFInvocationHandlerData data2 = new CXFInvocationHandlerDataImpl();
        CXFInvocationHandler handler2 = new InvokingInvocationHandler(data2);
        test2 = (TestInterface)Proxy.newProxyInstance(TestInterface.class.getClassLoader(), interfaces,
                                                      handler2);
        handler2.getData().setTarget(target);
    }

    /**
     * override this test - this handler is alway the last in the chain and is
     * responsible for delegating the invocation to the target object
     */
    @Test
    public void testHandlerInvokesNext() throws Throwable {
        assertTrue("target method  must not have be called", !target.methodInvoked);
        handler.invoke(target, testMethod, new Object[0]);
        assertTrue("target method must be called", target.methodInvoked);
    }

    @Test
    public void testInvocationThroughProxy() throws IllegalArgumentException {

        assertTrue("target object must no have been invoked", !target.methodInvoked);
        test.testMethod();
        assertTrue("target object must be invoked", target.methodInvoked);
    }

    protected CXFInvocationHandler getHandler() {

        return handler;
    }

    
}
