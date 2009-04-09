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

package org.apache.cxf.jaxrs.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.MethodDispatcher;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;

public final class ResourceUtils {
    
    private static final Logger LOG = LogUtils.getL7dLogger(ResourceUtils.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(ResourceUtils.class);

    
    private ResourceUtils() {
        
    }
    
    public static ClassResourceInfo createClassResourceInfo(
        final Class<?> rClass, final Class<?> sClass, boolean root, boolean enableStatic) {
        ClassResourceInfo cri  = new ClassResourceInfo(rClass, sClass, root, enableStatic);

        if (root) {
            URITemplate t = URITemplate.createTemplate(cri, cri.getPath());
            cri.setURITemplate(t);
        }
        
        evaluateResourceClass(cri, enableStatic);
        return checkMethodDispatcher(cri) ? cri : null;
    }

    private static void evaluateResourceClass(ClassResourceInfo cri, boolean enableStatic) {
        MethodDispatcher md = new MethodDispatcher();
        for (Method m : cri.getServiceClass().getMethods()) {
            
            Method annotatedMethod = AnnotationUtils.getAnnotatedMethod(m);
            
            String httpMethod = AnnotationUtils.getHttpMethodValue(annotatedMethod);
            Path path = (Path)AnnotationUtils.getMethodAnnotation(annotatedMethod, Path.class);
            
            if (httpMethod != null || path != null) {
                md.bind(createOperationInfo(m, annotatedMethod, cri, path, httpMethod), m);
                if (httpMethod == null) {
                    // subresource locator
                    Class<?> subClass = m.getReturnType();
                    if (enableStatic) {
                        ClassResourceInfo subCri = cri.findResource(subClass, subClass);
                        if (subCri == null) {
                            subCri = subClass == cri.getServiceClass() ? cri
                                     : createClassResourceInfo(subClass, subClass, false, enableStatic);
                        }
                        
                        if (subCri != null) {
                            cri.addSubClassResourceInfo(subCri);
                        }
                    }
                }
            }
        }
        cri.setMethodDispatcher(md);
    }
    
    public static Constructor findResourceConstructor(Class<?> resourceClass, boolean perRequest) {
        List<Constructor> cs = new LinkedList<Constructor>();
        for (Constructor c : resourceClass.getConstructors()) {
            Class<?>[] params = c.getParameterTypes();
            Annotation[][] anns = c.getParameterAnnotations();
            boolean match = true;
            for (int i = 0; i < params.length; i++) {
                if (!perRequest) { 
                    if (AnnotationUtils.getAnnotation(anns[i], Context.class) == null
                        || !AnnotationUtils.isContextClass(params[i])) {
                        match = false;
                        break;
                    }
                } else if (!AnnotationUtils.isValidParamAnnotations(anns[i])) {
                    match = false;
                    break;
                }
            }
            if (match) {
                cs.add(c);
            }
        }
        Collections.sort(cs, new Comparator<Constructor>() {

            public int compare(Constructor c1, Constructor c2) {
                int p1 = c1.getParameterTypes().length;
                int p2 = c2.getParameterTypes().length;
                return p1 > p2 ? -1 : p1 < p2 ? 1 : 0;
            }
        
        });
        return cs.size() == 0 ? null : cs.get(0);
    }
    
    private static OperationResourceInfo createOperationInfo(Method m, Method annotatedMethod, 
                                                      ClassResourceInfo cri, Path path, String httpMethod) {
        OperationResourceInfo ori = new OperationResourceInfo(m, cri);
        URITemplate t = 
            URITemplate.createTemplate(cri, path);
        ori.setURITemplate(t);
        ori.setHttpMethod(httpMethod);
        ori.setAnnotatedMethod(annotatedMethod);
        return ori;
    }
    
       
    private static boolean checkMethodDispatcher(ClassResourceInfo cr) {
        if (cr.getMethodDispatcher().getOperationResourceInfos().isEmpty()) {
            LOG.warning(new org.apache.cxf.common.i18n.Message("NO_RESOURCE_OP_EXC", 
                                                               BUNDLE, 
                                                               cr.getServiceClass().getName()).toString());
            return false;
        }
        return true;
    }
}
