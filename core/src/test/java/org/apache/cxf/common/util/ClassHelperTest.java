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
package org.apache.cxf.common.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.framework.ProxyFactory;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;



public class ClassHelperTest extends Assert {

    private Object proxiedObject;

    private Object springAopObject;

    private InvocationHandler realObjectInternalProxy;

    private Object realObjectInternalSpring;

    private Bus bus;

    private Bus currentThreadBus;

    @Before
    public void setUp() {
        realObjectInternalProxy = new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                return null;
            }
        };

        proxiedObject = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{AnyInterface.class}, realObjectInternalProxy);

        realObjectInternalSpring = new Object();

        ProxyFactory proxyFactory = new ProxyFactory(realObjectInternalSpring);
        proxyFactory.addAdvice(new AfterReturningAdvice() {

            @Override
            public void afterReturning(Object o, Method method, Object[] objects, Object o1) throws Throwable {

            }
        });

        springAopObject = proxyFactory.getProxy();

        currentThreadBus = BusFactory.getThreadDefaultBus();

        bus = EasyMock.mock(Bus.class);

        BusFactory.setThreadDefaultBus(bus);
    }

    @After
    public void tearDown() {
        BusFactory.setThreadDefaultBus(currentThreadBus);
    }

    @Test
    public void getRealClassPropertyWasSetInBus() {

        EasyMock.expect(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).andReturn(true);
        EasyMock.expect(bus.getProperty(ClassUnwrapper.class.getName())).andReturn(null);
        EasyMock.replay(bus);

        assertSame(realObjectInternalProxy.getClass(), ClassHelper.getRealClass(proxiedObject));

        EasyMock.verify(bus);

    }

    @Test
    public void getRealClassPropertyWasNotSetInBus() {

        EasyMock.expect(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).andReturn(false);
        EasyMock.expect(bus.getProperty(ClassUnwrapper.class.getName())).andReturn(null);
        EasyMock.replay(bus);

        assertSame(realObjectInternalSpring.getClass(), ClassHelper.getRealClass(springAopObject));

        EasyMock.verify(bus);

    }

    @Test
    public void getRealClassFromClassPropertyWasSetInBus() {

        EasyMock.expect(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).andReturn(true);
        EasyMock.replay(bus);

        assertSame(proxiedObject.getClass(), ClassHelper.getRealClassFromClass(proxiedObject.getClass()));

        EasyMock.verify(bus);

    }

    @Test
    public void getRealClassFromClassPropertyWasNotSetInBus() {

        EasyMock.expect(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).andReturn(false);
        EasyMock.replay(bus);

        assertSame(realObjectInternalSpring.getClass(), ClassHelper.getRealClassFromClass(springAopObject.getClass()));

        EasyMock.verify(bus);

    }


    @Test
    public void getRealObjectPropertyWasSetInBus() {

        EasyMock.expect(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).andReturn(true);
        EasyMock.replay(bus);

        assertSame(realObjectInternalProxy, ClassHelper.getRealObject(proxiedObject));

        EasyMock.verify(bus);

    }

    @Test
    public void getRealObjectPropertyWasNotSetInBus() {

        EasyMock.expect(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).andReturn(false);
        EasyMock.replay(bus);

        assertSame(realObjectInternalSpring, ClassHelper.getRealObject(springAopObject));

        EasyMock.verify(bus);

    }

    public interface AnyInterface {
        void anyMethod();
    }


}