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
package org.apache.cxf.microprofile.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;

final class Validator {
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(Validator.class);

    private Validator() {
    }

    
    public static void checkValid(Class<?> userType) throws RestClientDefinitionException {
        if (!userType.isInterface()) {
            throwException("VALIDATION_NOT_AN_INTERFACE", userType);
        }
        Method[] methods = userType.getMethods();
        checkMethodsForMultipleHTTPMethodAnnotations(methods);
        checkMethodsForInvalidURITemplates(userType, methods);
    }

    private static void checkMethodsForMultipleHTTPMethodAnnotations(Method[] clientMethods)
        throws RestClientDefinitionException {

        Map<String, Class<? extends Annotation>> httpMethods = new HashMap<>();
        for (Method method : clientMethods) {

            for (Annotation anno : method.getAnnotations()) {
                Class<? extends Annotation> annoClass = anno.annotationType();
                HttpMethod verb = annoClass.getAnnotation(HttpMethod.class);
                if (verb != null) {
                    httpMethods.put(verb.value(), annoClass);
                }
            }
            if (httpMethods.size() > 1) {
                throwException("VALIDATION_METHOD_WITH_MULTIPLE_VERBS", method, httpMethods.values());
            }
            httpMethods.clear();
        }

    }

    private static void checkMethodsForInvalidURITemplates(Class<?> userType, Method[] methods) 
        throws RestClientDefinitionException {

        Path classPathAnno = userType.getAnnotation(Path.class);
        
        final Set<String> classLevelVariables = new HashSet<>();
        URITemplate classTemplate = null;
        if (classPathAnno != null) {
            classTemplate = new URITemplate(classPathAnno.value());
            classLevelVariables.addAll(classTemplate.getVariables());
        }
        URITemplate template;
        for (Method method : methods) {
            
            Path methodPathAnno = method.getAnnotation(Path.class);
            if (methodPathAnno != null) {
                template = classPathAnno == null ? new URITemplate(methodPathAnno.value()) 
                    : new URITemplate(classPathAnno.value() + "/" + methodPathAnno.value());
            } else {
                template = classTemplate;
            }
            if (template == null) {
                continue;
            }
            Set<String> allVariables = new HashSet<>(template.getVariables());
            if (!allVariables.isEmpty()) {
                Map<String, String> paramMap = new HashMap<>();
                for (Parameter p : method.getParameters()) {
                    PathParam pathParam = p.getAnnotation(PathParam.class);
                    if (pathParam != null) {
                        paramMap.put(pathParam.value(), "x");
                    }
                }
                try {
                    template.substitute(paramMap, Collections.<String>emptySet(), false);
                } catch (IllegalArgumentException ex) {
                    throwException("VALIDATION_UNRESOLVED_PATH_PARAMS", userType, method);
                }
            } else {
                List<String> foundParams = new ArrayList<>();
                for (Parameter p : method.getParameters()) {
                    PathParam pathParam = p.getAnnotation(PathParam.class);
                    if (pathParam != null) {
                        foundParams.add(pathParam.value());
                    }
                }
                if (!foundParams.isEmpty()) {
                    throwException("VALIDATION_EXTRA_PATH_PARAMS", userType, method);
                }
            }
        }
    }

    private static void throwException(String msgKey, Object...msgParams) throws RestClientDefinitionException {
        Message msg = new Message(msgKey, BUNDLE, msgParams);
        throw new RestClientDefinitionException(msg.toString());
    }

}
