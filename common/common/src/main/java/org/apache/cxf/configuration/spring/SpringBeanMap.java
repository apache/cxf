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
package org.apache.cxf.configuration.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.Mergeable;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class SpringBeanMap<V> 
    extends AbstractSpringBeanMap<String, V> {

    
    protected void processBeans(ApplicationContext beanFactory) {
        if (beanFactory == null) {
            return;
        }

        String[] beanNames = beanFactory.getBeanNamesForType(type);

        ConfigurableApplicationContext ctxt = (ConfigurableApplicationContext)beanFactory;

        // Take any bean name or alias that has a web service annotation
        for (int i = 0; i < beanNames.length; i++) {
            BeanDefinition def = ctxt.getBeanFactory().getBeanDefinition(beanNames[i]);

            if (!def.isSingleton() || def.isAbstract()) {
                continue;
            }

            try {
                Collection<?> ids = null;
                PropertyValue pv = def.getPropertyValues().getPropertyValue(idsProperty);
                
                if (pv != null) {
                    Object value = pv.getValue();
                    if (!(value instanceof Collection)) {
                        throw new RuntimeException("The property " + idsProperty + " must be a collection!");
                    }

                    if (value instanceof Mergeable) {
                        if (!((Mergeable)value).isMergeEnabled()) {
                            ids = (Collection<?>)value;
                        }
                    } else {
                        ids = (Collection<?>)value;
                    }
                } 
                
                if (ids == null) {
                    ids = getIds(ctxt.getBean(beanNames[i]));
                    if (ids == null) {
                        continue;
                    }
                }
                
                if (ids instanceof ManagedSet || ids instanceof ManagedList) {
                    List<String> newIds = new ArrayList<String>();
                    for (Iterator itr = ids.iterator(); itr.hasNext();) {
                        Object o = itr.next();
                        if (o instanceof TypedStringValue) {
                            newIds.add(((TypedStringValue) o).getValue());
                        } else {
                            newIds.add((String) o);
                        }
                    }
                    ids = newIds;
                }
                for (Object id : ids) {
                    getBeanListForId(id.toString()).add(beanNames[i]);
                }
            } catch (BeanIsAbstractException e) {
                // The bean is abstract, we won't be doing anything with it.
                continue;
            }
        }

        processBeans(ctxt.getParent());
    }
}
