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
package org.apache.cxf.aegis.type;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.util.NamespaceHelper;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.XmlSchemaUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.constants.Constants;


/**
 * Static methods/constants for Aegis.
 */
public final class TypeUtil {
    private static final Logger LOG = LogUtils.getL7dLogger(TypeUtil.class);

    private TypeUtil() {
        //utility class
    }

    public static AegisType getReadType(XMLStreamReader xsr, AegisContext context, AegisType baseType) {

        if (!context.isReadXsiTypes()) {
            if (baseType == null) {
                LOG.warning("xsi:type reading disabled, and no type available for "
                         + xsr.getName());
            }
            return baseType;
        }

        String overrideType = xsr.getAttributeValue(Constants.URI_2001_SCHEMA_XSI, "type");
        if (overrideType != null) {
            QName overrideName = NamespaceHelper.createQName(xsr.getNamespaceContext(), overrideType);

            if (baseType == null || !overrideName.equals(baseType.getSchemaType())) {
                AegisType improvedType = null;
                TypeMapping tm;
                if (baseType != null) {
                    tm = baseType.getTypeMapping();
                    improvedType = tm.getType(overrideName);
                }
                if (improvedType == null) {
                    improvedType = context.getRootType(overrideName);
                }
                if (improvedType != null) {
                    return improvedType;
                }
            }

            if (baseType != null) {
                LOG.finest("xsi:type=\"" + overrideName
                         + "\" was specified, but no corresponding AegisType was registered; defaulting to "
                         + baseType.getSchemaType());
                return baseType;
            }
            LOG.warning("xsi:type=\"" + overrideName
                     + "\" was specified, but no corresponding AegisType was registered; no default.");
            return null;
        }
        if (baseType == null) {
            LOG.warning("xsi:type absent, and no type available for "
                     + xsr.getName());
        }
        return baseType;
    }

    /**
     * getReadType cannot just look up the xsi:type in the mapping. This function must be
     * called instead at the root where there is no initial mapping to start from, as from
     * a part or an element of some containing item.
     * @param xsr
     * @param context
     * @return
     */
    public static AegisType getReadTypeStandalone(XMLStreamReader xsr,
                                                  AegisContext context, AegisType baseType) {

        if (baseType != null) {
            return getReadType(xsr, context, baseType);
        }

        if (!context.isReadXsiTypes()) {
            LOG.warning("xsi:type reading disabled, and no type available for "
                     + xsr.getName());
            return null;
        }

        String typeNameString = xsr.getAttributeValue(Constants.URI_2001_SCHEMA_XSI, "type");
        if (typeNameString != null) {
            QName schemaTypeName = NamespaceHelper.createQName(xsr.getNamespaceContext(),
                                                               typeNameString);
            TypeMapping tm;
            tm = context.getTypeMapping();
            AegisType type = tm.getType(schemaTypeName);

            if (type == null) {
                type = context.getRootType(schemaTypeName);
            }

            if (type != null) {
                return type;
            }

            LOG.warning("xsi:type=\"" + schemaTypeName
                     + "\" was specified, but no corresponding AegisType was registered; no default.");
            return null;
        }
        LOG.warning("xsi:type was not specified for top-level element " + xsr.getName());
        return null;
    }

    public static AegisType getWriteType(AegisContext globalContext, Object value, AegisType type) {
        if (value != null && type != null && type.getTypeClass() != value.getClass()) {
            AegisType overrideType = globalContext.getRootType(value.getClass());
            if (overrideType != null) {
                return overrideType;
            }
        }
        return type;
    }

    public static AegisType getWriteTypeStandalone(AegisContext globalContext, Object value, AegisType type) {
        if (type != null) {
            return getWriteType(globalContext, value, type);
        }

        TypeMapping tm;
        tm = globalContext.getTypeMapping();
        // don't use this for null!
        type = tm.getType(value.getClass());

        return type;
    }

    /**
     * Allow writing of collections when the type of the collection object is known via
     * an {@link java.lang.reflect.Type} object.
     * @param globalContext the context
     * @param value the object to write.
     * @param reflectType the type to use in writing the object.
     * @return
     */
    public static AegisType getWriteTypeStandalone(AegisContext globalContext,
                                              Object value,
                                              java.lang.reflect.Type reflectType) {
        if (reflectType == null) {
            return getWriteTypeStandalone(globalContext, value, (AegisType)null);
        }
        return globalContext.getTypeMapping().getTypeCreator().createType(reflectType);


    }

    public static void setAttributeAttributes(QName name, AegisType type, XmlSchema root) {
        String ns = type.getSchemaType().getNamespaceURI();
        XmlSchemaUtils.addImportIfNeeded(root, ns);
    }

    /**
     * Utility function to cast a Type to a Class. This throws an unchecked exception if the Type is
     * not a Class. The idea here is that these Type references should have been checked for
     * reasonableness before the point of calls to this function.
     * @param type Reflection type.
     * @param throwForNonClass whether to throw (true) or return null (false) if the Type
     * is not a class.
     * @return the Class
     */
    public static Class<?> getTypeClass(Type type, boolean throwForNonClass) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (throwForNonClass) {
            throw new RuntimeException("Attempt to derive Class from reflection Type " + type);
        } else {
            return null;
        }
    }

    /**
     * Insist that a Type is a parameterized type of one parameter.
     * This is used to decompose Holders, for example.
     * @param type the type
     * @return the parameter, or null if the type is not what we want.
     */
    public static Type getSingleTypeParameter(Type type) {
        return getSingleTypeParameter(type, 0);
    }

    /**
     * Insist that a Type is a parameterized type of one parameter.
     * This is used to decompose Holders, for example.
     * @param type the type
     * @param index which parameter
     * @return the parameter, or null if the type is not what we want.
     */
    public static Type getSingleTypeParameter(Type type, int index) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] params = pType.getActualTypeArguments();
            if (params.length > index) {
                return params[index];
            }
        }
        return null;
    }

    /**
     * If a Type is a class, return it as a class.
     * If it is a ParameterizedType, return the raw type as a class.
     * Otherwise return null.
     * @param type
     * @return
     */
    public static Class<?> getTypeRelatedClass(Type type) {
        Class<?> directClass = getTypeClass(type, false);
        if (directClass != null) {
            return directClass;
        }

        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            return getTypeRelatedClass(pType.getRawType());
        }

        if (type instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) type;
            Type compType = gat.getGenericComponentType();
            Class<?> arrayBaseType = getTypeRelatedClass(compType);
            // believe it or not, this seems to be the only way to get the
            // Class object for an array of primitive type.
            Object instance = Array.newInstance(arrayBaseType, 0);
            return instance.getClass();
        }
        return null;
    }
}
