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
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.ext.Oneway;
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
    private boolean oneway;
    private boolean async;
    private Set<String> nameBindings = new LinkedHashSet<>();
    private Class<?>[] actualInParamTypes;
    private Type[] actualInGenericParamTypes;
    private Annotation[][] actualInParamAnnotations;
    private Annotation[] actualOutParamAnnotations;

    public OperationResourceInfo(Method mInvoke, ClassResourceInfo cri) {
        this(mInvoke, mInvoke, cri);
    }

    OperationResourceInfo(OperationResourceInfo ori, ClassResourceInfo cri) {
        this.uriTemplate = ori.uriTemplate;
        this.methodToInvoke = ori.methodToInvoke;
        this.annotatedMethod = ori.annotatedMethod;
        this.httpMethod = ori.httpMethod;
        this.produceMimes = ori.produceMimes;
        this.consumeMimes = ori.consumeMimes;
        this.encoded = ori.encoded;
        this.defaultParamValue = ori.defaultParamValue;
        this.parameters = ori.parameters;
        this.oneway = ori.oneway;
        this.async = ori.async;
        this.classResourceInfo = cri;
        this.nameBindings = ori.nameBindings;
        initActualMethodProperties();
    }

    public OperationResourceInfo(Method mInvoke, Method mAnnotated, ClassResourceInfo cri) {
        methodToInvoke = mInvoke;
        annotatedMethod = mAnnotated;
        // Combine the name bindings from annotated method and method to invoke
        nameBindings.addAll(AnnotationUtils.getNameBindings(mInvoke.getAnnotations()));
        if (mAnnotated != null) {
            parameters = ResourceUtils.getParameters(mAnnotated);
            nameBindings.addAll(AnnotationUtils.getNameBindings(mAnnotated.getAnnotations()));
        }
        classResourceInfo = cri;
        checkMediaTypes(null, null);
        checkEncoded();
        checkDefaultParameterValue();
        checkOneway();
        checkSuspended();
        initActualMethodProperties();
    }

    //CHECKSTYLE:OFF
    public OperationResourceInfo(Method m,
                                 ClassResourceInfo cri,
                                 URITemplate template,
                                 String httpVerb,
                                 String consumeMediaTypes,
                                 String produceMediaTypes,
                                 List<Parameter> params,
                                 boolean oneway) {
    //CHECKSTYLE:ON
        methodToInvoke = m;
        annotatedMethod = null;
        classResourceInfo = cri;
        uriTemplate = template;
        httpMethod = httpVerb;
        checkMediaTypes(consumeMediaTypes, produceMediaTypes);
        parameters = params;
        this.oneway = oneway;
        initActualMethodProperties();
    }

    private void initActualMethodProperties() {
        Method actualMethod = annotatedMethod == null ? methodToInvoke : annotatedMethod;
        actualInParamTypes = actualMethod.getParameterTypes();
        actualInGenericParamTypes = actualMethod.getGenericParameterTypes();
        actualInParamAnnotations = actualMethod.getParameterAnnotations();

        // out annotations
        Annotation[] invokedAnns = methodToInvoke.getAnnotations();
        if (methodToInvoke != annotatedMethod && annotatedMethod != null) {
            Annotation[] superAnns = annotatedMethod.getAnnotations();
            if (invokedAnns.length > 0) {
                Annotation[] merged = new Annotation[superAnns.length + invokedAnns.length];
                System.arraycopy(superAnns, 0, merged, 0, superAnns.length);
                System.arraycopy(invokedAnns, 0, merged, superAnns.length, invokedAnns.length);
                actualOutParamAnnotations = merged;
            } else {
                actualOutParamAnnotations = superAnns;
            }
        } else {
            actualOutParamAnnotations = invokedAnns;
        }
    }

    public void addNameBindings(List<String> names) {
        nameBindings.addAll(names);
    }

    public Set<String> getNameBindings() {
        Set<String> criNames = classResourceInfo.getNameBindings();
        if (criNames.isEmpty()) {
            return nameBindings;
        }
        Set<String> all = new LinkedHashSet<>(criNames);
        all.addAll(nameBindings);
        return all;
    }

    private void checkOneway() {
        if (annotatedMethod != null) {
            oneway = AnnotationUtils.getAnnotation(annotatedMethod.getAnnotations(), Oneway.class) != null;
        }
    }
    private void checkSuspended() {
        if (annotatedMethod != null) {
            for (Annotation[] anns : annotatedMethod.getParameterAnnotations()) {
                if (AnnotationUtils.getAnnotation(anns, Suspended.class) != null) {
                    async = true;
                    break;
                }
            }
        }
    }

    public boolean isOneway() {
        return oneway;
    }
    public boolean isAsync() {
        return async;
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
        return httpMethod == null;
    }


    public List<MediaType> getProduceTypes() {

        return produceMimes;
    }

    public List<MediaType> getConsumeTypes() {

        return consumeMimes;
    }

    private void checkMediaTypes(String consumeMediaTypes,
                                 String produceMediaTypes) {
        if (consumeMediaTypes != null) {
            consumeMimes = JAXRSUtils.sortMediaTypes(consumeMediaTypes, null);
        } else {
            Consumes cm =
                AnnotationUtils.getMethodAnnotation(annotatedMethod, Consumes.class);
            if (cm != null) {
                consumeMimes = JAXRSUtils.sortMediaTypes(JAXRSUtils.getMediaTypes(cm.value()), null);
            } else if (classResourceInfo != null) {
                consumeMimes = JAXRSUtils.sortMediaTypes(classResourceInfo.getConsumeMime(), null);
            }
        }
        if (produceMediaTypes != null) {
            produceMimes = JAXRSUtils.sortMediaTypes(produceMediaTypes, JAXRSUtils.MEDIA_TYPE_QS_PARAM);
        } else {
            Produces pm =
                AnnotationUtils.getMethodAnnotation(annotatedMethod, Produces.class);
            if (pm != null) {
                produceMimes = JAXRSUtils.sortMediaTypes(JAXRSUtils.getMediaTypes(pm.value()),
                                                         JAXRSUtils.MEDIA_TYPE_QS_PARAM);
            } else if (classResourceInfo != null) {
                produceMimes = JAXRSUtils.sortMediaTypes(classResourceInfo.getProduceMime(),
                                                         JAXRSUtils.MEDIA_TYPE_QS_PARAM);
            }
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
        DefaultValue dv = AnnotationUtils.getMethodAnnotation(annotatedMethod,
                                            DefaultValue.class);
        if (dv == null && classResourceInfo != null) {
            dv = AnnotationUtils.getClassAnnotation(
                                         classResourceInfo.getServiceClass(),
                                         DefaultValue.class);
        }
        if (dv != null) {
            defaultParamValue = dv.value();
        }
    }
    public Annotation[][] getInParameterAnnotations() {
        return actualInParamAnnotations;
    }
    public Type[] getInGenericParameterTypes() {
        return actualInGenericParamTypes;
    }
    public Class<?>[] getInParameterTypes() {
        return actualInParamTypes;
    }
    public Annotation[] getOutAnnotations() {
        return actualOutParamAnnotations;
    }

}
