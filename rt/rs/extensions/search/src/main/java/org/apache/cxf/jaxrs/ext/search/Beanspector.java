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
package org.apache.cxf.jaxrs.ext.search;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Bean introspection utility.
 */
public class Beanspector<T> {

    private Class<T> tclass;
    private T tobj;
    private Map<String, Method> getters = new LinkedHashMap<String, Method>();
    private Map<String, Method> setters = new LinkedHashMap<String, Method>();

    public Beanspector(Class<T> tclass) {
        if (tclass == null) {
            throw new IllegalArgumentException("tclass is null");
        }
        this.tclass = tclass;
        init();
    }

    public Beanspector(T tobj) {
        if (tobj == null) {
            throw new IllegalArgumentException("tobj is null");
        }
        this.tobj = tobj;
        init();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        if (tclass == null) {
            tclass = (Class<T>)tobj.getClass();
        }
        for (Method m : tclass.getMethods()) {
            if (isGetter(m)) {
                getters.put(getPropertyName(m), m);
            } else if (isSetter(m)) {
                setters.put(getPropertyName(m), m);
            }
        }
        // check type equality for getter-setter pairs
        Set<String> pairs = new HashSet<String>(getters.keySet());
        pairs.retainAll(setters.keySet());
        for (String accessor : pairs) {
            Class<?> getterClass = getters.get(accessor).getReturnType();
            Class<?> setterClass = setters.get(accessor).getParameterTypes()[0];
            if (!getterClass.equals(setterClass)) {
                throw new IllegalArgumentException(String
                    .format("Accessor '%s' type mismatch, getter type is %s while setter type is %s",
                            accessor, getterClass.getName(), setterClass.getName()));
            }
        }
    }

    public T getBean() {
        return tobj;
    }

    public Set<String> getGettersNames() {
        return Collections.unmodifiableSet(getters.keySet());
    }

    public Set<String> getSettersNames() {
        return Collections.unmodifiableSet(setters.keySet());
    }

    public Class<?> getAccessorType(String getterOrSetterName) throws Exception {
        Method m = getters.get(getterOrSetterName);
        if (m == null) {
            m = setters.get(getterOrSetterName);
        }
        if (m == null) {
            String msg = String.format("Accessor '%s' not found, "
                                       + "known setters are: %s, known getters are: %s", getterOrSetterName,
                                       setters.keySet(), getters.keySet());
            throw new IntrospectionException(msg);
        }
        return m.getReturnType();
    }

    public Beanspector<T> swap(T newobject) throws Exception {
        if (newobject == null) {
            throw new IllegalArgumentException("newobject is null");
        }
        tobj = newobject;
        return this;
    }

    public Beanspector<T> instantiate() throws Exception {
        tobj = tclass.newInstance();
        return this;
    }

    public Beanspector<T> setValue(String setterName, Object value) throws Throwable {
        Method m = setters.get(setterName);
        if (m == null) {
            String msg = String.format("Setter '%s' not found, " + "known setters are: %s", setterName,
                                       setters.keySet());
            throw new IntrospectionException(msg);
        }
        setValue(m, value);
        return this;
    }

    public Beanspector<T> setValue(Map<String, Object> settersWithValues) throws Throwable {
        for (Map.Entry<String, Object> entry : settersWithValues.entrySet()) {
            setValue(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public Beanspector<T> setValue(Method setter, Object value) throws Throwable {
        Class<?> paramType = setter.getParameterTypes()[0];
        try {
            setter.invoke(tobj, value);
            return this;
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (IllegalArgumentException e) {
            String msg = String.format("; setter parameter type: %s, set value type: %s",
                                       paramType.getName(), value.getClass().getName());
            throw new IllegalArgumentException(e.getMessage() + msg);
        } catch (Exception e) {
            throw e;
        }
    }

    public Object getValue(String getterName) throws Throwable {
        return getValue(getters.get(getterName));
    }

    public Object getValue(Method getter) throws Throwable {
        try {
            return getter.invoke(tobj);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (Exception e) {
            throw e;
        }
    }

    private boolean isGetter(Method m) {
        return m.getParameterTypes().length == 0
               && (m.getName().startsWith("get") || m.getName().startsWith("is"));
    }

    private String getPropertyName(Method m) {
        // at this point the method is either getter or setter
        String result = m.getName().toLowerCase();

        if (result.startsWith("is")) {
            result = result.substring(2, result.length());
        } else {
            result = result.substring(3, result.length());
        }
        return result;

    }

    private boolean isSetter(Method m) {
        return m.getReturnType().equals(void.class) && m.getParameterTypes().length == 1
               && (m.getName().startsWith("set") || m.getName().startsWith("is"));
    }

    

}
