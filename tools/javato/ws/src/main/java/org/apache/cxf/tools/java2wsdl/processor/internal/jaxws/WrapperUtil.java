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

package org.apache.cxf.tools.java2wsdl.processor.internal.jaxws;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.jws.Oneway;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public final class WrapperUtil {

    private WrapperUtil() {
    }

    public static boolean isWrapperClassExists(Method method) {
        Wrapper requestWrapper = new RequestWrapper();
        requestWrapper.setMethod(method);
        try {
            requestWrapper.getWrapperClass();
            boolean isOneWay = method.isAnnotationPresent(Oneway.class);
            if (!isOneWay) {
                Wrapper responseWrapper = new ResponseWrapper();
                responseWrapper.setMethod(method);
                responseWrapper.getWrapperClass();
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static List<Annotation> getJaxbAnnotations(Method method) {
        List<Annotation> jaxbAnnotation = new ArrayList<Annotation>();
        Annotation ann = method.getAnnotation(XmlAttachmentRef.class);
        if (ann != null) {
            jaxbAnnotation.add(ann);
        }
        ann = method.getAnnotation(XmlMimeType.class);
        if (ann != null) {
            jaxbAnnotation.add(ann);
        }
        ann = method.getAnnotation(XmlJavaTypeAdapter.class);
        if (ann != null) {
            jaxbAnnotation.add(ann);
        }
        ann = method.getAnnotation(XmlList.class);
        if (ann != null) {
            jaxbAnnotation.add(ann);
        }
        return jaxbAnnotation;
    }

    public static List<Annotation> getJaxbAnnotations(Method method, int idx) {
        List<Annotation> jaxbAnnotation = new ArrayList<Annotation>();
        Annotation[][] anns = method.getParameterAnnotations();
        for (int i = 0; i < method.getParameterTypes().length; i++) {            
            if (i == idx) {
                for (Annotation ann : anns[i]) {
                    if (ann.annotationType() == XmlAttachmentRef.class
                        || ann.annotationType() == XmlMimeType.class
                        || ann.annotationType() == XmlJavaTypeAdapter.class
                        || ann.annotationType() == XmlList.class) {
                        jaxbAnnotation.add(ann);
                    }                   
                }
            }
        }
        return jaxbAnnotation;
    }
}
