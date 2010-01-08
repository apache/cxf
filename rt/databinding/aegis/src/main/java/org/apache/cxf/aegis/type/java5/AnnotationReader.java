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
import java.lang.reflect.Method;

import org.apache.cxf.aegis.type.Type;

public class AnnotationReader {
    private static final Class<? extends Annotation> WEB_PARAM = load("javax.jws.WebParam");
    private static final Class<? extends Annotation> WEB_RESULT = load("javax.jws.WebResult");
    private static final Class<? extends Annotation> XML_ATTRIBUTE =
            load("javax.xml.bind.annotation.XmlAttribute");
    private static final Class<? extends Annotation> XML_ELEMENT =
            load("javax.xml.bind.annotation.XmlElement");
    private static final Class<? extends Annotation> XML_SCHEMA =
            load("javax.xml.bind.annotation.XmlSchema");
    private static final Class<? extends Annotation> XML_TYPE =
            load("javax.xml.bind.annotation.XmlType");
    private static final Class<? extends Annotation> XML_TRANSIENT =
            load("javax.xml.bind.annotation.XmlTransient");
    
    private static final Class<? extends Annotation> XFIRE_IGNORE_PROPERTY =
            load("org.codehaus.xfire.aegis.type.java5.IgnoreProperty");            
    private static final Class<? extends Annotation> XFIRE_XML_ATTRIBUTE =
        load("org.codehaus.xfire.aegis.type.java5.XmlAttribute");            
    private static final Class<? extends Annotation> XFIRE_XML_ELEMENT =
        load("org.codehaus.xfire.aegis.type.java5.XmlElement");            
    private static final Class<? extends Annotation> XFIRE_XML_TYPE =
        load("org.codehaus.xfire.aegis.type.java5.XmlType");
    private static final Class<? extends Annotation> XFIRE_XML_PARAM_TYPE =
        load("org.codehaus.xfire.aegis.type.java5.XmlParamType");
    private static final Class<? extends Annotation> XFIRE_XML_RETURN_TYPE =
        load("org.codehaus.xfire.aegis.type.java5.XmlReturnType");
    
    
    @SuppressWarnings("unchecked")
    public boolean isIgnored(AnnotatedElement element) {
        return isAnnotationPresent(element,
                IgnoreProperty.class,
                XFIRE_IGNORE_PROPERTY,
                XML_TRANSIENT);
    }

    @SuppressWarnings("unchecked")
    public boolean isAttribute(AnnotatedElement element) {
        return isAnnotationPresent(element,
                XmlAttribute.class,
                XFIRE_XML_ATTRIBUTE,
                XML_ATTRIBUTE);
    }

    @SuppressWarnings("unchecked")
    public boolean isElement(AnnotatedElement element) {
        return isAnnotationPresent(element,
                XmlElement.class,
                XFIRE_XML_ELEMENT,
                XML_ELEMENT);
    }

    // PMD incorrectly identifies this as a string comparison
    @SuppressWarnings("unchecked")
    public Boolean isNillable(AnnotatedElement element) {
        return Boolean.TRUE.equals(getAnnotationValue("nillable", // NOPMD
                element,
                Boolean.FALSE,
                XmlElement.class,
                XFIRE_XML_ELEMENT,
                XML_ELEMENT));
    }
    @SuppressWarnings("unchecked")
    public static Boolean isNillable(Annotation[] anns) {
        if (anns == null) {
            return null;
        }
        return (Boolean)getAnnotationValue("nillable", // NOPMD
                anns,
                XmlElement.class,
                XFIRE_XML_ELEMENT,
                XML_ELEMENT);
    }

