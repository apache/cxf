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
package org.apache.cxf.aegis.type.java5;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.type.basic.BeanTypeInfo;
import org.apache.cxf.aegis.util.NamespaceHelper;

public class AnnotatedTypeInfo extends BeanTypeInfo {
    private final AnnotationReader annotationReader;

    public AnnotatedTypeInfo(TypeMapping tm, Class typeClass, String ns) {
        this(tm, typeClass, ns, new AnnotationReader());
    }

    public AnnotatedTypeInfo(TypeMapping tm, Class typeClass, String ns, AnnotationReader annotationReader) {
        super(typeClass, ns);
        this.annotationReader = annotationReader;
        setTypeMapping(tm);
        initialize();
    }

    /**
     * Override from parent in order to check for IgnoreProperty annotation.
     */
    protected void mapProperty(PropertyDescriptor pd) {
        // skip ignored properties
        if (annotationReader.isIgnored(pd.getReadMethod())) {
            return; 
        }

        String name = pd.getName();
        if (isAttribute(pd)) {
            mapAttribute(name, createMappedName(pd));
        } else if (isElement(pd)) {
            mapElement(name, createMappedName(pd));
        }
    }

    @Override
    protected boolean registerType(PropertyDescriptor desc) {
        Method readMethod = desc.getReadMethod();

        Class type = annotationReader.getType(readMethod);
        return type == null && super.registerType(desc);
    }

    protected boolean isAttribute(PropertyDescriptor desc) {
        return annotationReader.isAttribute(desc.getReadMethod());
    }

    protected boolean isElement(PropertyDescriptor desc) {
        return !isAttribute(desc);
    }

    @Override
    protected QName createMappedName(PropertyDescriptor desc) {
        return createQName(desc);
    }

    protected QName createQName(PropertyDescriptor desc) {
        String name = annotationReader.getName(desc.getReadMethod());
        if (name == null) {
            name = desc.getName();
        }

        // namespace: method, class, package, generated
        String namespace = annotationReader.getNamespace(desc.getReadMethod());
        if (namespace == null) {
            namespace = annotationReader.getNamespace(getTypeClass());
        }
        if (namespace == null) {
            namespace = annotationReader.getNamespace(getTypeClass().getPackage());
        }
        if (namespace == null) {
            namespace = NamespaceHelper.makeNamespaceFromClassName(getTypeClass().getName(), "http");
        }

        return new QName(namespace, name);
    }

    public boolean isNillable(QName name) {
        PropertyDescriptor desc = getPropertyDescriptorFromMappedName(name);
        if (annotationReader.isElement(desc.getReadMethod())) {
            return annotationReader.isNillable(desc.getReadMethod());
        } else {
            return super.isNillable(name);
        }
    }

    public int getMinOccurs(QName name) {
        PropertyDescriptor desc = getPropertyDescriptorFromMappedName(name);
        if (annotationReader.isElement(desc.getReadMethod())) {
            return annotationReader.getMinOccurs(desc.getReadMethod());
        }
        return super.getMinOccurs(name);
    }
}
