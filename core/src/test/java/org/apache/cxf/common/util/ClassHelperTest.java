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
import java.util.function.Function;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClassHelperTest {

    private Object proxiedObject;

    private Object springAopObject;
    
    private Object cglibProxyObject;

    private InvocationHandler realObjectInternalProxy;

    private Object realObjectInternalSpring;

    private Bus bus;

    private Bus currentThreadBus;
    
    private Function<String, Integer> fn;

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
        
        final Callback callback = new org.springframework.cglib.proxy.InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return null;
            }
        };
        cglibProxyObject = Enhancer.create(Object.class, new Class[] {Function.class}, callback);
        
        fn = Integer::parseInt;

        currentThreadBus = BusFactory.getThreadDefaultBus();

        bus = mock(Bus.class);

        BusFactory.setThreadDefaultBus(bus);
    }

    @After
    public void tearDown() {
        BusFactory.setThreadDefaultBus(currentThreadBus);
    }

    @Test
    public void getRealClassPropertyWasSetInBus() {

        when(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).thenReturn(true);
        when(bus.getProperty(ClassUnwrapper.class.getName())).thenReturn(null);

        assertSame(realObjectInternalProxy.getClass(), ClassHelper.getRealClass(proxiedObject));

        verify(bus, times(1)).getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER);
        verify(bus, times(1)).getProperty(ClassUnwrapper.class.getName());

    }

    @Test
    public void getRealClassPropertyWasNotSetInBus() {

        when(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).thenReturn(false);
        when(bus.getProperty(ClassUnwrapper.class.getName())).thenReturn(null);

        assertSame(realObjectInternalSpring.getClass(), ClassHelper.getRealClass(springAopObject));

        verify(bus, times(1)).getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER);
        verify(bus, times(1)).getProperty(ClassUnwrapper.class.getName());

    }

    @Test
    public void getRealClassFromClassPropertyWasSetInBus() {

        when(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).thenReturn(true);
        when(bus.getProperty(ClassUnwrapper.class.getName())).thenReturn(null);

        assertSame(proxiedObject.getClass(), ClassHelper.getRealClassFromClass(proxiedObject.getClass()));

        verify(bus, times(1)).getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER);
        verify(bus, times(1)).getProperty(ClassUnwrapper.class.getName());
    }

    @Test
    public void getRealClassFromClassPropertyWasNotSetInBus() {

        when(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).thenReturn(false);
        when(bus.getProperty(ClassUnwrapper.class.getName())).thenReturn(null);

        assertSame(realObjectInternalSpring.getClass(), ClassHelper.getRealClassFromClass(springAopObject.getClass()));

        verify(bus, times(1)).getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER);
        verify(bus, times(1)).getProperty(ClassUnwrapper.class.getName());
    }


    @Test
    public void getRealObjectPropertyWasSetInBus() {

        when(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).thenReturn(true);
        when(bus.getProperty(ClassUnwrapper.class.getName())).thenReturn(null);

        assertSame(realObjectInternalProxy, ClassHelper.getRealObject(proxiedObject));

        verify(bus, times(1)).getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER);
        verify(bus, times(1)).getProperty(ClassUnwrapper.class.getName());

    }

    @Test
    public void getRealObjectPropertyWasNotSetInBus() {

        when(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).thenReturn(false);
        when(bus.getProperty(ClassUnwrapper.class.getName())).thenReturn(null);

        assertSame(realObjectInternalSpring, ClassHelper.getRealObject(springAopObject));

        verify(bus, times(1)).getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER);
        verify(bus, times(1)).getProperty(ClassUnwrapper.class.getName());

    }

    @Test
    public void getRealLambdaClassPropertyWasNotSetInBus() {
        when(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).thenReturn(false);
        when(bus.getProperty(ClassUnwrapper.class.getName())).thenReturn(null);

        assertSame(fn.getClass(), ClassHelper.getRealClass(fn));

        verify(bus, times(1)).getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER);
        verify(bus, times(1)).getProperty(ClassUnwrapper.class.getName());
    }

    @Test
    public void getRealLambdaClassPropertyWasSetInBus() {
        when(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).thenReturn(true);
        when(bus.getProperty(ClassUnwrapper.class.getName())).thenReturn(null);

        assertSame(fn.getClass(), ClassHelper.getRealClass(fn));

        verify(bus, times(1)).getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER);
        verify(bus, times(1)).getProperty(ClassUnwrapper.class.getName());
    }

    @Test
    public void getRealLambdaClassFromClassPropertyWasSetInBus() {
        when(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).thenReturn(true);
        when(bus.getProperty(ClassUnwrapper.class.getName())).thenReturn(null);

        assertSame(fn.getClass(), ClassHelper.getRealClassFromClass(fn.getClass()));

        verify(bus, times(1)).getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER);
        verify(bus, times(1)).getProperty(ClassUnwrapper.class.getName());
    }

    @Test
    public void getRealLambdaClassFromClassPropertyWasNotSetInBus() {
        when(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).thenReturn(false);
        when(bus.getProperty(ClassUnwrapper.class.getName())).thenReturn(null);

        assertSame(fn.getClass(), ClassHelper.getRealClassFromClass(fn.getClass()));

        verify(bus, times(1)).getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER);
        verify(bus, times(1)).getProperty(ClassUnwrapper.class.getName());
    }

    @Test
    public void getRealCglibClassFromClassPropertyWasNotSetInBus() {
        when(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).thenReturn(false);
        when(bus.getProperty(ClassUnwrapper.class.getName())).thenReturn(null);

        assertSame(Object.class, ClassHelper.getRealClassFromClass(cglibProxyObject.getClass()));

        verify(bus, times(1)).getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER);
        verify(bus, times(1)).getProperty(ClassUnwrapper.class.getName());
    }

    @Test
    public void getRealCglibClassFromClassPropertyWasSetInBus() {
        when(bus.getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER)).thenReturn(true);
        when(bus.getProperty(ClassUnwrapper.class.getName())).thenReturn(null);

        assertSame(cglibProxyObject.getClass(), ClassHelper.getRealClassFromClass(cglibProxyObject.getClass()));

        verify(bus, times(1)).getProperty(ClassHelper.USE_DEFAULT_CLASS_HELPER);
        verify(bus, times(1)).getProperty(ClassUnwrapper.class.getName());
    }

    public interface AnyInterface {
        void anyMethod();
    }


}