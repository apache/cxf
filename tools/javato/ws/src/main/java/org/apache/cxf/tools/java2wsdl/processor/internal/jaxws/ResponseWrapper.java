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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.jws.WebParam;
import javax.xml.ws.Holder;

import org.apache.cxf.common.util.CollectionUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.tools.common.model.JavaField;
import org.apache.cxf.tools.java2wsdl.generator.wsdl11.model.WrapperBeanClass;


public final class ResponseWrapper extends Wrapper {
    @Override
    public void setOperationInfo(final OperationInfo op) {
        super.setOperationInfo(op);
        setName(op.getOutput().getMessageParts().get(0).getElementQName());
        setClassName((String)op.getOutput().getMessageParts().get(0)
                         .getProperty("RESPONSE.WRAPPER.CLASSNAME"));
    }

    @Override
    public boolean isWrapperAbsent(final Method method) {
        javax.xml.ws.ResponseWrapper resWrapper = method.getAnnotation(javax.xml.ws.ResponseWrapper.class);
        return getClassName() == null && (resWrapper == null || StringUtils.isEmpty(resWrapper.className()));
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
        return buildFields(getMethod(), getOperationInfo().getUnwrappedOperation().getOutput());
    }

    protected List<JavaField> buildFields(final Method method, final MessageInfo message) {
        List<JavaField> fields = new ArrayList<JavaField>();

        final Class<?> returnType = method.getReturnType();
        JavaField field = new JavaField();
        if (CollectionUtils.isEmpty(message.getMessageParts())) {
            return fields;
        }
        MessagePartInfo part = message.getMessageParts().get(0);

        field.setName(part.getName().getLocalPart());

        boolean hasReturnType = false;

        if (!returnType.isAssignableFrom(void.class)) {
            hasReturnType = true;
            String type = getTypeString(method.getGenericReturnType());
            List<Annotation> jaxbAnns = WrapperUtil.getJaxbAnnotations(method);
            field.setType(type);
            field.setJaxbAnnotations(jaxbAnns.toArray(new Annotation[jaxbAnns.size()]));
            field.setTargetNamespace("");

        }
        fields.add(field);

        final Type[] paramClasses = method.getGenericParameterTypes();
        for (MessagePartInfo mpi : message.getMessageParts()) {
            int idx = hasReturnType ? mpi.getIndex() - 1 : mpi.getIndex();
            if (idx >= 0) {
                String name = mpi.getName().getLocalPart();
                Type t = paramClasses[idx];
                String type = getTypeString(t);
                JavaField jf = new JavaField(name, type, "");
                List<Annotation> jaxbAnns = WrapperUtil.getJaxbAnnotations(method, idx - 1);
                jf.setJaxbAnnotations(jaxbAnns.toArray(new Annotation[jaxbAnns.size()]));
                if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType)t;
                    Class c = (Class)pt.getRawType();
                    if (Holder.class.isAssignableFrom(c)) {
                        Annotation[][] paramAnnotations = method.getParameterAnnotations();
                        WebParam wParam = getWebParamAnnotation(paramAnnotations[idx]);
                        if (wParam != null && !StringUtils.isEmpty(wParam.targetNamespace())) {
                            jf.setTargetNamespace(wParam.targetNamespace());
                        }
                    }                   
                }
                fields.add(jf);
            }
        }

        return fields;
    }
    
    @Override
    public WrapperBeanClass getWrapperBeanClass(final Method method) {
        javax.xml.ws.ResponseWrapper resWrapper = method.getAnnotation(javax.xml.ws.ResponseWrapper.class);
        String resClassName = getClassName();
        String resNs = null;

        if (resWrapper != null) {
            resClassName = resWrapper.className().length() > 0 ? resWrapper.className() : resClassName;
            resNs = resWrapper.targetNamespace().length() > 0 ? resWrapper.targetNamespace() : null;
        }
        if (resClassName == null) {
            resClassName = getPackageName(method) + ".jaxws."
                + StringUtils.capitalize(method.getName())
                + "Response";
        }

        WrapperBeanClass jClass = new WrapperBeanClass();
        jClass.setFullClassName(resClassName);
        jClass.setNamespace(resNs);
        return jClass;
    }
}
