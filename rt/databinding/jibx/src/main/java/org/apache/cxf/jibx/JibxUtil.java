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

package org.apache.cxf.jibx;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.jibx.runtime.Utility;

public final class JibxUtil {

    public static final String SCHEMA_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

    public static final QName STRING_QNAME = new QName(SCHEMA_NAMESPACE, "string");

    private static Map<String, QName> simpleObjectTypeMap = new HashMap<String, QName>();

    private static Map<String, QName> simplePrimitiveTypeMap = new HashMap<String, QName>();

    private static Map<String, Class<?>> simpleWrapperMap = new HashMap<String, Class<?>>();

    private static Map<QName, Format> simpleTypeMap = new HashMap<QName, Format>();

    private static final Class<Utility> UTILITY = org.jibx.runtime.Utility.class;

    private static final Class<String> INPUT_TYPE = java.lang.String.class;

    static {
        simpleObjectTypeMap.put("java.lang.Boolean", new QName(SCHEMA_NAMESPACE, "boolean"));
        simpleObjectTypeMap.put("java.lang.Byte", new QName(SCHEMA_NAMESPACE, "byte"));
        simpleObjectTypeMap.put("java.lang.Character", new QName(SCHEMA_NAMESPACE, "unsignedInt"));
        simpleObjectTypeMap.put("java.lang.Double", new QName(SCHEMA_NAMESPACE, "double"));
        simpleObjectTypeMap.put("java.lang.Float", new QName(SCHEMA_NAMESPACE, "float"));
        simpleObjectTypeMap.put("java.lang.Integer", new QName(SCHEMA_NAMESPACE, "int"));
        simpleObjectTypeMap.put("java.lang.Long", new QName(SCHEMA_NAMESPACE, "long"));
        simpleObjectTypeMap.put("java.lang.Short", new QName(SCHEMA_NAMESPACE, "short"));
        simpleObjectTypeMap.put("java.lang.String", STRING_QNAME);
        simpleObjectTypeMap.put("java.math.BigDecimal", new QName(SCHEMA_NAMESPACE, "decimal"));
        simpleObjectTypeMap.put("java.math.BigInteger", new QName(SCHEMA_NAMESPACE, "integer"));
        simpleObjectTypeMap.put("java.util.Date", new QName(SCHEMA_NAMESPACE, "dateTime"));
        // #!j2me{
        simpleObjectTypeMap.put("java.sql.Date", new QName(SCHEMA_NAMESPACE, "date"));
        simpleObjectTypeMap.put("java.sql.Time", new QName(SCHEMA_NAMESPACE, "time"));
        simpleObjectTypeMap.put("java.sql.Timestamp", new QName(SCHEMA_NAMESPACE, "dateTime"));
        simpleObjectTypeMap.put("org.joda.time.LocalDate", new QName(SCHEMA_NAMESPACE, "date"));
        simpleObjectTypeMap.put("org.joda.time.DateMidnight", new QName(SCHEMA_NAMESPACE, "date"));
        simpleObjectTypeMap.put("org.joda.time.LocalTime", new QName(SCHEMA_NAMESPACE, "time"));
        simpleObjectTypeMap.put("org.joda.time.DateTime", new QName(SCHEMA_NAMESPACE, "dateTime"));
        // #j2me}
        simpleObjectTypeMap.put("byte[]", new QName(SCHEMA_NAMESPACE, "base64"));
        simpleObjectTypeMap.put("org.jibx.runtime.QName", new QName(SCHEMA_NAMESPACE, "QName"));

        simplePrimitiveTypeMap.put("boolean", new QName(SCHEMA_NAMESPACE, "boolean"));
        simplePrimitiveTypeMap.put("byte", new QName(SCHEMA_NAMESPACE, "byte"));
        simplePrimitiveTypeMap.put("char", new QName(SCHEMA_NAMESPACE, "unsignedInt"));
        simplePrimitiveTypeMap.put("double", new QName(SCHEMA_NAMESPACE, "double"));
        simplePrimitiveTypeMap.put("float", new QName(SCHEMA_NAMESPACE, "float"));
        simplePrimitiveTypeMap.put("int", new QName(SCHEMA_NAMESPACE, "int"));
        simplePrimitiveTypeMap.put("long", new QName(SCHEMA_NAMESPACE, "long"));
        simplePrimitiveTypeMap.put("short", new QName(SCHEMA_NAMESPACE, "short"));

        simpleWrapperMap.put("boolean", Boolean.TYPE);
        simpleWrapperMap.put("byte", Byte.TYPE);
        simpleWrapperMap.put("char", Character.TYPE);
        simpleWrapperMap.put("double", Double.TYPE);
        simpleWrapperMap.put("float", Float.TYPE);
        simpleWrapperMap.put("int", Integer.TYPE);
        simpleWrapperMap.put("long", Long.TYPE);
        simpleWrapperMap.put("short", Short.TYPE);

        buildFormat("byte", "byte", "serializeByte", "parseByte", "0", simpleTypeMap);
        buildFormat("unsignedShort", "char", "serializeChar", "parseChar", "0", simpleTypeMap);
        buildFormat("double", "double", "serializeDouble", "parseDouble", "0.0", simpleTypeMap);
        buildFormat("float", "float", "serializeFloat", "parseFloat", "0.0", simpleTypeMap);
        buildFormat("int", "int", "serializeInt", "parseInt", "0", simpleTypeMap);
        buildFormat("long", "long", "serializeLong", "parseLong", "0", simpleTypeMap);
        buildFormat("short", "short", "serializeShort", "parseShort", "0", simpleTypeMap);
        buildFormat("boolean", "boolean", "serializeBoolean", "parseBoolean", "false", simpleTypeMap);
        buildFormat("dateTime", "java.util.Date", "serializeDateTime", "deserializeDateTime", null,
                    simpleTypeMap);
        buildFormat("date", "java.sql.Date", "serializeSqlDate", "deserializeSqlDate", null, simpleTypeMap);
        buildFormat("time", "java.sql.Time", "serializeSqlTime", "deserializeSqlTime", null, simpleTypeMap);
        buildFormat("base64Binary", "byte[]", "serializeBase64", "deserializeBase64", null, simpleTypeMap);
        buildFormat("string", "java.lang.String", null, null, null, simpleTypeMap);
    }

