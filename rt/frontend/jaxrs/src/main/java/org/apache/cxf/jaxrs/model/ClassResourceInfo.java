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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;
import javax.ws.rs.ConsumeMime;
import javax.ws.rs.Path;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.Context;

import org.apache.cxf.jaxrs.JAXRSUtils;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;

public class ClassResourceInfo {
    
    private boolean root;
    private Class<?> resourceClass;
    private Class<?> serviceClass;
    private URITemplate uriTemplate;
    private MethodDispatcher methodDispatcher;
    private ResourceProvider resourceProvider;
    private List<ClassResourceInfo> subClassResourceInfo = new ArrayList<ClassResourceInfo>();
    private List<Field> httpContexts;
    private List<Field> resources;

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
        resourceClass = theResourceClass;
        serviceClass = theServiceClass;
        root = theRoot;
        initHttpContexts();
        initResources();
    }

    public boolean isRoot() {
        return root;
    }
    
    public Class<?> getResourceClass() {
        return resourceClass;
    }
    
    public Class<?> getServiceClass() {
        return serviceClass;
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
    
    private void initHttpContexts() {
        if (resourceClass == null || !root) {
            return;
        }
        httpContexts = new ArrayList<Field>();
        Field[] fields = getServiceClass().getDeclaredFields();
        
        for (Field f : fields) {
            Context context = f.getAnnotation(Context.class);
            if (context != null) {
                httpContexts.add(f);               
            }
        }
    }

    private void initResources() {
        if (resourceClass == null || !root) {
            return;
        }
        resources = new ArrayList<Field>();
        Field[] fields = getServiceClass().getDeclaredFields();
        
        for (Field f : fields) {
            Resource resource = f.getAnnotation(Resource.class);
            if (resource != null) {
                resources.add(f);               
            }
        }
    }

    public ProduceMime getProduceMime() {
        return (ProduceMime)JAXRSUtils.getClassAnnotation(getServiceClass(), ProduceMime.class);
    }
    
    public ConsumeMime getConsumeMime() {
        return (ConsumeMime)JAXRSUtils.getClassAnnotation(getServiceClass(), ConsumeMime.class);
    }
    
    public Path getPath() {
        return (Path)JAXRSUtils.getClassAnnotation(getServiceClass(), Path.class);
    }
    
    public List<Field> getHttpContexts() {
        List<Field> ret;
        if (httpContexts != null) {
            ret = Collections.unmodifiableList(httpContexts);
        } else {
            ret = Collections.emptyList();
        }
        return ret;
    }
    
    public List<Field> getResources() {
        List<Field> ret;
        if (resources != null) {
            ret = Collections.unmodifiableList(resources);
        } else {
            ret = Collections.emptyList();
        }
        return ret;
    }
    
    
}
