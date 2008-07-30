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

import javax.ws.rs.ConsumeMime;
import javax.ws.rs.Path;
import javax.ws.rs.ProduceMime;

import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;

public class ClassResourceInfo extends AbstractResourceInfo {
    
    private URITemplate uriTemplate;
    private MethodDispatcher methodDispatcher;
    private ResourceProvider resourceProvider;
    private List<ClassResourceInfo> subClassResourceInfo = new ArrayList<ClassResourceInfo>();
   
    private List<Field> paramFields;
    private List<Method> paramMethods;
    
    public ClassResourceInfo(Class<?> theResourceClass) {
        this(theResourceClass, false);
    }
    
    public ClassResourceInfo(Class<?> theResourceClass, boolean theRoot) {
        this(theResourceClass, theResourceClass, theRoot);
    }
    
    public ClassResourceInfo(Class<?> theResourceClass, Class<?> theServiceClass) {
        this(theResourceClass, theServiceClass, false);
    }
    
    public ClassResourceInfo(Class<?> theResourceClass, Class<?> theServiceClass, boolean theRoot) {
        super(theResourceClass, theServiceClass, theRoot);
        if (theRoot) {
            initParamFields();
            initParamMethods();
        }
    }
    
    private void initParamFields() {
        if (getResourceClass() == null || !isRoot()) {
            return;
        }
        
        
        for (Field f : getServiceClass().getDeclaredFields()) {
            for (Annotation a : f.getAnnotations()) {
                if (AnnotationUtils.isParamAnnotationClass(a.annotationType())) {
                    if (paramFields == null) {
                        paramFields = new ArrayList<Field>();
                    }
                    paramFields.add(f);
                }
            }
        }
    }
    
    private void initParamMethods() {
        
        for (Method m : getServiceClass().getMethods()) {
        
            if (!m.getName().startsWith("set") || m.getParameterTypes().length != 1) {
                continue;
            }
            for (Annotation a : m.getAnnotations()) {
                if (AnnotationUtils.isParamAnnotationClass(a.annotationType())) {
                    checkParamMethod(m, AnnotationUtils.getAnnotationValue(a));
                    break;
                }
            }
        }
    }

    public URITemplate getURITemplate() {
        return uriTemplate;
    }

    public void setURITemplate(URITemplate u) {
        uriTemplate = u;
    }

    public MethodDispatcher getMethodDispatcher() {
        return methodDispatcher;
    }

    public void setMethodDispatcher(MethodDispatcher md) {
        methodDispatcher = md;
    }

    public boolean hasSubResources() {
        return !subClassResourceInfo.isEmpty();
    }
    
    public void addSubClassResourceInfo(ClassResourceInfo cri) {
        subClassResourceInfo.add(cri);
    }
    
    public List<ClassResourceInfo> getSubClassResourceInfo() {
        return subClassResourceInfo;
    }
    
    public ResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    public void setResourceProvider(ResourceProvider rp) {
        resourceProvider = rp;
    }
    
    public ProduceMime getProduceMime() {
        return (ProduceMime)AnnotationUtils.getClassAnnotation(getServiceClass(), ProduceMime.class);
    }
    
    public ConsumeMime getConsumeMime() {
        return (ConsumeMime)AnnotationUtils.getClassAnnotation(getServiceClass(), ConsumeMime.class);
    }
    
    public Path getPath() {
        return (Path)AnnotationUtils.getClassAnnotation(getServiceClass(), Path.class);
    }
    
    private void addParamMethod(Method m) {
        if (paramMethods == null) {
            paramMethods = new ArrayList<Method>();
        }
        paramMethods.add(m);
    }
    
    @SuppressWarnings("unchecked")
    public List<Method> getParameterMethods() {
        return paramMethods == null ? Collections.EMPTY_LIST 
                                    : Collections.unmodifiableList(paramMethods);
    }
    
    @SuppressWarnings("unchecked")
    public List<Field> getParameterFields() {
        return paramFields == null ? Collections.EMPTY_LIST 
                                    : Collections.unmodifiableList(paramFields);
    }
    
    private void checkParamMethod(Method m, String value) {
        if (m.getName().equalsIgnoreCase("set" + value)) {
            addParamMethod(m);
        }
    }
    
    @Override
    public boolean isSingleton() {
        return resourceProvider instanceof SingletonResourceProvider;
    }
}
