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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaField;
import org.apache.cxf.tools.java2wsdl.generator.wsdl11.annotator.WrapperBeanAnnotator;
import org.apache.cxf.tools.java2wsdl.generator.wsdl11.annotator.WrapperBeanFieldAnnotator;
import org.apache.cxf.tools.java2wsdl.generator.wsdl11.model.WrapperBeanClass;
import org.apache.cxf.tools.util.AnnotationUtil;
import org.apache.cxf.tools.util.NameUtil;
import org.apache.cxf.tools.util.URIParserUtil;


public class Wrapper {
    private static final Logger LOG = LogUtils.getL7dLogger(Wrapper.class);
    private QName name;
    private WrapperBeanClass javaClass;
    private Method method;
    private boolean isSamePackage;

    private OperationInfo operationInfo;
    private String className;

    public void setOperationInfo(final OperationInfo op) {
        this.operationInfo = op;
        setMethod((Method) op.getProperty("operation.method"));
    }

    public void setMethod(Method m) {
        this.method = m;
    }

    public void setName(QName n) {
        this.name = n;
    }
    public void setClassName(String s) {
        className = s;
    }
    public String getClassName() {
        return className;
    }

    public WrapperBeanClass getWrapperBeanClass(final Method m) {
        return new WrapperBeanClass();
    }
    
    protected WrapperBeanClass getWrapperBeanClass(final QName wrapperBeanName) {
        WrapperBeanClass jClass = new WrapperBeanClass();
        if (wrapperBeanName == null) {
            return jClass;
        }
        String ns = wrapperBeanName.getNamespaceURI();
        jClass.setNamespace(ns);
        jClass.setPackageName(URIParserUtil.getPackageName(ns));
        jClass.setName(NameUtil.mangleNameToClassName(wrapperBeanName.getLocalPart()));
        jClass.setElementName(wrapperBeanName);
        return jClass;
    }

    private WrapperBeanClass merge(final WrapperBeanClass c1, final WrapperBeanClass c2) {
        if (c1.getElementName() == null) {
            c1.setElementName(c2.getElementName());
        }

        if (StringUtils.isEmpty(c1.getNamespace())) {
            c1.setNamespace(c2.getNamespace());
        }

        if (StringUtils.isEmpty(c1.getPackageName())) {
            c1.setPackageName(c2.getPackageName());
        } else {
            this.isSamePackage = c1.getPackageName().equals(c2.getPackageName());
        }


        if (StringUtils.isEmpty(c1.getName())) {
            c1.setName(c2.getName());
        }
        return c1;
    }
    
    public WrapperBeanClass getJavaClass() {
        if (javaClass == null) {
            WrapperBeanClass jClass1 = getWrapperBeanClass(this.name);
            WrapperBeanClass jClass2 = getWrapperBeanClass(this.method);
            javaClass = merge(jClass2, jClass1);
        }
        return javaClass;
    }

    public WrapperBeanClass buildWrapperBeanClass() {
        WrapperBeanClass jClass = getJavaClass();
        List<JavaField> fields = buildFields();
        for (JavaField field : fields) {
            field.setOwner(jClass);
            field.annotate(new WrapperBeanFieldAnnotator());
            jClass.addField(field);
            jClass.appendGetter(field);
            jClass.appendSetter(field);
        }
        jClass.annotate(new WrapperBeanAnnotator());
        return jClass;
    }

    protected String getPackageName(final Method m) {
        String pkg = PackageUtils.getPackageName(m.getDeclaringClass());
        return pkg.length() == 0 ? ToolConstants.DEFAULT_PACKAGE_NAME : pkg;
    }

    public boolean isWrapperAbsent() {
        return isWrapperAbsent(this.method);
    }

    public boolean isWrapperAbsent(final Method m) {
        return false;
    }

    public boolean isWrapperBeanClassNotExist() {
        try {
            Message msg = new Message("LOADING_WRAPPER_CLASS", LOG, getJavaClass().getFullClassName());
            LOG.log(Level.FINE, msg.toString());
            getWrapperClass();
            return false;
        } catch (ToolException e) {
            return true;
        }
    }

    public boolean isToDifferentPackage() {
        return !isSamePackage;
    }

    public Class getWrapperClass() {
        try {
            return AnnotationUtil.loadClass(getJavaClass().getFullClassName(),
                                            getClass().getClassLoader());
        } catch (Exception e) {
            Message msg = new Message("LOAD_WRAPPER_CLASS_FAILED", LOG, getJavaClass().getFullClassName());
            LOG.log(Level.FINE, msg.toString());
            throw new ToolException(msg);
        }
    }

    protected boolean isBuiltInTypes(Class<?> clz) {
        if (clz == null || clz.isPrimitive()) {
            return true;
        }
        return "java.lang".equals(clz.getPackage().getName());
    }
    
    protected List<JavaField> buildFields() {
        return new ArrayList<JavaField>();
    }

    public Method getMethod() {
        return this.method;
    }

    public OperationInfo getOperationInfo() {
        return this.operationInfo;
    }
    
    protected String getTypeString(Type t) {
        String type = "Object";
        if (t instanceof Class) {
            Class clz = (Class) t;
            if (clz.isArray()) {
                if (isBuiltInTypes(clz.getComponentType())) {
                    type = clz.getComponentType().getSimpleName() + "[]";
                } else {
                    type = clz.getComponentType().getName() + "[]";
                }
            } else {
                type = clz.getName();
            }
        } else if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            Class c = (Class)pt.getRawType();
            if (Holder.class.isAssignableFrom(c)
                && pt.getActualTypeArguments().length == 1
                && pt.getActualTypeArguments()[0] instanceof Class) {
                type = getTypeString(pt.getActualTypeArguments()[0]);
            } else {
                type = t.toString();
            }
        } else if (t instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType)t;
            type = gat.toString();
        }
        type = type.replace('$', '.');
        return type;
    }

}
