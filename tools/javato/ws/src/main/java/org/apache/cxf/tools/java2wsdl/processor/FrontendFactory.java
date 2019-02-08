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

package org.apache.cxf.tools.java2wsdl.processor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.WebServiceProvider;

import org.apache.cxf.tools.java2wsdl.processor.internal.jaxws.WrapperUtil;
import org.apache.cxf.tools.util.AnnotationUtil;

public final class FrontendFactory {
    private Class<?> serviceClass;
    private List<Method> wsMethods;

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation>[] annotations
        = new Class[] {SOAPBinding.class,
                       WebService.class,
                       WebServiceProvider.class};

    private static final class FrontendFactoryHolder {
        private static final FrontendFactory INSTANCE =
            new FrontendFactory();
    }

    public enum Style {
        Jaxws,
        Simple
    }

    private FrontendFactory() {
    }

    public static FrontendFactory getInstance() {
        return FrontendFactoryHolder.INSTANCE;
    }

    private boolean isJaxws() {
        if (serviceClass == null) {
            return true;
        }
        for (Class<? extends Annotation> annotation : annotations) {
            if (serviceClass.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return isJAXWSAnnotationExists();
    }

    private boolean isJAXWSAnnotationExists() {
        for (Method method : wsMethods) {
            if (WrapperUtil.isWrapperClassExists(method)) {
                return true;
            }
            WebMethod m = AnnotationUtil.getPrivMethodAnnotation(method, WebMethod.class);
            if (m != null) {
                return true;
            }
            WebResult res = AnnotationUtil.getPrivMethodAnnotation(method, WebResult.class);
            if (res != null) {
                return true;
            }
        }
        return false;
    }

    private List<Method> getWSMethods() {
        List<Method> methods = new ArrayList<>();
        for (Method method : serviceClass.getMethods()) {
            if (method.getDeclaringClass().equals(Object.class)
                || !Modifier.isPublic(method.getModifiers())
                || isExcluced(method)) {
                continue;
            }
            methods.add(method);
        }
        return methods;
    }

    private boolean isExcluced(Method method) {
        WebMethod webMethod = AnnotationUtil.getPrivMethodAnnotation(method, WebMethod.class);
        return webMethod != null && webMethod.exclude();
    }

    public Style discoverStyle() {
        if (isJaxws()) {
            return Style.Jaxws;
        }
        return Style.Simple;
    }

    public void setServiceClass(Class<?> c) {
        this.serviceClass = c;
        if (c != null) {
            this.wsMethods = getWSMethods();
        }
    }
}
