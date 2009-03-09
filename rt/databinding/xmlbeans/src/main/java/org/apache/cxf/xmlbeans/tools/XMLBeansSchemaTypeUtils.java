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

package org.apache.cxf.xmlbeans.tools;

import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.SchemaType;

/**
 * This class will help us to map the 
 * <a href="http://xmlbeans.apache.org/docs/2.0.0/guide/conXMLBeansSupportBuiltInSchemaTypes.html">
 * XMLBeans Builtin Type</a> into Natural Java Type
 *
 * 
 */
public final class XMLBeansSchemaTypeUtils {
    private static final Map<String, String> BUILTIN_TYPES_MAP;
    static {
        BUILTIN_TYPES_MAP = new HashMap<String, String>();
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlObject", "org.apache.xmlbeans.XmlObject");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlAnySimpleType", "String");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlAnyURI", "String");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlBase64Binary", "byte[]");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlBoolean", "boolean");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlByte", "byte");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlDate", "java.util.Calendar");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlDateTime", "java.util.Calendar");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlDecimal", "java.math.BigDecimal");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlDouble", "double");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlDuration", "org.apache.xmlbeans.GDuration");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlENTITIES", "String");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlENTITY", "String");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlFloat", "float");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlGDay", "java.util.Calendar");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlGMonth", "java.util.Calendar");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlGMonthDay", "java.util.Calendar");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlGYear", "java.util.Calendar");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlGYearMonth", "java.util.Calendar");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlHexBinary", "byte[]");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlID", "String");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlIDREF", "String");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlIDREFS", "String");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlInt", "int");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlInteger", "java.math.BigInteger");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlLanguage", "String");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlLong", "long");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlName", "String");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlNCNAME", "String");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlNegativeInteger", "java.math.BigInteger");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlNMTOKEN", "String");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlNMTOKENS", "String");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlNonNegativeInteger", "java.math.BigInteger");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlNonPositiveInteger", "java.math.BigInteger");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlNormalizedString", "String");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlNOTATION", "org.apache.xmlbeans.XmlNOTATION");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlPositiveInteger", "java.math.BigInteger");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlQName", "javax.xml.namespace.QName");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlShort", "short");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlString", "String");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlTime", "java.util.Calendar");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlToken", "String");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlUnsignedByte", "short");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlUnsignedInt", "long");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlUnsignedLong", "java.math.BigInteger");
        BUILTIN_TYPES_MAP.put("org.apache.xmlbeans.XmlUnsignedShort", "int");
    }
    
    private XMLBeansSchemaTypeUtils() {
        // helper class
    }
    
    public static String getNaturalJavaClassName(SchemaType st) {
        SchemaType schemaType = st;
        String result = null;
        if (st.isSimpleType() && !st.isBuiltinType()) {
            schemaType = st.getBaseType();
            while (schemaType != null && !schemaType.isBuiltinType()) {
                schemaType = schemaType.getBaseType();
            }
        }
        if (schemaType.isBuiltinType()) {
            result = BUILTIN_TYPES_MAP.get(schemaType.getFullJavaName());
        } else {
            result = schemaType.getFullJavaName().replace('$', '.');
        }        
        return result;
        
    }

}
