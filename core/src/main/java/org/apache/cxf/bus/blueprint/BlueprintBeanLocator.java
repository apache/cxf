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

package org.apache.cxf.bus.blueprint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.aries.blueprint.ExtendedBeanMetadata;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.apache.cxf.bus.extension.ExtensionManagerImpl;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.NoSuchComponentException;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.ComponentMetadata;

/**
 *
 */
public class BlueprintBeanLocator implements ConfiguredBeanLocator {
    private static final Logger LOG = LogUtils.getL7dLogger(BlueprintBeanLocator.class);
    ConfiguredBeanLocator orig;
    BlueprintContainer container;
    BundleContext context;

    public BlueprintBeanLocator(ConfiguredBeanLocator orig,
                                BlueprintContainer cont,
                                BundleContext context) {
        this.orig = orig;
        this.container = cont;
        this.context = context;
        if (orig instanceof ExtensionManagerImpl) {
            List<String> names = new ArrayList<>(container.getComponentIds());
            ((ExtensionManagerImpl)orig).removeBeansOfNames(names);
        }
    }

    static Class<?> getClassForMetaData(BlueprintContainer container, ComponentMetadata cmd) {
        Class<?> cls = null;
        if (cmd instanceof BeanMetadata) {
            BeanMetadata bm = (BeanMetadata)cmd;
            if (bm instanceof ExtendedBeanMetadata) {
                cls = ((ExtendedBeanMetadata)bm).getRuntimeClass();
            }
            if (cls == null && container instanceof ExtendedBlueprintContainer && bm.getClassName() != null) {
                try {
                    cls = ((ExtendedBlueprintContainer)container).loadClass(bm.getClassName());
                } catch (ClassNotFoundException cnfe) {
                    //ignore
                }
            }
        }
        return cls;
    }
    private Class<?> getClassForMetaData(ComponentMetadata cmd) {
        return getClassForMetaData(container, cmd);
    }
    private ComponentMetadata getComponentMetadata(String id) {
        try {
            return container.getComponentMetadata(id);
        } catch (NoSuchComponentException nsce) {
            return null;
        }
    }

    public <T> T getBeanOfType(String name, Class<T> type) {

        ComponentMetadata cmd = getComponentMetadata(name);
        Class<?> cls = getClassForMetaData(cmd);
        if (cls != null && type.isAssignableFrom(cls)) {
            return type.cast(container.getComponentInstance(name));
        }
        return orig.getBeanOfType(name, type);
    }
    /** {@inheritDoc}*/
    public List<String> getBeanNamesOfType(Class<?> type) {
        Set<String> names = new LinkedHashSet<>();
        for (String s : container.getComponentIds()) {
            ComponentMetadata cmd = container.getComponentMetadata(s);
            Class<?> cls = getClassForMetaData(cmd);
            if (cls != null && type.isAssignableFrom(cls)) {
                names.add(s);
            }
        }
        names.addAll(orig.getBeanNamesOfType(type));
        return new ArrayList<>(names);
    }

    /** {@inheritDoc}*/
    public <T> Collection<? extends T> getBeansOfType(Class<T> type) {
        List<T> list = new ArrayList<>();

        for (String s : container.getComponentIds()) {
            ComponentMetadata cmd = container.getComponentMetadata(s);
            Class<?> cls = getClassForMetaData(cmd);
            if (cls != null && type.isAssignableFrom(cls)) {
                list.add(type.cast(container.getComponentInstance(s)));
            }
        }
        if (list.isEmpty() && context != null) {
            try {
                ServiceReference<?>[] refs = context.getServiceReferences(type.getName(), null);
                if (refs != null) {
                    for (ServiceReference<?> r : refs) {
                        list.add(type.cast(context.getService(r)));
                    }
                }
            } catch (Exception ex) {
                //ignore, just don't support the OSGi services
                LOG.info("Try to find the Bean with type:" + type
                    + " from OSGi services and get error: " + ex);
            }
        }
        list.addAll(orig.getBeansOfType(type));

        return list;
    }

    /** {@inheritDoc}*/
    public <T> boolean loadBeansOfType(Class<T> type, BeanLoaderListener<T> listener) {
        List<String> names = new ArrayList<>();
        boolean loaded = false;
        for (String s : container.getComponentIds()) {
            ComponentMetadata cmd = container.getComponentMetadata(s);
            Class<?> cls = getClassForMetaData(cmd);
            if (cls != null && type.isAssignableFrom(cls)) {
                names.add(s);
            }
        }
        Collections.reverse(names);
        for (String s : names) {
            ComponentMetadata cmd = container.getComponentMetadata(s);
            Class<?> beanType = getClassForMetaData(cmd);
            Class<? extends T> t = beanType.asSubclass(type);
            if (listener.loadBean(s, t)) {
                Object o = container.getComponentInstance(s);
                if (listener.beanLoaded(s, type.cast(o))) {
                    return true;
                }
                loaded = true;
            }
        }

        try {
            if (context != null) {
                ServiceReference<?>[] refs = context.getServiceReferences(type.getName(), null);
                if (refs != null) {
                    for (ServiceReference<?> r : refs) {
                        Object o2 = context.getService(r);
                        Class<? extends T> t = o2.getClass().asSubclass(type);
                        if (listener.loadBean(t.getName(), t)) {
                            if (listener.beanLoaded(t.getName(), type.cast(o2))) {
                                return true;
                            }
                            loaded = true;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            //ignore, just don't support the OSGi services
            LOG.info("Try to find the Bean with type:" + type
                + " from OSGi services and get error: " + ex);
        }
        return orig.loadBeansOfType(type, listener) || loaded;
    }

    public boolean hasConfiguredPropertyValue(String beanName, String propertyName, String value) {
        ComponentMetadata cmd = getComponentMetadata(beanName);
        if (cmd instanceof BeanMetadata) {
            BeanMetadata br = (BeanMetadata)cmd;
            for (BeanProperty s : br.getProperties()) {
                if (propertyName.equals(s.getName())) {
                    return true;
                }
            }
            return false;
        }
        return orig.hasConfiguredPropertyValue(beanName, propertyName, value);
    }

    public boolean hasBeanOfName(String name) {
        ComponentMetadata cmd = getComponentMetadata(name);
        if (cmd instanceof BeanMetadata) {
            return true;
        }
        return orig.hasBeanOfName(name);
    }

}
