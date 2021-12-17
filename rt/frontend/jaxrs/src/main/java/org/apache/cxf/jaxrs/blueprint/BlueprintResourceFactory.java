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
package org.apache.cxf.jaxrs.blueprint;

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
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.BeanMetadata;

public class BlueprintResourceFactory implements ResourceProvider {

    private BlueprintContainer blueprintContainer;
    private Constructor<?> c;
    private String beanId;
    private Method postConstructMethod;
    private Method preDestroyMethod;
    private boolean isSingleton;

    public BlueprintResourceFactory() {

    }

    public BlueprintResourceFactory(String name) {
        beanId = name;
    }

    private void init() {
        Class<?> type = ClassHelper.getRealClassFromClass(blueprintContainer.getComponentInstance(beanId)
                                                          .getClass());
        if (Proxy.isProxyClass(type)) {
            type = ClassHelper.getRealClass(blueprintContainer.getComponentInstance(beanId));
        }
        c = ResourceUtils.findResourceConstructor(type, !isSingleton());
        if (c == null) {
            throw new RuntimeException("Resource class " + type + " has no valid constructor");
        }
        postConstructMethod = ResourceUtils.findPostConstructMethod(type);
        preDestroyMethod = ResourceUtils.findPreDestroyMethod(type);

        Object component = blueprintContainer.getComponentMetadata(beanId);
        if (component instanceof BeanMetadata) {
            BeanMetadata local = (BeanMetadata) component;
            isSingleton = BeanMetadata.SCOPE_SINGLETON.equals(local.getScope())
                || (local.getScope() == null && local.getId() != null);
        }
    }

    public Object getInstance(Message m) {
        //TODO -- This is not the BP way.
        ProviderInfo<?> application = m == null ? null
            : (ProviderInfo<?>)m.getExchange().getEndpoint().get(Application.class.getName());
        Map<Class<?>, Object> mapValues = CastUtils.cast(application == null ? null
            : Collections.singletonMap(Application.class, application.getProvider()));
        Object[] values = ResourceUtils.createConstructorArguments(c, m, !isSingleton(), mapValues);
        //TODO Very springish...
        Object instance = values.length > 0 ? blueprintContainer.getComponentInstance(beanId)
            : blueprintContainer.getComponentInstance(beanId);
        if (!isSingleton() || m == null) {
            InjectionUtils.invokeLifeCycleMethod(instance, postConstructMethod);
        }
        return instance;
    }

    public boolean isSingleton() {
        return isSingleton;
    }

    public void releaseInstance(Message m, Object o) {
        if (!isSingleton()) {
            InjectionUtils.invokeLifeCycleMethod(o, preDestroyMethod);
        }
    }

    public void setBeanId(String serviceBeanId) {
        this.beanId = serviceBeanId;
    }

    Constructor<?> getBeanConstructor() {
        return c;
    }

    public Class<?> getResourceClass() {
        return c.getDeclaringClass();
    }

    public void setBlueprintContainer(BlueprintContainer blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
        init();
    }
}
