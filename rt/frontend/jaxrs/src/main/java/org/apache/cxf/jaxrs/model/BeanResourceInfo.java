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

package org.apache.cxf.jaxrs.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;

public abstract class BeanResourceInfo extends AbstractResourceInfo {
    protected List<Field> paramFields;
    protected List<Method> paramMethods;
    private boolean paramsAvailable;

    protected BeanResourceInfo(Bus bus) {
        super(bus);
    }

    protected BeanResourceInfo(Class<?> resourceClass, Class<?> serviceClass, boolean isRoot, Bus bus) {
        this(resourceClass, serviceClass, isRoot, true, bus);
    }

    protected BeanResourceInfo(Class<?> resourceClass, Class<?> serviceClass,
                               boolean isRoot, boolean checkContexts, Bus bus) {
        super(resourceClass, serviceClass, isRoot, checkContexts, bus);
        if (checkContexts && resourceClass != null) {
            setParamField(serviceClass);
            setParamMethods(serviceClass);
        }
    }

    public boolean paramsAvailable() {
        return paramsAvailable;
    }

    private void setParamField(Class<?> cls) {
        if (Object.class == cls || cls == null) {
            return;
        }
        for (Field f : ReflectionUtil.getDeclaredFields(cls)) {
            for (Annotation a : f.getAnnotations()) {
                if (AnnotationUtils.isParamAnnotationClass(a.annotationType())) {
                    if (paramFields == null) {
                        paramFields = new ArrayList<>();
                    }
                    paramsAvailable = true;
                    paramFields.add(f);
                }
            }
        }
        setParamField(cls.getSuperclass());
    }

    private void setParamMethods(Class<?> cls) {

        for (Method m : cls.getMethods()) {

            if (!m.getName().startsWith("set") || m.getParameterTypes().length != 1) {
                continue;
            }
            for (Annotation a : m.getAnnotations()) {
                if (AnnotationUtils.isParamAnnotationClass(a.annotationType())) {
                    addParamMethod(m);
                    break;
                }
            }
        }
        Class<?>[] interfaces = cls.getInterfaces();
        for (Class<?> i : interfaces) {
            setParamMethods(i);
        }
    }

    private void addParamMethod(Method m) {
        if (paramMethods == null) {
            paramMethods = new ArrayList<>();
        }
        paramsAvailable = true;
        paramMethods.add(m);
    }

    public List<Method> getParameterMethods() {
        return paramMethods == null ? Collections.<Method>emptyList()
                                    : Collections.unmodifiableList(paramMethods);
    }

    public List<Field> getParameterFields() {
        return paramFields == null ? Collections.<Field>emptyList()
                                    : Collections.unmodifiableList(paramFields);
    }
}