    private JibxUtil() {
    }

    private static void buildFormat(final String stype, final String jtype, final String sname,
                                    final String dname, final String dflt, final Map<QName, Format> map) {
        Format format = new Format();
        format.setTypeName(jtype);
        format.setSerializeMethod(sname);
        format.setDeserializeMethod(dname);
        format.setDefaultValue(dflt);
        map.put(new QName(SCHEMA_NAMESPACE, stype), format);
    }

    public static Format getFormatElement(final QName type) {
        return simpleTypeMap.get(type);
    }

    public static Object toObject(final String text, final QName stype) {
        Format format = simpleTypeMap.get(stype);
        if (format != null) {
            String deserializerMethod = format.getDeserializeMethod();
            if (deserializerMethod != null) {
                try {
                    Method method = UTILITY.getMethod(deserializerMethod, INPUT_TYPE);
                    return method.invoke(null, new Object[] {
                        text
                    });
                } catch (Exception e) {
                    throw new RuntimeException("", e);
                }
            }
        }
        return text;
    }

    public static String toText(final Object value, final QName stype) {
        Format format = simpleTypeMap.get(stype);
        if (format != null) {
            String serializeMethod = format.getSerializeMethod();
            if (serializeMethod != null) {
                String jtype = format.getTypeName();
                Class<?>[] paraTypes = (JibxUtil.isPrimitiveType(jtype)) ? new Class[] {
                    JibxUtil.getPrimitiveType(jtype)
                } : new Class[] {
                    value.getClass()
                };
                try {
                    Method method = UTILITY.getMethod(serializeMethod, paraTypes);
                    return method.invoke(null, new Object[] {
                        value
                    }).toString();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return value.toString();
    }

    public static boolean isSimpleValue(final String type) {
        return simplePrimitiveTypeMap.containsKey(type) || simpleObjectTypeMap.containsKey(type)
               || "void".equals(type);
    }

    public static boolean isSimpleValue(final Class type) {
        return isSimpleValue(type.getName());
    }

    public static QName getSchemaType(final String jtype) {
        QName stype = (QName)simplePrimitiveTypeMap.get(jtype);
        if (stype == null) {
            stype = (QName)simpleObjectTypeMap.get(jtype);
        }
        return stype;
    }

    public static boolean isPrimitiveType(final String type) {
        return simpleWrapperMap.containsKey(type);
    }

    public static Class<?> getPrimitiveType(final String type) {
        return simpleWrapperMap.get(type);
    }

    public static QName getQName(String qname) {
        String ns = qname.substring(1, qname.indexOf("}"));
        String localName = qname.substring(qname.indexOf("}") + 2);
        return new QName(ns, localName);
    }

    static class Format {

        private String typeName;
        private String deserializeMethod;
        private String serializeMethod;
        private String defaultValue;

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(final String typeName) {
            this.typeName = typeName;
        }

        public String getDeserializeMethod() {
            return deserializeMethod;
        }

        public void setDeserializeMethod(final String deserializeMethod) {
            this.deserializeMethod = deserializeMethod;
        }

        public String getSerializeMethod() {
            return serializeMethod;
        }

        public void setSerializeMethod(final String serializeMethod) {
            this.serializeMethod = serializeMethod;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(final String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }
}