    @SuppressWarnings("unchecked")
    public Class getType(AnnotatedElement element) {
        Class value = (Class) getAnnotationValue("type",
                element,
                Type.class,
                XmlAttribute.class,
                XmlElement.class,
                XFIRE_XML_ATTRIBUTE,
                XFIRE_XML_ELEMENT);
        // jaxb uses a different default value
        if (value == null) {
            value = (Class) getAnnotationValue("type",
                    element,
                    javax.xml.bind.annotation.XmlElement.DEFAULT.class,
                    XML_ELEMENT);
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    public Class getParamType(Method method, int index) {
        return (Class) getAnnotationValue("type",
                method,
                index,
                Type.class,
                XmlParamType.class,
                XFIRE_XML_PARAM_TYPE);
    }

    @SuppressWarnings("unchecked")
    public Class getReturnType(AnnotatedElement element) {
        return (Class) getAnnotationValue("type",
                element,
                Type.class,
                XmlReturnType.class,
                XFIRE_XML_RETURN_TYPE);
    }

    @SuppressWarnings("unchecked")
    public String getName(AnnotatedElement element) {
        String name = (String) getAnnotationValue("name",
                element,
                "",
                XmlType.class,
                XFIRE_XML_TYPE,
                XmlAttribute.class,
                XFIRE_XML_ATTRIBUTE,
                XmlElement.class,
                XFIRE_XML_ELEMENT);

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

    @SuppressWarnings("unchecked")
    public String getParamName(Method method, int index) {
        return (String) getAnnotationValue("name",
                method,
                index,
                Type.class,
                XmlParamType.class,
                XFIRE_XML_PARAM_TYPE,
                WEB_PARAM);
    }

    @SuppressWarnings("unchecked")
    public String getReturnName(AnnotatedElement element) {
        return (String) getAnnotationValue("name",
                element,
                "",
                XmlReturnType.class,
                XFIRE_XML_RETURN_TYPE,
                WEB_RESULT);
    }

    @SuppressWarnings("unchecked")
    public String getNamespace(AnnotatedElement element) {
        // some poor class loader implementations may end not define Package elements
        if (element == null) {
            return null;
        }

        String namespace = (String) getAnnotationValue("namespace",
                element,
                "",
                XmlType.class,
                XFIRE_XML_TYPE,
                XmlAttribute.class,
                XFIRE_XML_ATTRIBUTE,
                XmlElement.class,
                XFIRE_XML_ELEMENT,
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

    @SuppressWarnings("unchecked")
    public String getParamNamespace(Method method, int index) {
        String namespace = (String) getAnnotationValue("namespace",
                method,
                index,
                "",
                XmlParamType.class,
                XFIRE_XML_PARAM_TYPE);

        // JWS annotation field is named targetNamespace
        if (namespace == null) {
            namespace = (String) getAnnotationValue("targetNamespace", method, index, "", WEB_PARAM);
        }
        return namespace;
    }

    @SuppressWarnings("unchecked")
    public String getReturnNamespace(AnnotatedElement element) {
        String namespace = (String) getAnnotationValue("namespace",
                element,
                "",
                XmlReturnType.class,
                XFIRE_XML_RETURN_TYPE);

        // JWS annotation field is named targetNamespace
        if (namespace == null) {
            namespace = (String) getAnnotationValue("targetNamespace", element, "", WEB_RESULT);
        }
        return namespace;
    }

    @SuppressWarnings("unchecked")
    public int getMinOccurs(AnnotatedElement element) {
        String minOccurs = (String) getAnnotationValue("minOccurs",
                element,
                "",
                XmlElement.class,
                XFIRE_XML_ELEMENT);
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
    @SuppressWarnings("unchecked")
    public static Integer getMinOccurs(Annotation[] anns) {
        if (anns == null) {
            return null;
        }
        String minOccurs = (String) getAnnotationValue("minOccurs",
                anns,
                XmlElement.class,
                XFIRE_XML_ELEMENT);
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
    @SuppressWarnings("unchecked")
    public boolean isExtensibleElements(AnnotatedElement element, boolean defaultValue) {
        Boolean extensibleElements = (Boolean) getAnnotationValue("extensibleElements",
                element,
                Boolean.TRUE,
                XmlType.class,
                XFIRE_XML_TYPE);

        if (extensibleElements == null) {
            return defaultValue;
        }
        return extensibleElements;
    }

    @SuppressWarnings("unchecked")
    public boolean isExtensibleAttributes(AnnotatedElement element, boolean defaultValue) {
        Boolean extensibleAttributes = (Boolean) getAnnotationValue("extensibleAttributes",
                element,
                Boolean.TRUE,
                XmlType.class,
                XFIRE_XML_TYPE);

        if (extensibleAttributes == null) {
            return defaultValue;
        }
        return extensibleAttributes;
    }

    // PMD doesn't fully understand varargs
    private static boolean isAnnotationPresent(AnnotatedElement element, // NOPMD
            Class<? extends Annotation>... annotations) {
        for (Class<?> annotation : annotations) {
            if (annotation != null && element.isAnnotationPresent(annotation.asSubclass(Annotation.class))) {
                return true;
            }
        }
        return false;
    }

    static Object getAnnotationValue(String name,
            AnnotatedElement element,
            Object ignoredValue,
            Class<? extends Annotation>... annotations) {
        
        for (Class<?> annotation : annotations) {
            if (annotation != null) {
                try {
                    Annotation ann = element.getAnnotation(annotation.asSubclass(Annotation.class));
                    if (ann != null) {
                        Method method = ann.getClass().getMethod(name);
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
    static Object getAnnotationValue(String name,
                                     Annotation[] anns,
                                     Class<? extends Annotation>... annotations) {
        for (Class<?> annotation : annotations) {
            if (annotation != null) {
                try {
                    for (Annotation ann : anns) {
                        if (annotation.isInstance(ann)) {
                            Method method = ann.getClass().getMethod(name);
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
                        Object value = ann.getClass().getMethod(name).invoke(ann);
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

    private static Class<? extends Annotation> load(String name) {
        try {
            return AnnotationReader.class.getClassLoader().loadClass(name).asSubclass(Annotation.class);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

}
