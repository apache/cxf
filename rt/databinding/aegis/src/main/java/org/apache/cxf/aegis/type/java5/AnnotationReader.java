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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jakarta.xml.bind.annotation.XmlEnumValue;
import org.apache.cxf.aegis.type.AegisType;

public class AnnotationReader {
    private static final Class<? extends Annotation> WEB_PARAM = jakarta.jws.WebParam.class;
    private static final Class<? extends Annotation> WEB_RESULT = jakarta.jws.WebResult.class;
    private static final Class<? extends Annotation> XML_ATTRIBUTE =
            jakarta.xml.bind.annotation.XmlAttribute.class;
    private static final Class<? extends Annotation> XML_ELEMENT =
            jakarta.xml.bind.annotation.XmlElement.class;
    private static final Class<? extends Annotation> XML_SCHEMA =
            jakarta.xml.bind.annotation.XmlSchema.class;
    private static final Class<? extends Annotation> XML_TYPE =
            jakarta.xml.bind.annotation.XmlType.class;
    private static final Class<? extends Annotation> XML_TRANSIENT =
            jakarta.xml.bind.annotation.XmlTransient.class;

    public boolean isIgnored(AnnotatedElement element) {
        return isAnnotationPresent(element,
                IgnoreProperty.class,
                XML_TRANSIENT);
    }

    public boolean isAttribute(AnnotatedElement element) {
        return isAnnotationPresent(element,
                XmlAttribute.class,
                XML_ATTRIBUTE);
    }

    public boolean isElement(AnnotatedElement element) {
        return isAnnotationPresent(element,
                XmlElement.class,
                XML_ELEMENT);
    }

    public Boolean isNillable(AnnotatedElement element) {
        return Boolean.TRUE.equals(getAnnotationValue("nillable",
                element,
                Boolean.FALSE,
                XmlElement.class,
                XML_ELEMENT));
    }
    public static Boolean isNillable(Annotation[] anns) {
        if (anns == null) {
            return null;
        }
        return (Boolean)getAnnotationValue("nillable",
                anns,
                XmlElement.class,
                XML_ELEMENT);
    }

    public Class<?> getType(AnnotatedElement element) {
        Class<?> value = (Class<?>) getAnnotationValue("type",
                element,
                AegisType.class,
                XmlAttribute.class,
                XmlElement.class);
        // jaxb uses a different default value
        if (value == null) {
            value = (Class<?>) getAnnotationValue("type",
                    element,
                    jakarta.xml.bind.annotation.XmlElement.DEFAULT.class,
                    XML_ELEMENT);
        }

        return value;
    }

    public Class<?> getParamType(Method method, int index) {
        return (Class<?>) getAnnotationValue("type",
                method,
                index,
                AegisType.class,
                XmlParamType.class);
    }

    public Class<?> getReturnType(AnnotatedElement element) {
        return (Class<?>) getAnnotationValue("type",
                element,
                AegisType.class,
                XmlReturnType.class);
    }

    public String getName(AnnotatedElement element) {
        String name = (String) getAnnotationValue("name",
                element,
                "",
                XmlType.class,
                XmlAttribute.class,
                XmlElement.class);

        // jaxb uses a different default value
        if (name == null) {
            name = (String) getAnnotationValue("name",
                    element,
                    "##default",
                    XML_TYPE,
                    XML_ATTRIBUTE,
                    XML_ELEMENT);
        }
        return name;
    }

    public String getParamTypeName(Method method, int index) {
        return (String) getAnnotationValue("name",
                method,
                index,
                AegisType.class,
                XmlParamType.class);
    }

    public String getReturnTypeName(AnnotatedElement element) {
        return (String) getAnnotationValue("name",
                element,
                "",
                XmlReturnType.class);
    }

    public String getNamespace(AnnotatedElement element) {
        // some poor class loader implementations may end not define Package elements
        if (element == null) {
            return null;
        }

        String namespace = (String) getAnnotationValue("namespace",
                element,
                "",
                XmlType.class,
                XmlAttribute.class,
                XmlElement.class,
                XML_SCHEMA);

        // jaxb uses a different default value
        if (namespace == null) {
            namespace = (String) getAnnotationValue("namespace",
                    element,
                    "##default",
                    XML_TYPE,
                    XML_ATTRIBUTE,
                    XML_ELEMENT);
        }

        return namespace;
    }

    public String getParamNamespace(Method method, int index) {
        String namespace = (String) getAnnotationValue("namespace",
                method,
                index,
                "",
                XmlParamType.class);

        // JWS annotation field is named targetNamespace
        if (namespace == null) {
            namespace = (String) getAnnotationValue("targetNamespace", method, index, "", WEB_PARAM);
        }
        return namespace;
    }

    public String getReturnNamespace(AnnotatedElement element) {
        String namespace = (String) getAnnotationValue("namespace",
                element,
                "",
                XmlReturnType.class);

        // JWS annotation field is named targetNamespace
        if (namespace == null) {
            namespace = (String) getAnnotationValue("targetNamespace", element, "", WEB_RESULT);
        }
        return namespace;
    }

