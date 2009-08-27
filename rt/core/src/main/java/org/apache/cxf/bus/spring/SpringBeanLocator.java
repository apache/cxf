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

package org.apache.cxf.bus.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.springframework.beans.Mergeable;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 
 */
public class SpringBeanLocator implements ConfiguredBeanLocator {
    ApplicationContext context;
    public SpringBeanLocator(ApplicationContext ctx) {
        context = ctx;
    }

    /** {@inheritDoc}*/
    public List<String> getBeanNamesOfType(Class<?> type) {
        return Arrays.asList(context.getBeanNamesForType(type, false, true));
    }

    /** {@inheritDoc}*/
    public <T> T getBeanOfType(String name, Class<T> type) {
        return type.cast(context.getBean(name, type));
    }

    /** {@inheritDoc}*/
    @SuppressWarnings("unchecked")
    public <T> Collection<? extends T> getBeansOfType(Class<T> type) {
        return context.getBeansOfType(type, false, true).values();
    }

    @SuppressWarnings("unchecked")
    public <T> boolean loadBeansOfType(Class<T> type,
                                       BeanLoaderListener<T> listener) {
        List<String> list = new ArrayList<String>(Arrays.asList(context.getBeanNamesForType(type, 
                                                                                            false, 
                                                                                            true)));
        Collections.reverse(list);
        for (String s : list) {
            Class<? extends T> c = context.getType(s);
            if (listener.loadBean(s, c)) {
                Object o = context.getBean(s);
                if (listener.beanLoaded(s, type.cast(o))) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasConfiguredPropertyValue(String beanName, String propertyName, String searchValue) {
        ConfigurableApplicationContext ctxt = (ConfigurableApplicationContext)context;
        BeanDefinition def = ctxt.getBeanFactory().getBeanDefinition(beanName);
        if (!def.isSingleton() || def.isAbstract() || def.isAbstract()) {
            return false;
        }
        Collection<?> ids = null;
        PropertyValue pv = def.getPropertyValues().getPropertyValue(propertyName);
        
        if (pv != null) {
            Object value = pv.getValue();
            if (!(value instanceof Collection)) {
                throw new RuntimeException("The property " + propertyName + " must be a collection!");
            }

            if (value instanceof Mergeable) {
                if (!((Mergeable)value).isMergeEnabled()) {
                    ids = (Collection<?>)value;
                }
            } else {
                ids = (Collection<?>)value;
            }
        } 
        
        if (ids != null) {
            for (Iterator itr = ids.iterator(); itr.hasNext();) {
                Object o = itr.next();
                if (o instanceof TypedStringValue) {
                    if (searchValue.equals(((TypedStringValue) o).getValue())) {
                        return true;
                    }
                } else {
                    if (searchValue.equals((String)o)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
