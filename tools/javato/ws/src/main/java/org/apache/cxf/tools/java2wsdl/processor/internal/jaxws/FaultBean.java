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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.ws.WebFault;

import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.model.JavaClass;
import org.apache.cxf.tools.common.model.JavaField;
import org.apache.cxf.tools.java2wsdl.generator.wsdl11.annotator.WrapperBeanAnnotator;
import org.apache.cxf.tools.java2wsdl.generator.wsdl11.model.WrapperBeanClass;
import org.apache.cxf.tools.util.AnnotationUtil;
import org.apache.cxf.tools.util.URIParserUtil;

public final class FaultBean {
    private static final String[] EXCLUDED_GETTER = new String[] {"getCause",
                                                                  "getLocalizedMessage",
                                                                  "getStackTrace",
                                                                  "getClass"};

    public boolean faultBeanExists(final Class<?> exceptionClass) {
        String fb = getWebFaultBean(exceptionClass);
        if (!StringUtils.isEmpty(fb)) {
            try {
                return AnnotationUtil.loadClass(fb,
                                                exceptionClass.getClassLoader()) != null;
            } catch (Exception e) {
                return false;
            }
        }
        return false; 
    }
    
    private String getWebFaultBean(final Class<?> exceptionClass) {
        WebFault fault = exceptionClass.getAnnotation(WebFault.class);
        if (fault == null) {
            return null;
        }
        return fault.faultBean();
    }

    private boolean isWebFaultAbsent(final Class exceptionClass) {
        return StringUtils.isEmpty(getWebFaultBean(exceptionClass));
    }
    
    public WrapperBeanClass transform(final Class exceptionClass, final String defaultPackage) {
        WrapperBeanClass jClass = new WrapperBeanClass();
        if (isWebFaultAbsent(exceptionClass)) {
            jClass.setName(exceptionClass.getSimpleName() + "Bean");
            jClass.setPackageName(defaultPackage);
        } else {
            jClass.setFullClassName(getWebFaultBean(exceptionClass));
        }

        buildBeanFields(exceptionClass, jClass);
        
        String pkg = PackageUtils.getPackageName(exceptionClass);
        if (pkg.length() > 0) {
            jClass.setElementName(new QName(URIParserUtil.getNamespace(pkg),
                                        exceptionClass.getSimpleName()));
        } else {
            jClass.setElementName(new QName(URIParserUtil.getNamespace(ToolConstants.DEFAULT_PACKAGE_NAME),
                                        exceptionClass.getSimpleName()));
        }
        jClass.annotate(new WrapperBeanAnnotator(exceptionClass));
        
        return jClass;
    }

    private String getFieldName(final Method method) {
        String name = method.getName().substring(3);
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    private boolean isIncludedGetter(final Method method) {
        return method.getName().startsWith("get")
            && !Arrays.asList(EXCLUDED_GETTER).contains(method.getName());
    }
    
    private void buildBeanFields(final Class exceptionClass, final JavaClass jClass) {
        Map<String, JavaField> fields = new TreeMap<String, JavaField>(); 
        
        for (Method method : exceptionClass.getMethods()) {
            if (isIncludedGetter(method)) {
                JavaField field = new JavaField(getFieldName(method),
                                                method.getReturnType().getName(),
                                                "");
                field.setOwner(jClass);
                fields.put(field.getName(), field);
            }
        }
        for (Map.Entry<String, JavaField> ent : fields.entrySet()) {
            jClass.addField(ent.getValue());
            jClass.appendGetter(ent.getValue());
            jClass.appendSetter(ent.getValue());
        }
    }
    
}
