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
package org.apache.cxf.jaxrs.spring;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;

import jakarta.ws.rs.core.Application;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * The ResourceProvider implementation which delegates to
 * ApplicationContext to manage the life-cycle of the resource
 */
public class SpringResourceFactory implements ResourceProvider, ApplicationContextAware {

    private Constructor<?> c;
    private Class<?> type;
    private ApplicationContext ac;
    private String beanId;
    private Method postConstructMethod;
    private Method preDestroyMethod;
    private boolean isSingleton;
    private boolean isPrototype;
    private boolean callPostConstruct;
    private boolean callPreDestroy = true;
    private String postConstructMethodName;
    private String preDestroyMethodName;
    private Object singletonInstance;

    public SpringResourceFactory() {

    }

    public SpringResourceFactory(String name) {
        beanId = name;
    }

    private void init() {
        type = ClassHelper.getRealClassFromClass(ac.getType(beanId));
        if (Proxy.isProxyClass(type)) {
            type = ClassHelper.getRealClass(ac.getBean(beanId));
        }
        isSingleton = ac.isSingleton(beanId);
        postConstructMethod = ResourceUtils.findPostConstructMethod(type, postConstructMethodName);
        preDestroyMethod = ResourceUtils.findPreDestroyMethod(type, preDestroyMethodName);

        if (isSingleton()) {
            try {
                singletonInstance = ac.getBean(beanId);
            } catch (BeansException ex) {
                // ignore for now, try resolving resource constructor later
            }
            if (singletonInstance != null) {
                return;
            }
        } else {
            isPrototype = ac.isPrototype(beanId);
        }
        c = ResourceUtils.findResourceConstructor(type, !isSingleton());
        if (c == null) {
            throw new RuntimeException("Resource class " + type
                                       + " has no valid constructor");
        }

    }

    /**
     * {@inheritDoc}
     */
    public Object getInstance(Message m) {
        if (singletonInstance != null) {
            return singletonInstance;
        }
        ProviderInfo<?> application = m == null ? null
            : (ProviderInfo<?>)m.getExchange().getEndpoint().get(Application.class.getName());
        Map<Class<?>, Object> mapValues = CastUtils.cast(application == null ? null
            : Collections.singletonMap(Application.class, application.getProvider()));
        Object[] values = ResourceUtils.createConstructorArguments(c, m, !isSingleton(), mapValues);
        Object instance = values.length > 0 ? ac.getBean(beanId, values) : ac.getBean(beanId);
        initInstance(m, instance);
        return instance;
    }

    protected void initInstance(Message m, Object instance) {
        if (isCallPostConstruct()) {
            InjectionUtils.invokeLifeCycleMethod(ClassHelper.getRealObject(instance), postConstructMethod);
        }
    }



    /**
     * {@inheritDoc}
     */
    public boolean isSingleton() {
        return isSingleton;
    }

    /**
     * {@inheritDoc}
     */
    public void releaseInstance(Message m, Object o) {
        if (doCallPreDestroy()) {
            InjectionUtils.invokeLifeCycleMethod(o, preDestroyMethod);
        }
    }

    protected boolean doCallPreDestroy() {
        return isCallPreDestroy() && isPrototype;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ac = applicationContext;
        init();
    }

    public void setBeanId(String serviceBeanId) {
        this.beanId = serviceBeanId;
    }

    public ApplicationContext getApplicationContext() {
        return ac;
    }

    Constructor<?> getBeanConstructor() {
        return c;
    }

    /**
     * {@inheritDoc}
     */
    public Class<?> getResourceClass() {
        return type;
    }

    public void setCallPostConstruct(boolean callPostConstruct) {
        this.callPostConstruct = callPostConstruct;
    }

    public boolean isCallPostConstruct() {
        return this.callPostConstruct;
    }

    public void setCallPreDestroy(boolean callPreDestroy) {
        this.callPreDestroy = callPreDestroy;
    }

    public boolean isCallPreDestroy() {
        return this.callPreDestroy;
    }

    public void setPreDestroyMethodName(String preDestroyMethodName) {
        this.preDestroyMethodName = preDestroyMethodName;
    }

    public void setPostConstructMethodName(String postConstructMethodName) {
        this.postConstructMethodName = postConstructMethodName;
    }
}
