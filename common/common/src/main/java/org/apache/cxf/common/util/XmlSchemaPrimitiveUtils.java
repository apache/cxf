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

package org.apache.cxf.common.util;

import java.util.HashMap;
import java.util.Map;

public final class XmlSchemaPrimitiveUtils {
    private static final String SCHEMA_NS_PREFIX = "xs";
    private static final Map<Class<?>, String> XML_SCHEMA_PRIMITIVE_MAP = new HashMap<Class<?>, String>();
    static {
        initializeMap();
    }
    private XmlSchemaPrimitiveUtils() {
    }
    
    private static void initializeMap() {
        registerPrimitiveClasses("int", Integer.class, int.class);
        registerPrimitiveClasses("byte", Byte.class, byte.class);
        registerPrimitiveClasses("boolean", Boolean.class, boolean.class);
        registerPrimitiveClasses("long", Long.class, long.class);
        registerPrimitiveClasses("float", Float.class, float.class);
        registerPrimitiveClasses("double", Double.class, double.class);
        registerPrimitiveClasses("string", String.class);
        // add more as needed
    }
    
    private static void registerPrimitiveClasses(String value, Class<?> ... classes) {
        for (Class<?> cls : classes) {
            XML_SCHEMA_PRIMITIVE_MAP.put(cls, value);
        }
    }
    
    public static String getSchemaRepresentation(Class<?> type) {
        return getSchemaRepresentation(type, SCHEMA_NS_PREFIX);
    }
    
    public static String getSchemaRepresentation(Class<?> type, String xsdPrefix) {
        String value =  XML_SCHEMA_PRIMITIVE_MAP.get(type);
        return value == null ? value : xsdPrefix + ":" + value;
    }
    
}
