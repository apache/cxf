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

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

public class OperationResourceInfo {
    private URITemplate uriTemplate;
    private ClassResourceInfo classResourceInfo;
    private Method methodToInvoke;
    private Method annotatedMethod;
    private String httpMethod;
    private List<MediaType> produceMimes;
    private List<MediaType> consumeMimes;
    private boolean encoded;
    private String defaultParamValue;
    private List<Parameter> parameters;

    public OperationResourceInfo(Method mInvoke, ClassResourceInfo cri) {
        this(mInvoke, mInvoke, cri);
    }
    
    public OperationResourceInfo(Method mInvoke, Method mAnnotated, ClassResourceInfo cri) {
        methodToInvoke = mInvoke;
        annotatedMethod = mAnnotated;
        if (mAnnotated != null) {
            parameters = ResourceUtils.getParameters(mAnnotated);
        }
        classResourceInfo = cri;
        checkMediaTypes();
        checkEncoded();
        checkDefaultParameterValue();
    }
    
    public OperationResourceInfo(Method m, 
                                 ClassResourceInfo cri,
                                 URITemplate template,
                                 String httpVerb,
                                 String consumeMediaTypes,
                                 String produceMediaTypes,
                                 List<Parameter> params) {
        methodToInvoke = m;
        annotatedMethod = null;
        classResourceInfo = cri;
        uriTemplate = template;
        httpMethod = httpVerb;
        consumeMimes = JAXRSUtils.sortMediaTypes(consumeMediaTypes);
        produceMimes = JAXRSUtils.sortMediaTypes(produceMediaTypes);
        parameters = params;
    }

    public List<Parameter> getParameters() {
        return parameters;
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

    public Method getMethodToInvoke() {
        return methodToInvoke;
    }
    
    public Method getAnnotatedMethod() {
        return annotatedMethod;
    }

    public void setMethodToInvoke(Method m) {
        methodToInvoke = m;
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
        
        return produceMimes;
    }
    
    public List<MediaType> getConsumeTypes() {
        
        return consumeMimes;
    }
    
    private void checkMediaTypes() {
        Consumes cm = 
            (Consumes)AnnotationUtils.getMethodAnnotation(annotatedMethod, Consumes.class);
        if (cm != null) {
            consumeMimes = JAXRSUtils.sortMediaTypes(JAXRSUtils.getMediaTypes(cm.value()));
        } else if (classResourceInfo != null) {
            consumeMimes = JAXRSUtils.sortMediaTypes(
                               JAXRSUtils.getConsumeTypes(classResourceInfo.getConsumeMime()));
        }
        
        Produces pm = 
            (Produces)AnnotationUtils.getMethodAnnotation(annotatedMethod, Produces.class);
        if (pm != null) {
            produceMimes = JAXRSUtils.sortMediaTypes(JAXRSUtils.getMediaTypes(pm.value()));
        } else if (classResourceInfo != null) {
            produceMimes = JAXRSUtils.sortMediaTypes(
                               JAXRSUtils.getProduceTypes(classResourceInfo.getProduceMime()));
        }
    }
    
    public boolean isEncodedEnabled() {
        return encoded;
    }
    
    public String getDefaultParameterValue() {
        return defaultParamValue;
    }
    
    private void checkEncoded() {
        encoded = AnnotationUtils.getMethodAnnotation(annotatedMethod, 
                                            Encoded.class) != null;
        if (!encoded && classResourceInfo != null) {
            encoded = AnnotationUtils.getClassAnnotation(classResourceInfo.getServiceClass(), 
                                                          Encoded.class) != null;
        }
    }
    
    private void checkDefaultParameterValue() {
        DefaultValue dv = (DefaultValue)AnnotationUtils.getMethodAnnotation(annotatedMethod, 
                                            DefaultValue.class);
        if (dv == null && classResourceInfo != null) {
            dv = (DefaultValue)AnnotationUtils.getClassAnnotation(
                                         classResourceInfo.getServiceClass(), 
                                         DefaultValue.class);
        }
        if (dv != null) {
            defaultParamValue = dv.value();
        }
    }
}
