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
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;

final class Validator {
    private static final Logger LOG = LogUtils.getL7dLogger(Validator.class);
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
        checkForInvalidClientHeaderParams(userType, methods);
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

        URITemplate classTemplate = null;
        if (classPathAnno != null) {
            classTemplate = new URITemplate(classPathAnno.value());
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

            List<String> foundParams = new ArrayList<>();
            for (Parameter p : method.getParameters()) {
                PathParam pathParam = p.getAnnotation(PathParam.class);
                if (pathParam != null) {
                    foundParams.add(pathParam.value());
                }
            }

            Set<String> allVariables = new HashSet<>(template.getVariables());
            if (!allVariables.isEmpty()) {
                for (String variable : template.getVariables()) {
                    if (!foundParams.contains(variable)) {
                        throwException("VALIDATION_UNRESOLVED_PATH_PARAMS", userType, method);
                    }
                }
            } else if (!foundParams.isEmpty()) {
                throwException("VALIDATION_EXTRA_PATH_PARAMS", userType, method);
            }
        }
    }

    private static void checkForInvalidClientHeaderParams(Class<?> userType, Method[] methods) {
        ClientHeaderParam[] interfaceAnnotations = userType.getAnnotationsByType(ClientHeaderParam.class);
        checkClientHeaderParamAnnotation(interfaceAnnotations, userType, methods);
        for (Method method : methods) {
            ClientHeaderParam[] methodAnnotations = method.getAnnotationsByType(ClientHeaderParam.class);
            checkClientHeaderParamAnnotation(methodAnnotations, userType, methods);
        }
    }

    private static void checkClientHeaderParamAnnotation(ClientHeaderParam[] annos, Class<?> userType,
                                                         Method[] methods) {
        Set<String> headerNames = new HashSet<>();
        for (ClientHeaderParam anno : annos) {
            String name = anno.name();
            if (headerNames.contains(name)) {
                throwException("CLIENT_HEADER_MULTIPLE_SAME_HEADER_NAMES", userType.getName());
            }
            headerNames.add(name);

            if (name == null || "".equals(name)) {
                throwException("CLIENT_HEADER_NO_NAME", userType.getName());
            }

            String[] values = anno.value();

            for (String value : values) {
                if (StringUtils.isEmpty(value)) {
                    throwException("CLIENT_HEADER_NO_VALUE", userType.getName());
                }

                if (value.startsWith("{") && value.endsWith("}")) {
                    if (values.length > 1) {
                        throwException("CLIENT_HEADER_MULTI_METHOD", userType.getName());
                    }
                    String computeValue = value.substring(1, value.length() - 1);
                    boolean usingOtherClass = false;
                    if (computeValue.contains(".")) {
                        usingOtherClass = true;
                        String className = computeValue.substring(0, computeValue.lastIndexOf('.'));
                        computeValue = computeValue.substring(computeValue.lastIndexOf('.') + 1);
                        try {
                            Class<?> computeClass = ClassLoaderUtils.loadClass(className, userType);
                            methods = Arrays.stream(computeClass.getDeclaredMethods())
                                                                .filter(m -> {
                                                                    int i = m.getModifiers();
                                                                    return Modifier.isPublic(i)
                                                                        && Modifier.isStatic(i);
                                                                })
                                                                .toArray(Method[]::new);
                        } catch (ClassNotFoundException ex) {
                            if (LOG.isLoggable(Level.FINEST)) {
                                LOG.log(Level.FINEST, "Unable to load specified compute method class", ex);
                            }
                            throwException("CLIENT_HEADER_COMPUTE_CLASS_NOT_FOUND", userType.getName(), ex);
                        }

                    }
                    boolean foundMatchingMethod = false;
                    for (Method method : methods) {
                        Class<?> returnType = method.getReturnType();
                        if ((usingOtherClass || method.isDefault())
                            && (String.class.equals(returnType) || String[].class.equals(returnType))
                            && computeValue.equals(method.getName())) {

                            Class<?>[] args = method.getParameterTypes();
                            if (args.length == 0 || (args.length == 1 && String.class.equals(args[0]))) {
                                foundMatchingMethod = true;
                                break;
                            }

                        }
                    }
                    if (!foundMatchingMethod) {
                        throwException("CLIENT_HEADER_INVALID_COMPUTE_METHOD",
                                       userType.getName(),
                                       computeValue);
                    }
                }
            }
        }
    }

    private static void throwException(String msgKey, Object...msgParams) throws RestClientDefinitionException {
        Message msg = new Message(msgKey, BUNDLE, msgParams);
        throw new RestClientDefinitionException(msg.toString());
    }

}
