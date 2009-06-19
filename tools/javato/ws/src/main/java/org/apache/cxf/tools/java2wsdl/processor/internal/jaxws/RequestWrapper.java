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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.jws.WebParam;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.tools.common.model.JavaField;
import org.apache.cxf.tools.java2wsdl.generator.wsdl11.model.WrapperBeanClass;

public class RequestWrapper extends Wrapper {
    @Override
    public void setOperationInfo(final OperationInfo op) {
        super.setOperationInfo(op);
        setName(op.getInput().getMessageParts().get(0).getElementQName());
        setClassName((String)op.getInput().getMessageParts().get(0).getProperty("REQUEST.WRAPPER.CLASSNAME"));
    }

    @Override
    public boolean isWrapperAbsent(final Method method) {
        javax.xml.ws.RequestWrapper reqWrapper = method.getAnnotation(javax.xml.ws.RequestWrapper.class);
        return getClassName() == null && (reqWrapper == null || StringUtils.isEmpty(reqWrapper.className()));
    }

    public String getWrapperTns(Method method) {
        javax.xml.ws.RequestWrapper reqWrapper = method.getAnnotation(javax.xml.ws.RequestWrapper.class);
        if (reqWrapper != null) {
            return reqWrapper.targetNamespace();
        }
        return null;
    }

    @Override
    protected List<JavaField> buildFields() {
        return buildFields(getMethod(), getOperationInfo().getUnwrappedOperation().getInput());
    }    
    
    protected List<JavaField> buildFields(final Method method, final MessageInfo message) {
        List<JavaField> fields = new ArrayList<JavaField>();

        final Type[] paramClasses = method.getGenericParameterTypes();
        final Annotation[][] paramAnnotations = method.getParameterAnnotations();

        for (MessagePartInfo mpi : message.getMessageParts()) {
            int idx = mpi.getIndex();
            String name = mpi.getName().getLocalPart();
            Type t = paramClasses[idx];
            String type = getTypeString(t);

            JavaField field = new JavaField(name, type, "");

            if (paramAnnotations != null 
                && paramAnnotations.length == paramClasses.length) {
                WebParam wParam = getWebParamAnnotation(paramAnnotations[idx]);
                if (wParam != null && !StringUtils.isEmpty(wParam.targetNamespace())) {
                    field.setTargetNamespace(wParam.targetNamespace());
                } else {
                    field.setTargetNamespace("");
                }
            }

            List<Annotation> jaxbAnns = WrapperUtil.getJaxbAnnotations(method, idx);
            field.setJaxbAnnotations(jaxbAnns.toArray(new Annotation[jaxbAnns.size()]));
            fields.add(field);
        }

        return fields;
    }

    private WebParam getWebParamAnnotation(final Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof WebParam) {
                return (WebParam) annotation;
            }
        }
        return null;
    }

    @Override
    public WrapperBeanClass getWrapperBeanClass(final Method method) {
        javax.xml.ws.RequestWrapper reqWrapper = method.getAnnotation(javax.xml.ws.RequestWrapper.class);
        String reqClassName = getClassName();
        String reqNs = null;

        if (reqWrapper != null) {
            reqClassName = reqWrapper.className().length() > 0 ? reqWrapper.className() : reqClassName;
            reqNs = reqWrapper.targetNamespace().length() > 0 ? reqWrapper.targetNamespace() : null;
        }
        if (reqClassName == null) {
            reqClassName = getPackageName(method) + ".jaxws." + StringUtils.capitalize(method.getName());
        }

        WrapperBeanClass jClass = new WrapperBeanClass();
        jClass.setFullClassName(reqClassName);
        jClass.setNamespace(reqNs);
        return jClass;
    }

}
