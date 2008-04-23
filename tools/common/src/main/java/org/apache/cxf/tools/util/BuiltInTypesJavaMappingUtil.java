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

package org.apache.cxf.tools.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class BuiltInTypesJavaMappingUtil {
    private static final String XML_SCHEMA_NS = "http://www.w3.org/2000/10/XMLSchema";
    private static final String NS_XMLNS = "http://www.w3.org/2000/xmlns/";
    private static final String NS_XSD = "http://www.w3.org/2001/XMLSchema";
    private static final String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    
    private static Set<String> nameSpaces = new HashSet<String>();
    static {
        nameSpaces.add(XML_SCHEMA_NS.toLowerCase());
        nameSpaces.add(NS_XMLNS.toLowerCase());
        nameSpaces.add(NS_XSD.toLowerCase());
        nameSpaces.add(NS_XSI.toLowerCase());
    }
    
    private static Map<String, String> jTypeMapping = new HashMap<String, String>();
    static {
        jTypeMapping.put("string", "java.lang.String");
        jTypeMapping.put("integer", "java.math.BigInteger");
        jTypeMapping.put("int", "int");
        jTypeMapping.put("long", "long");
        jTypeMapping.put("short", "short");
        jTypeMapping.put("decimal", "java.math.BigDecimal");
        jTypeMapping.put("float", "float");
        jTypeMapping.put("double", "double");
        jTypeMapping.put("boolean", "boolean");
        jTypeMapping.put("byte", "byte");
        jTypeMapping.put("qname", "javax.xml.namespace.QName");
        jTypeMapping.put("dataTime", "javax.xml.datatype.XMLGregorianCalendar");
        jTypeMapping.put("time", "javax.xml.datatype.XMLGregorianCalendar");
        jTypeMapping.put("date", "javax.xml.datatype.XMLGregorianCalendar");
        jTypeMapping.put("dataTime", "javax.xml.datatype.XMLGregorianCalendar");
        jTypeMapping.put("gday", "javax.xml.datatype.XMLGregorianCalendar");
        jTypeMapping.put("gmonth", "javax.xml.datatype.XMLGregorianCalendar");
        jTypeMapping.put("gyear", "javax.xml.datatype.XMLGregorianCalendar");
        jTypeMapping.put("gmonthday", "javax.xml.datatype.XMLGregorianCalendar");       
        jTypeMapping.put("base64binary", "byte[]");
        jTypeMapping.put("hexbinary", "byte[]");
        jTypeMapping.put("unsignedint", "long");
        jTypeMapping.put("unsignedshort", "int");
        jTypeMapping.put("unsignedbyte", "short");
        jTypeMapping.put("anytype", "Object");
        
    }
    private BuiltInTypesJavaMappingUtil() {
    }

    public static String getJType(String nameSpace, String type) {
        if (type == null || nameSpace == null || !nameSpaces.contains(nameSpace.toLowerCase())) {
            return null;
        }
        return jTypeMapping.get(type.toLowerCase());
    }

}
