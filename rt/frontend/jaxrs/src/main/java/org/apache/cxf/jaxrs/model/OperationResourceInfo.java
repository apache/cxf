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

import java.lang.reflect.Method;
import java.util.List;

import javax.ws.rs.ConsumeMime;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.JAXRSUtils;

public class OperationResourceInfo {
    private URITemplate uriTemplate;
    private ClassResourceInfo classResourceInfo;
    private Method method;
    private String httpMethod;
   

    public OperationResourceInfo(Method m, ClassResourceInfo cri) {
        method = m;
        classResourceInfo = cri;
    }

    public URITemplate getURITemplate() {
        return uriTemplate;
    }

    public void setURITemplate(URITemplate u) {
        uriTemplate = u;
    }

    public ClassResourceInfo getClassResourceInfo() {
        return classResourceInfo;
    }

    public void setClassResourceInfo(ClassResourceInfo c) {
        classResourceInfo = c;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method m) {
        method = m;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String m) {
        httpMethod = m;
    }
    
    public boolean isSubResourceLocator() {
        return httpMethod == null ? true : false;
    }

    
    public List<MediaType> getProduceTypes() {
        
        // this needs to be calculated on init
        ProduceMime pm = 
            (ProduceMime)JAXRSUtils.getMethodAnnotation(method, ProduceMime.class);
        if (pm != null) {
            return JAXRSUtils.getMediaTypes(pm.value());
        }
        
        return JAXRSUtils.getProduceTypes(classResourceInfo.getProduceMime());
    }
    
    public List<MediaType> getConsumeTypes() {
        
        // this needs to be calculated on init
        ConsumeMime pm = 
            (ConsumeMime)JAXRSUtils.getMethodAnnotation(method, ConsumeMime.class);
        if (pm != null) {
            return JAXRSUtils.getMediaTypes(pm.value());
        }
        
        return JAXRSUtils.getConsumeTypes(classResourceInfo.getConsumeMime());
    }
}
