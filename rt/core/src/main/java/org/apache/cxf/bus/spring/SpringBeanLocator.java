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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerImpl;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.osgi.framework.ServiceReference;
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
    ConfiguredBeanLocator orig;
    Set<String> passThroughs = new HashSet<String>();
    Object bundleContext;
    boolean osgi = true;
    
    public SpringBeanLocator(ApplicationContext ctx, Bus bus) {
        context = ctx;
        orig = bus.getExtension(ConfiguredBeanLocator.class);
        if (orig instanceof ExtensionManagerImpl) {
            List<String> names = new ArrayList<String>();
            for (String s : ctx.getBeanDefinitionNames()) {
                ConfigurableApplicationContext ctxt = (ConfigurableApplicationContext)context;
                BeanDefinition def = ctxt.getBeanFactory().getBeanDefinition(s);
                String cn =  def.getBeanClassName();
                if (OldSpringSupport.class.getName().equals(cn)) {
                    passThroughs.add(s);
                    for (String s2 : ctx.getAliases(s)) {
                        passThroughs.add(s2);
                    }
                } else {
                    names.add(s);
                    for (String s2 : ctx.getAliases(s)) {
                        names.add(s2);
                    }
                }
            }
            
            ((ExtensionManagerImpl)orig).removeBeansOfNames(names);
        }
        
        loadOSGIContext(bus);
    }

    private void loadOSGIContext(Bus b) {
        try {
            //use a little reflection to allow this to work without the spring-dm jars
            //for the non-osgi cases
            Method m = context.getClass().getMethod("getBundleContext");
            bundleContext = m.invoke(context);
            @SuppressWarnings("unchecked")
            Class<Object> cls = (Class<Object>)m.getReturnType();
            b.setExtension(bundleContext, cls);
        } catch (Throwable t) {
            //ignore
            osgi = false;
        }
    }
    
    
    /** {@inheritDoc}*/
    public List<String> getBeanNamesOfType(Class<?> type) {
        Set<String> s = new LinkedHashSet<String>(Arrays.asList(context.getBeanNamesForType(type,
                                                                                         false,
                                                                                         true)));
        s.removeAll(passThroughs);
        s.addAll(orig.getBeanNamesOfType(type));
        return new ArrayList<String>(s);
    }

    /** {@inheritDoc}*/
    public <T> Collection<? extends T> getBeansOfType(Class<T> type) {
        Set<String> s = new LinkedHashSet<String>(Arrays.asList(context.getBeanNamesForType(type,
                                                                                            false,
                                                                                            true)));
        s.removeAll(passThroughs);
        List<T> lst = new LinkedList<T>();
        for (String n : s) {
            lst.add(context.getBean(n, type));
        }
        lst.addAll(orig.getBeansOfType(type));
        if (lst.isEmpty()) {
            tryOSGI(lst, type);
        }
        return lst;
    }
    private <T> void tryOSGI(Collection<T> lst, Class<T> type) {
        if (!osgi) {
            return;
        }
        try {
            //use a little reflection to allow this to work without the spring-dm jars
            //for the non-osgi cases

            Object o = bundleContext.getClass()
                .getMethod("getServiceReference", String.class).invoke(bundleContext, type.getName());
            if (o != null) {
                o = bundleContext.getClass().getMethod("getService", ServiceReference.class)
                    .invoke(bundleContext, o);
                lst.add(type.cast(o));
            }
        } catch (NoSuchMethodException e) {
            osgi = false;
            //not using OSGi
        } catch (Throwable e) {
            //ignore
        }
    }
    

    public <T> boolean loadBeansOfType(Class<T> type,
                                       BeanLoaderListener<T> listener) {
        List<String> list = new ArrayList<String>(Arrays.asList(context.getBeanNamesForType(type,
                                                                                            false,
                                                                                            true)));
        list.removeAll(passThroughs);
        Collections.reverse(list);
        boolean loaded = false;
        for (String s : list) {
            Class<?> beanType = context.getType(s);
            Class<? extends T> t = beanType.asSubclass(type);
            if (listener.loadBean(s, t)) {
                Object o = context.getBean(s);
                if (listener.beanLoaded(s, type.cast(o))) {
                    return true;
                }
                loaded = true;
            }
        }
        return loaded || orig.loadBeansOfType(type, listener);
    }

    public boolean hasConfiguredPropertyValue(String beanName, String propertyName, String searchValue) {
        if (context.containsBean(beanName) && !passThroughs.contains(beanName)) {
            ConfigurableApplicationContext ctxt = (ConfigurableApplicationContext)context;
            BeanDefinition def = ctxt.getBeanFactory().getBeanDefinition(beanName);
            if (!ctxt.getBeanFactory().isSingleton(beanName) || def.isAbstract()) {
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
        }
        return orig.hasConfiguredPropertyValue(beanName, propertyName, searchValue);
    }

}
