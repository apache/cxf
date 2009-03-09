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
 * This class will help us to map the XMLBeansBuildinType into Natural Java Type
 */
public final class XMLBeansSchemaTypeUtils {
    private static final Map<String, String> BUILDIN_TYPES_MAP;
    static {
        BUILDIN_TYPES_MAP = new HashMap<String, String>();
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlObject", "org.apache.xmlbeans.XmlObject");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlAnySimpleType", "String");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlAnyURI", "String");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlBase64Binary", "byte[]");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlBoolean", "boolean");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlByte", "byte");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlDate", "java.util.Calendar");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlDateTime", "java.util.Calendar");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlDecimal", "java.math.BigDecimal");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlDouble", "double");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlDuration", "org.apache.xmlbeans.GDuration");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlENTITIES", "String");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlENTITY", "String");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlFloat", "float");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlGDay", "java.util.Calendar");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlGMonth", "java.util.Calendar");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlGMonthDay", "java.util.Calendar");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlGYear", "java.util.Calendar");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlHexBinary", "java.util.Calendar");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlHexBinary", "byte[]");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlID", "String");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlIDREF", "String");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlIDREFS", "String");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlInt", "int");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlInteger", "java.math.BigInteger");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlLanguage", "String");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlLong", "long");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlName", "String");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlNCNAME", "String");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlNegativeInteger", "java.math.BigInteger");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlNMTOKEN", "String");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlNMTOKENS", "String");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlNonNegativeInteger", "java.math.BigInteger");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlNonPositiveInteger", "java.math.BigInteger");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlNormalizedString", "String");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlNOTATION", "org.apache.xmlbeans.XmlNOTATION");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlPositiveInteger", "java.math.BigInteger");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlQName", "javax.xml.namespace.QName");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlShort", "short");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlString", "String");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlTime", "java.util.Calendar");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlToken", "String");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlUnsignedByte", "short");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlUnsignedInt", "long");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlUnsignedLong", "java.math.BigInteger");
        BUILDIN_TYPES_MAP.put("org.apache.xmlbeans.XmlUnsignedShort", "int");
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
            result = BUILDIN_TYPES_MAP.get(schemaType.getFullJavaName());
        } else {
            result = schemaType.getFullJavaName().replace('$', '.');
        }        
        return result;
        
    }

}