    public int getMinOccurs(AnnotatedElement element) {
        String minOccurs = (String) getAnnotationValue("minOccurs",
                element,
                "",
                XmlElement.class);
        if (minOccurs != null) {
            return Integer.parseInt(minOccurs);
        }

        // check jaxb annotation
        Boolean required = (Boolean) getAnnotationValue("required", element, null, XML_ELEMENT);
        if (Boolean.TRUE.equals(required)) {
            return 1;
        }

        return 0;
    }
    public static Integer getMinOccurs(Annotation[] anns) {
        if (anns == null) {
            return null;
        }
        String minOccurs = (String) getAnnotationValue("minOccurs",
                anns,
                XmlElement.class);
        if (minOccurs != null) {
            return Integer.valueOf(minOccurs);
        }

        // check jaxb annotation
        Boolean required = (Boolean) getAnnotationValue("required", anns, XML_ELEMENT);
        if (Boolean.TRUE.equals(required)) {
            return 1;
        }
        return null;
    }
    public boolean isExtensibleElements(AnnotatedElement element, boolean defaultValue) {
        Boolean extensibleElements = (Boolean) getAnnotationValue("extensibleElements",
                element,
                Boolean.TRUE,
                XmlType.class);

        if (extensibleElements == null) {
            return defaultValue;
        }
        return extensibleElements;
    }

    public boolean isExtensibleAttributes(AnnotatedElement element, boolean defaultValue) {
        Boolean extensibleAttributes = (Boolean) getAnnotationValue("extensibleAttributes",
                element,
                Boolean.TRUE,
                XmlType.class);

        if (extensibleAttributes == null) {
            return defaultValue;
        }
        return extensibleAttributes;
    }

    @SafeVarargs
    private static boolean isAnnotationPresent(AnnotatedElement element,
            Class<? extends Annotation>... annotations) {
        for (Class<?> annotation : annotations) {
            if (annotation != null && element.isAnnotationPresent(annotation.asSubclass(Annotation.class))) {
                return true;
            }
        }
        return false;
    }

    @SafeVarargs
    static Object getAnnotationValue(String name,
            AnnotatedElement element,
            Object ignoredValue,
            Class<? extends Annotation>... annotations) {

        for (Class<?> annotation : annotations) {
            if (annotation != null && element != null) {
                try {
                    Annotation ann = element.getAnnotation(annotation.asSubclass(Annotation.class));
                    if (ann != null) {
                        Method method = ann.annotationType().getMethod(name);
                        Object value = method.invoke(ann);
                        if ((ignoredValue == null && value != null) || (ignoredValue != null
                                && !ignoredValue.equals(value))) {
                            return value;
                        }
                    }
                } catch (Exception ignored) {
                    // annotation did not have value
                }
            }
        }
        return null;
    }
    @SafeVarargs
    static Object getAnnotationValue(String name,
                                     Annotation[] anns,
                                     Class<? extends Annotation>... annotations) {
        for (Class<?> annotation : annotations) {
            if (annotation != null) {
                try {
                    for (Annotation ann : anns) {
                        if (annotation.isInstance(ann)) {
                            Method method = ann.annotationType().getMethod(name);
                            return method.invoke(ann);
                        }
                    }
                } catch (Exception ignored) {
                    // annotation did not have value
                }
            }
        }
        return null;
    }

    @SafeVarargs
    static Object getAnnotationValue(String name,
            Method method,
            int index,
            Object ignoredValue,
            Class<? extends Annotation>... annotations) {

        if (method.getParameterAnnotations() == null
            || method.getParameterAnnotations().length <= index
            || method.getParameterAnnotations()[index] == null) {
            return null;
        }

        for (Class<? extends Annotation> annotation : annotations) {
            if (annotation != null) {
                try {
                    Annotation ann = getAnnotation(method, index, annotation);
                    if (ann != null) {
                        Object value = ann.annotationType().getMethod(name).invoke(ann);
                        if ((ignoredValue == null && value != null) || (ignoredValue != null
                                && !ignoredValue.equals(value))) {
                            return value;
                        }
                    }
                } catch (Exception ignored) {
                    // annotation did not have value
                }
            }
        }
        return null;
    }

    private static Annotation getAnnotation(Method method, int index, Class<? extends Annotation> type) {
        if (method.getParameterAnnotations() == null
            || method.getParameterAnnotations().length <= index
            || method.getParameterAnnotations()[index] == null) {
            return null;
        }

        Annotation[] annotations = method.getParameterAnnotations()[index];
        for (Annotation annotation : annotations) {
            if (type.isInstance(annotation)) {
                return annotation;
            }
        }
        return null;
    }

    public boolean isFlat(Annotation[] annotations) {
        if (annotations != null) {
            for (Annotation a : annotations) {
                if (a instanceof XmlFlattenedArray) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getEnumValue(Enum<?> enumConstant) {
        @SuppressWarnings("rawtypes")
        Class<? extends Enum> enumClass = enumConstant.getClass();
        try {
            Field constantField = enumClass.getDeclaredField(enumConstant.name());
            XmlEnumValue constantValueAnnotation = constantField.getAnnotation(XmlEnumValue.class);
            if (constantValueAnnotation == null) {
                return null;
            }
            return constantValueAnnotation.value();
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

}
