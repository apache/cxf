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

package org.apache.cxf.common.injection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.annotation.Resources;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.ResourceResolver;

import org.easymock.classextension.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class ResourceInjectorTest extends Assert {
    private static final String RESOURCE_ONE = "resource one";
    private static final String RESOURCE_TWO = "resource two";
    private static final String RESOURCE_THREE = "resource three";
    
    private ResourceInjector injector; 
        
    public void setUpResourceManager(String pfx) { 

        ResourceManager resMgr = EasyMock.createMock(ResourceManager.class);
        List<ResourceResolver> resolvers = new ArrayList<ResourceResolver>();
        
        resMgr.getResourceResolvers();
        EasyMock.expectLastCall().andReturn(resolvers);
        resMgr.resolveResource(pfx + "resource1", String.class, resolvers);
        EasyMock.expectLastCall().andReturn(RESOURCE_ONE);
        resMgr.resolveResource("resource2", String.class, resolvers);
        EasyMock.expectLastCall().andReturn(RESOURCE_TWO);
        resMgr.resolveResource("resource3", CharSequence.class, resolvers);
        EasyMock.expectLastCall().andReturn(RESOURCE_THREE);
        EasyMock.replay(resMgr);
        
        injector = new ResourceInjector(resMgr); 
    } 

    @Test
    public void testFieldInjection() { 
        setUpResourceManager(FieldTarget.class.getCanonicalName() + "/");
        doInjectTest(new FieldTarget()); 
    }
    
        
    @Test
    public void testFieldInSuperClassInjection() { 
        setUpResourceManager("org.apache.cxf.common.injection.FieldTarget/");
        doInjectTest(new SubFieldTarget()); 
    }
    
    @Test
    public void testSetterInSuperClassInjection() {
        setUpResourceManager("org.apache.cxf.common.injection.SetterTarget/");
        doInjectTest(new SubSetterTarget()); 
    }

    @Test
    public void testSetterInjection() {
        setUpResourceManager(SetterTarget.class.getCanonicalName() + "/");
        doInjectTest(new SetterTarget()); 
    }
    
    @Test
    public void testProxyInjection() {
        setUpResourceManager(SetterTarget.class.getCanonicalName() + "/");
        doInjectTest(getProxyObject(), SetterTarget.class);
    }
    
    @Test
    public void testEnhancedInjection() {
        setUpResourceManager(FieldTarget.class.getCanonicalName() + "/");               
        doInjectTest(getEnhancedObject());
    }

    @Test
    public void testClassLevelInjection() {
        setUpResourceManager("");
        doInjectTest(new ClassTarget());
    }

    @Test
    public void testResourcesContainer() {
        setUpResourceManager("");
        doInjectTest(new ResourcesContainerTarget()); 
    }

    @Test
    public void testPostConstruct() { 
        setUpResourceManager(SetterTarget.class.getCanonicalName() + "/");

        SetterTarget target = new SetterTarget(); 
        doInjectTest(target); 
        assertTrue(target.injectionCompleteCalled()); 
    }

    @Test
    public void testPreDestroy() { 
        injector = new ResourceInjector(null, null);
        SetterTarget target = new SetterTarget(); 
        injector.destroy(target); 
        assertTrue(target.preDestroyCalled()); 
    }

    private void doInjectTest(Target target) {
        doInjectTest(target, target.getClass());
    }
    
    private void doInjectTest(Target target, Class<?> clazz) {

        injector.inject(target, clazz);
        injector.construct(target);
        assertNotNull(target.getResource1()); 
        assertEquals(RESOURCE_ONE, target.getResource1()); 

        assertNotNull(target.getResource2()); 
        assertEquals(RESOURCE_TWO, target.getResource2());
        
        assertNotNull(target.getResource3());
        assertEquals(RESOURCE_THREE, target.getResource3());
    }
    
    private Target getProxyObject() {
        Target t = (Target)Proxy.newProxyInstance(ISetterTarget.class.getClassLoader(),
                                                  new Class[] {ISetterTarget.class},
                                                  new ProxyClass(new SetterTarget()));
        return t;
    }
        
    private FieldTarget getEnhancedObject() {
        Enhancer e = new Enhancer();
        e.setSuperclass(FieldTarget.class);        
        e.setCallback(new CallInterceptor());
        return (FieldTarget)e.create();        
    }

}


interface Target {
    String getResource1(); 
    String getResource2(); 
    CharSequence getResource3();
}

