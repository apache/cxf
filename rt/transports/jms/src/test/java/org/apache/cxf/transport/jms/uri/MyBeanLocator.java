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
package org.apache.cxf.transport.jms.uri;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.configuration.ConfiguredBeanLocator;

public class MyBeanLocator implements ConfiguredBeanLocator {
    ConfiguredBeanLocator cbl;
    Map<String, Object> registry = new HashMap<>();

    public MyBeanLocator(ConfiguredBeanLocator cbl) {
        this.cbl = cbl;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getBeanOfType(String name, Class<T> type) {
        if (registry.containsKey(name)) {
            return (T)registry.get(name);
        }
        return cbl.getBeanOfType(name, type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Collection<? extends T> getBeansOfType(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (String name : registry.keySet()) {
            Object bean = registry.get(name);
            if (type.isAssignableFrom(bean.getClass())) {
                result.add((T)bean);
            }
        }
        result.addAll(cbl.getBeansOfType(type));
        return result;
    }

    public <T> boolean loadBeansOfType(Class<T> type, BeanLoaderListener<T> listener) {
        return cbl.loadBeansOfType(type, listener);

    }
    public boolean hasConfiguredPropertyValue(String beanName, String propertyName, String value) {
        return cbl.hasConfiguredPropertyValue(beanName, propertyName, value);
    }

    public List<String> getBeanNamesOfType(Class<?> type) {
        return cbl.getBeanNamesOfType(type);
    }
    public boolean hasBeanOfName(String name) {
        return cbl.hasBeanOfName(name);
    }

    public void register(String name, Object object) {
        registry.put(name, object);
    }

}
