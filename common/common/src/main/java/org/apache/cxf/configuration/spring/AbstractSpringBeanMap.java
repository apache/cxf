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

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.cxf.helpers.CastUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

abstract class AbstractSpringBeanMap<X, V> 
    implements ApplicationContextAware, InitializingBean, MapProvider<X, V>, Serializable {
    protected ApplicationContext context;
    protected Class<?> type;
    protected String idsProperty;
    protected Map<X, List<String>> idToBeanName = new ConcurrentHashMap<X, List<String>>();
    protected Map<X, V> putStore = new ConcurrentHashMap<X, V>();

    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.context = ctx;
    }

    public void afterPropertiesSet() throws Exception {
        processBeans(context);
    }
    
    public Map<X, V> createMap() {
        return new SpringBeanMapWrapper(); 
    }
    
    protected abstract void processBeans(ApplicationContext beanFactory);

    protected synchronized List<String> getBeanListForId(X id) {
        List<String> lst = idToBeanName.get(id);
        if (lst == null) {
            lst = new CopyOnWriteArrayList<String>();
            idToBeanName.put(id, lst);
        }
        return lst;
    }

    protected Collection<String> getIds(Object bean) {
        try {
            PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(bean.getClass(), idsProperty);
            Method method = pd.getReadMethod();
            Collection<String> c = CastUtils.cast((Collection<?>)method.invoke(bean, new Object[0]));

            return c;
        } catch (IllegalArgumentException e) {
            throw new BeanInitializationException("Could not retrieve ids.", e);
        } catch (IllegalAccessException e) {
            throw new BeanInitializationException("Could not access id getter.", e);
        } catch (InvocationTargetException e) {
            throw new BeanInitializationException("Could not invoke id getter.", e);
        } catch (SecurityException e) {
            throw new BeanInitializationException("Could not invoke id getter.", e);
        }
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public String getIdsProperty() {
        return idsProperty;
    }

    public void setIdsProperty(String idsProperty) {
        this.idsProperty = idsProperty;
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean containsKey(Object key) {
        return idToBeanName.containsKey(key) || putStore.containsKey(key);
    }

    public boolean containsValue(Object arg0) {
        throw new UnsupportedOperationException();
    }

    public Set<java.util.Map.Entry<X, V>> entrySet() {
        Set<Map.Entry<X, V>> entries = new LinkedHashSet<Map.Entry<X, V>>();
        for (X k : keySet()) {
            entries.add(new Entry<X, V>(this, k));
        }
        return entries;
    }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        List<String> names = idToBeanName.get(key);
        
        if (names != null) {
            for (String name : names) {
                context.getBean(name);
            }
            if (putStore.containsKey(key)) {
                return putStore.get(key);
            }
            V v = (V)context.getBean(names.get(0));
            putStore.put((X)key, v);
            idToBeanName.remove(key);
            return v;
        } else {
            return putStore.get(key);
        }
    }

    public boolean isEmpty() {
        return idToBeanName.isEmpty() && putStore.isEmpty();
    }

    public Set<X> keySet() {
        Set<X> keys = new LinkedHashSet<X>();
        keys.addAll(putStore.keySet());
        keys.addAll(idToBeanName.keySet());
        return keys;
    }

    public V put(X key, V value) {
        // Make sure we don't take the key from Spring any more
        idToBeanName.remove(key);
        return putStore.put(key, value);
    }

    public void putAll(Map<? extends X, ? extends V> m) {
        putStore.putAll(m);
    }

    public V remove(Object key) {
        V v = get(key);
        if (v != null) {
            idToBeanName.remove(key);
        } else {
            v = putStore.get(key);
        }

        return v;
    }

    public int size() {
        return idToBeanName.size() + putStore.size();
    }

    public Collection<V> values() {
        List<V> values = new ArrayList<V>();
        values.addAll(putStore.values());
        for (X id : idToBeanName.keySet()) {
            values.add(get(id));
        }
        return values;
    }
    
    public static class Entry<X, V> implements Map.Entry<X, V> {
        private AbstractSpringBeanMap<X, V> map;
        private X key;

        public Entry(AbstractSpringBeanMap<X, V> map, X key) {
            this.map = map;
            this.key = key;
        }
        
        public X getKey() {
            return key;
        }

        public V getValue() {
            return map.get(key);
        }

        public V setValue(V value) {
            return map.put(key, value);
        }
    }
    
    private class SpringBeanMapWrapper implements Map<X, V> {

        public void clear() {
            AbstractSpringBeanMap.this.clear();
        }

        public boolean containsKey(Object key) {
            return AbstractSpringBeanMap.this.containsKey(key);
        }

        public boolean containsValue(Object value) {
            return AbstractSpringBeanMap.this.containsValue(value);
        }

        public Set<java.util.Map.Entry<X, V>> entrySet() {
            return AbstractSpringBeanMap.this.entrySet();
        }

        public V get(Object key) {
            return AbstractSpringBeanMap.this.get(key);
        }

        public boolean isEmpty() {
            return AbstractSpringBeanMap.this.isEmpty();
        }

        public Set<X> keySet() {
            return AbstractSpringBeanMap.this.keySet();
        }

        public V put(X key, V value) {
            return AbstractSpringBeanMap.this.put(key, value);
        }

        public void putAll(Map<? extends X, ? extends V> t) {
            AbstractSpringBeanMap.this.putAll(t);
        }

        public V remove(Object key) {
            return AbstractSpringBeanMap.this.remove(key);
        }

        public int size() {
            return AbstractSpringBeanMap.this.size();
        }

        public Collection<V> values() {
            return AbstractSpringBeanMap.this.values();
        }
        
    }
}
