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

package org.apache.cxf.service.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.namespace.QName;

public abstract class AbstractPropertiesHolder implements Extensible {
    private AtomicReference<Map<String, Object>> propertyMap = new AtomicReference<Map<String, Object>>();
    private AtomicReference<Object[]> extensors = new AtomicReference<Object[]>();
    private Map<QName, Object> extensionAttributes;
    
    public Object getProperty(String name) {
        if (null == propertyMap.get()) {
            return null;
        }
        return propertyMap.get().get(name);
    }
    
    public <T> T getProperty(String name, Class<T> cls) {
        return cls.cast(getProperty(name));
    }
    public boolean hasProperty(String name) {
        Map<String, Object> map = propertyMap.get();
        if (map != null) {
            return map.containsKey(name);
        }
        return false;
    }
    
    public void setProperty(String name, Object v) {
        if (null == propertyMap.get()) {
            propertyMap.compareAndSet(null, new ConcurrentHashMap<String, Object>(4));
        }
        if (v == null) {
            propertyMap.get().remove(name);
        } else {
            propertyMap.get().put(name, v);
        }
    }
    
    public boolean containsExtensor(Object el) {
        Object exts[] = extensors.get();
        if (exts != null) {
            for (Object o : exts) {
                if (o == el) {
                    return true;
                }
            }
        }
        return false;
    }
    public void addExtensor(Object el) {
        Object exts[] = extensors.get();
        Object exts2[];
        if (exts == null) {
            exts2 = new Object[1];
        } else {
            exts2 = new Object[exts.length + 1];
            for (int i = 0; i < exts.length; i++) {
                exts2[i] = exts[i];
            }
        }
        exts2[exts2.length - 1] = el;
        if (!extensors.compareAndSet(exts, exts2)) {
            //keep trying
            addExtensor(el);
        }
    }

    public <T> T getExtensor(Class<T> cls) {
        Object exts[] = extensors.get();
        if (exts == null) {
            return null;
        }
        for (int x = 0; x < exts.length; x++) {
            if (cls.isInstance(exts[x])) {
                return cls.cast(exts[x]);
            }
        }
        return null;
    }
    public <T> List<T> getExtensors(Class<T> cls) {
        Object exts[] = extensors.get();
        if (exts == null) {
            return null;
        }
        List<T> list = new ArrayList<T>(exts.length);
        for (int x = 0; x < exts.length; x++) {
            if (cls.isInstance(exts[x])) {
                list.add(cls.cast(exts[x]));
            }
        }
        return list;
    }

    public AtomicReference<Object[]> getExtensors() {
        return extensors;
    }
    
      
    public Object getExtensionAttribute(QName name) {        
        return null == extensionAttributes ? null : extensionAttributes.get(name);
    }

    public Map<QName, Object> getExtensionAttributes() {
        return extensionAttributes;
    }
    
    public void addExtensionAttribute(QName name, Object attr) {
        if (null == extensionAttributes) {
            extensionAttributes = new HashMap<QName, Object>();
        }
        extensionAttributes.put(name, attr);
    }
   
    public void setExtensionAttributes(Map<QName, Object> attrs) {
        extensionAttributes = attrs;        
    }

    /**
     * Lookup a configuration value. This may be found in the properties holder supplied
     * (i.e. an EndpointInfo or ServiceInfo), or it may be a property on the Bus itself.
     * If no value is found, the defaultValue is returned.
     * 
     * @param defaultValue the default value
     * @param type the extensor type
     * @return the configuration value or the default
     */
    public <T> T getTraversedExtensor(T defaultValue, Class<T> type) {
        T extensor = getExtensor(type);
        if (extensor == null) {
            return defaultValue;
        }
        return extensor;
    }
}
