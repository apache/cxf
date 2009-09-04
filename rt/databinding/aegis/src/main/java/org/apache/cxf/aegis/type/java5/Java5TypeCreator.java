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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AbstractTypeCreator;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeClassInfo;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.util.NamespaceHelper;
import org.apache.cxf.aegis.util.ServiceUtils;

public class Java5TypeCreator extends AbstractTypeCreator {
    private AnnotationReader annotationReader;

    public Java5TypeCreator() {
        this(new AnnotationReader());
    }

    public Java5TypeCreator(AnnotationReader annotationReader) {
        this.annotationReader = annotationReader;
    }
    
    public static Class<? extends AegisType> castToAegisTypeClass(Class<?> c) {
        if (c == null) {
            return null;
        }
        if (AegisType.class.isAssignableFrom(c)) {
            return c.asSubclass(AegisType.class);
        } else {
            throw new DatabindingException("Invalid Aegis type annotation to non-type class" + c);
        }
    }

    @Override
    public TypeClassInfo createClassInfo(Method m, int index) {
        if (index >= 0) {
            TypeClassInfo info;
            Type genericType = m.getGenericParameterTypes()[index];
            if (genericType instanceof Class) {
                info = nextCreator.createClassInfo(m, index);
            } else {
                info = new TypeClassInfo();
                info.setDescription("method " + m.getName() + " parameter " + index);
                info.setType(genericType);
            }

            Class paramTypeClass = annotationReader.getParamType(m, index);
            info.setAegisTypeClass(castToAegisTypeClass(paramTypeClass));

            String paramName = annotationReader.getParamName(m, index);
            if (paramName != null) {
                info.setTypeName(createQName(m.getParameterTypes()[index],
                        paramName,
                        annotationReader.getParamNamespace(m, index)));
            }

            return info;
        } else {
            Type genericReturnType = m.getGenericReturnType();
            TypeClassInfo info;
            if (genericReturnType instanceof Class) {
                info = nextCreator.createClassInfo(m, index);
            } else {
                info = new TypeClassInfo();
                info.setDescription("method " + m.getName() + " parameter " + index);
                info.setType(genericReturnType);
            }

            if (m.getParameterAnnotations() != null && m.getAnnotations().length > 0) {
                info.setAnnotations(m.getAnnotations());
            }

            info.setAegisTypeClass(castToAegisTypeClass(annotationReader.getReturnType(m)));

            String returnName = annotationReader.getReturnName(m);
            if (returnName != null) {
                info.setTypeName(createQName(m.getReturnType(),
                        returnName,
                        annotationReader.getReturnNamespace(m)));
            }

            return info;
        }
    }

    /*
     * Apparently, this callers must notice collection types and not call this.
     */
    @Override
    public TypeClassInfo createClassInfo(PropertyDescriptor pd) {
        Type genericType = pd.getReadMethod().getGenericReturnType();
        TypeClassInfo info = createBasicClassInfo(pd.getPropertyType());
        info.setType(genericType); // override basicClassInfo's of the type.
        info.setAnnotations(pd.getReadMethod().getAnnotations());
        info.setAegisTypeClass(castToAegisTypeClass(annotationReader.getType(pd.getReadMethod())));

        return info;
    }

    @Override
    public AegisType createCollectionType(TypeClassInfo info) {
        Type type = info.getType();

        Type componentType = getComponentType(type, 0);

        if (componentType != null) {
            return createCollectionTypeFromGeneric(info);
        } else {
            return nextCreator.createCollectionType(info);
        }
    }

    // should be called 'collection'
    protected AegisType getOrCreateGenericType(TypeClassInfo info) {
        return getOrCreateParameterizedType(info.getType(), 0);
    }

    protected AegisType getOrCreateMapKeyType(TypeClassInfo info) {
        return getOrCreateParameterizedType(info.getType(), 0);
    }

    protected AegisType getOrCreateMapValueType(TypeClassInfo info) {
        return getOrCreateParameterizedType(info.getType(), 1);
    }

    protected AegisType getOrCreateParameterizedType(Type generic, int index) {
        Type paramType = getComponentType(generic, index);

        if (paramType == null) {
            return createObjectType();
        }
        
        /* null arises when the index-th parameter to generic is something list List<T> */
        Class clazz = TypeUtil.getTypeRelatedClass(paramType);
        if (clazz == null) {
            return createObjectType();
        }
        
        // here is where we insist that we only deal with collection types.

        if (!Collection.class.isAssignableFrom(clazz) 
            && !Map.class.isAssignableFrom(clazz)) {
            return getTopCreator().createType(clazz);
        }
        
        TypeClassInfo info = createBasicClassInfo(clazz);
        info.setDescription(clazz.toString());
        info.setType(paramType); 

        AegisType type = createTypeForClass(info);

        return type;
    }

    protected Type getComponentType(Type genericType, int index) {
        if (genericType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType)genericType;
            Type paramType = type.getActualTypeArguments()[index]; 
            if (paramType instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType)paramType;
                // we really aren't prepared to deal with multiple upper bounds,
                // so we just look at the first one.
                return wildcardType.getUpperBounds()[0];
            } else {
                return paramType; // take our chances.
            }
        } else {
            return null;
        }
    }

    @Override
    public AegisType createDefaultType(TypeClassInfo info) {
        QName typeName = info.getTypeName();
        Class<?> relatedClass = TypeUtil.getTypeRelatedClass(info.getType());
        if (typeName == null) {
            typeName = createQName(relatedClass);
        }

        AnnotatedTypeInfo typeInfo = new AnnotatedTypeInfo(
                getTypeMapping(),
                relatedClass,
                typeName.getNamespaceURI(),
                getConfiguration());

        typeInfo.setExtensibleElements(annotationReader.isExtensibleElements(
                relatedClass,
                getConfiguration().isDefaultExtensibleElements()));
        typeInfo.setExtensibleAttributes(annotationReader.isExtensibleAttributes(
                relatedClass,
                getConfiguration().isDefaultExtensibleAttributes()));

        typeInfo.setDefaultMinOccurs(getConfiguration().getDefaultMinOccurs());
        typeInfo.setDefaultNillable(getConfiguration().isDefaultNillable());

        BeanType type = new BeanType(typeInfo);
        type.setTypeMapping(getTypeMapping());
        type.setSchemaType(typeName);

        return type;
    }

    @Override
    public AegisType createEnumType(TypeClassInfo info) {
        EnumType type = new EnumType();

        type.setSchemaType(createQName(TypeUtil.getTypeRelatedClass(info.getType())));
        type.setTypeClass(info.getType());
        type.setTypeMapping(getTypeMapping());

        return type;
    }

    @Override
    public QName createQName(Class typeClass) {
        String name = annotationReader.getName(typeClass);
        String ns = annotationReader.getNamespace(typeClass);
        return createQName(typeClass, name, ns);
    }

    private QName createQName(Class typeClass, String name, String ns) {
        if (name == null || name.length() == 0) {
            name = ServiceUtils.makeServiceNameFromClassName(typeClass);
        }

        // check jaxb package annotation
        if (ns == null || ns.length() == 0) {
            ns = annotationReader.getNamespace(typeClass.getPackage());
        }
        if (ns == null || ns.length() == 0) {
            ns = NamespaceHelper.makeNamespaceFromClassName(typeClass.getName(), "http");
        }

        return new QName(ns, name);
    }

    @Override
    protected boolean isEnum(Class javaType) {
        return javaType.isEnum();
    }
}
