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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.helpers.CastUtils;
import org.springframework.beans.Mergeable;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

@NoJSR250Annotations
public class SpringBeanQNameMap<V> 
    extends AbstractSpringBeanMap<QName, V> {

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
                if (ids == null && staticFieldName != null) {
                    Class<?> cls = context.getType(beanNames[i]);
                    try {
                        Field f = cls.getDeclaredField(staticFieldName);
                        f.setAccessible(true);
                        Collection<QName> sids = CastUtils.cast((Collection<?>)f.get(null));
                        if (sids != null) {
                            ids = new ArrayList<QName>(sids);
                        }
                    } catch (Exception ex) {
                        //ignore, fall through
                    }
                }

                // if values are not legal keys (for lazy-init bean definitions id values may be
                // BeanDefinitionHolders), load the bean and get its id values instead
                // for BeanReference type values, simply resolve reference
                //  
                if (null != ids) {
                    Collection<Object> checked = new ArrayList<Object>(ids.size());
                    for (Object id : ids) {
                        if (id instanceof QName) {
                            checked.add(id);
                        } else if (id instanceof BeanReference) {
                            BeanReference br = (BeanReference)id;
                            Object refId = context.getBean(br.getBeanName());
                            checked.add(refId);
                        } else if (id instanceof BeanDefinitionHolder) {
                            BeanDefinitionHolder bdh = (BeanDefinitionHolder)id;
                            if (QName.class.getName().equals(bdh.getBeanDefinition().getBeanClassName())) {
                                try {
                                    java.util.List l = bdh.getBeanDefinition().getConstructorArgumentValues()
                                        .getGenericArgumentValues();
                                    
                                    ConstructorArgumentValues.ValueHolder v 
                                        = (ConstructorArgumentValues.ValueHolder)l.get(0);
                                    
                                    TypedStringValue nss = (TypedStringValue)v.getValue();
                                    v = (ConstructorArgumentValues.ValueHolder)l.get(1);
                                    TypedStringValue ln = (TypedStringValue)v.getValue();
                                    checked.add(new QName(nss.getValue(), ln.getValue()));
                                } catch (Exception ex) {
                                    //ignore
                                    break;
                                }
                            } else {
                                break;
                            }
                        } else {
                            
                            break;
                        }
                    }
                    if (checked.size() < ids.size()) {
                        ids = null;
                    } else {
                        ids = checked;
                    }
                } 
                if (ids == null) {
                    ids = getIds(ctxt.getBean(beanNames[i]));
                    if (ids == null) {
                        continue;
                    }
                }
                
                for (Object id : ids) {
                    QName key = (QName)id;
                    getBeanListForId(key).add(beanNames[i]);
                }
            } catch (BeanIsAbstractException e) {
                // The bean is abstract, we won't be doing anything with it.
                continue;
            }
        }

        processBeans(ctxt.getParent());
    }

}