class CallInterceptor implements MethodInterceptor {
    
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        Object retValFromSuper = null;
        if (!Modifier.isAbstract(method.getModifiers())) {
            retValFromSuper = proxy.invokeSuper(obj, args);
        }        
        return retValFromSuper;            
    }
}


class FieldTarget implements Target {

    @Resource
    private String resource1; 

    @Resource(name = "resource2")
    private String resource2foo;
    
    @Resource(name = "resource3")
    private CharSequence resource3foo;

    public String getResource1() { 
        return resource1; 
    } 

    public String getResource2() { 
        return resource2foo;
    } 
    
    public CharSequence getResource3() {
        return resource3foo;
    }

    public String toString() { 
        return "[" + resource1 + ":" + resource2foo + ":" + resource3foo + "]";
    }

}

class SubFieldTarget extends FieldTarget {
}

class SubSetterTarget extends SetterTarget {
    
}

interface ISetterTarget extends Target {    
    void setResource1(final String argResource1);    
    void setResource2(final String argResource2);
    void setResource3(final CharSequence argResource3);
}

class ProxyClass implements InvocationHandler {
    Object obj;

    public ProxyClass(Object o) {
        obj = o;
    }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        Object result = null;
        try {
            Class[] types = new Class[0];
            if (args != null) {
                types = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    types[i] = args[i].getClass();
                    if ("setResource3".equals(m.getName()) && types[i].equals(String.class)) {
                        types[i] = CharSequence.class;
                    }
                }
            }    
            Method target = obj.getClass().getMethod(m.getName(), types);
            result = target.invoke(obj, args);
        } catch (InvocationTargetException e) {
            // Do nothing here
        } catch (Exception eBj) {
            eBj.printStackTrace();
        } finally {
            // Do something after the method is called ...
        }
        return result;
    }
}
class SetterTarget implements Target { 

    private String resource1;
    private String resource2;
    private CharSequence resource3;
    private boolean injectionCompletePublic; 
    private boolean injectionCompletePrivate; 
    private boolean preDestroy; 
    private boolean preDestroyPrivate; 

    public final String getResource1() {
        return this.resource1;
    }

    @Resource
    public final void setResource1(final String argResource1) {
        this.resource1 = argResource1;
    }
    
    public final String getResource2() {
        return this.resource2;
    }
    
    @Resource(name = "resource2")
    public void setResource2(final String argResource2) {
        this.resource2 = argResource2;
    }
    
    public final CharSequence getResource3() {
        return this.resource3;
    }
    
    @Resource(name = "resource3")
    public void setResource3(final CharSequence argResource3) {
        this.resource3 = argResource3;
    }

    @PostConstruct
    public void injectionIsAllFinishedNowThankYouVeryMuch() { 
        injectionCompletePublic = true;

        // stick this here to keep PMD happy...
        injectionIsAllFinishedNowThankYouVeryMuchPrivate();
    } 
    
    @PostConstruct
    private void injectionIsAllFinishedNowThankYouVeryMuchPrivate() { 
        injectionCompletePrivate = true;
    } 
    
    @PreDestroy
    public void preDestroyMethod() { 
        preDestroy = true;
    } 
    
    @PreDestroy
    private void preDestroyMethodPrivate() { 
        preDestroyPrivate = true;
    } 
    
    public boolean injectionCompleteCalled() { 
        return injectionCompletePrivate && injectionCompletePublic;
    }

    public boolean preDestroyCalled() { 
        return preDestroy && preDestroyPrivate;
    }
    
    // dummy method to access the private methods to avoid compile warnings
    public void dummyMethod() {
        preDestroyMethodPrivate();
        setResource2("");
    }
}

//CHECKSTYLE:OFF
@Resource(name = "resource1")
class ClassTarget implements Target {

    @Resource(name = "resource3") 
    public CharSequence resource3foo;
    @Resource(name = "resource2") 
    public String resource2foo; 
    private String res1; 

    public final void setResource1(String res) { 
        res1 = res; 
    } 

    public final String getResource1() {
        return res1;
    }

    public final String getResource2() {
        return resource2foo;
    }
    
    public final CharSequence getResource3() {
        return resource3foo;
    }
}



@Resources({@Resource(name = "resource1"), 
            @Resource(name = "resource2"),
            @Resource(name = "resource3") })
class ResourcesContainerTarget implements Target {

    private String res1; 
    private String resource2; 
    private CharSequence resource3;

    public final void setResource1(String res) { 
        res1 = res; 
    } 

    public final String getResource1() {
        return res1;
    }

    public final String getResource2() {
        return resource2;
    }
    
    public final CharSequence getResource3() {
        return resource3;
    }
}
