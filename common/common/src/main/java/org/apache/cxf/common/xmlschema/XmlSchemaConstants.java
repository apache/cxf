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

package org.apache.cxf.common.xmlschema;

import javax.xml.namespace.QName;

/**
 * This class holds constants related to XML Schema. Over time, some of the contents
 * of WSDLConstants should move here.
 */
public final class XmlSchemaConstants {
    
    public static final String XSD_NAMESPACE_URI = "http://www.w3.org/2001/XMLSchema";
    public static final QName ANY_TYPE_QNAME = new QName(XSD_NAMESPACE_URI, "anyType");
    public static final QName ANY_URI_QNAME = new QName(XSD_NAMESPACE_URI, "anyURI");
    public static final QName BASE64BINARY_QNAME = new QName(XSD_NAMESPACE_URI, "base64Binary");
    public static final QName BOOLEAN_QNAME = new QName(XSD_NAMESPACE_URI, "boolean");
    public static final QName BYTE_QNAME = new QName(XSD_NAMESPACE_URI, "byte");
    public static final QName DATE_QNAME = new QName(XSD_NAMESPACE_URI, "date");
    public static final QName DATETIME_QNAME = new QName(XSD_NAMESPACE_URI, "dateTime");
    public static final QName DOUBLE_QNAME = new QName(XSD_NAMESPACE_URI, "double");
    public static final QName DURATION_QNAME = new QName(XSD_NAMESPACE_URI, "duration");
    public static final QName ENTITIES_QNAME = new QName(XSD_NAMESPACE_URI, "ENTITIES");
    public static final QName ENTITY_QNAME = new QName(XSD_NAMESPACE_URI, "ENTITY");
    public static final QName FLOAT_QNAME = new QName(XSD_NAMESPACE_URI, "float");
    public static final QName GDAY_QNAME = new QName(XSD_NAMESPACE_URI, "gDay");
    public static final QName GMONTH_QNAME = new QName(XSD_NAMESPACE_URI, "gMonth");
    public static final QName GMONTHDAY_QNAME = new QName(XSD_NAMESPACE_URI, "gMonthDay");
    public static final QName GYEAR_QNAME = new QName(XSD_NAMESPACE_URI, "gYear");
    public static final QName GYEARMONTH_QNAME = new QName(XSD_NAMESPACE_URI, "gYearMonth");
    public static final QName HEX_BINARY_QNAME = new QName(XSD_NAMESPACE_URI, "hexBinary");
    public static final QName ID_QNAME = new QName(XSD_NAMESPACE_URI, "ID");
    public static final QName IDREF_QNAME = new QName(XSD_NAMESPACE_URI, "IDREF");
    public static final QName IDREFS_QNAME = new QName(XSD_NAMESPACE_URI, "IDREFS");
    public static final QName INT_QNAME = new QName(XSD_NAMESPACE_URI, "int");
    public static final QName INTEGER_QNAME = new QName(XSD_NAMESPACE_URI, "integer");
    public static final QName LANGUAGE_QNAME = new QName(XSD_NAMESPACE_URI, "language");
    public static final QName LONG_QNAME = new QName(XSD_NAMESPACE_URI, "long");
    public static final QName NAME_QNAME = new QName(XSD_NAMESPACE_URI, "Name");
    public static final QName NCNAME_QNAME = new QName(XSD_NAMESPACE_URI, "NCName");
    public static final QName NEGATIVEINTEGER_QNAME = new QName(XSD_NAMESPACE_URI, "negativeInteger");
    public static final QName NMTOKEN_QNAME = new QName(XSD_NAMESPACE_URI, "NMTOKEN");
    public static final QName NMTOKENS_QNAME = new QName(XSD_NAMESPACE_URI, "NMTOKENS");
    public static final QName NONNEGATIVEINTEGER_QNAME = new QName(XSD_NAMESPACE_URI, "nonNegativeInteger");
    public static final QName NONPOSITIVEINTEGER_QNAME = new QName(XSD_NAMESPACE_URI, "nonPositiveInteger");
    public static final QName NORMALIZEDSTRING_QNAME = 
        new QName(XSD_NAMESPACE_URI, "normalizedStringInteger");
    public static final QName NOTATION_QNAME = new QName(XSD_NAMESPACE_URI, "NOTATION");
    public static final QName POSITIVEINTEGER_QNAME = new QName(XSD_NAMESPACE_URI, "positiveInteger");
    public static final QName QNAME_QNAME = new QName(XSD_NAMESPACE_URI, "QName");
    public static final QName SHORT_QNAME = new QName(XSD_NAMESPACE_URI, "short");
    public static final QName STRING_QNAME = new QName(XSD_NAMESPACE_URI, "string");
    public static final QName TIME_QNAME = new QName(XSD_NAMESPACE_URI, "time");
    public static final QName TOKEN_QNAME = new QName(XSD_NAMESPACE_URI, "token");
    public static final QName UNSIGNEDBYTE_QNAME = new QName(XSD_NAMESPACE_URI, "unsignedByte");
    public static final QName UNSIGNEDINT_QNAME = new QName(XSD_NAMESPACE_URI, "unsignedInt");
    public static final QName UNSIGNEDLONG_QNAME = new QName(XSD_NAMESPACE_URI, "unsignedLong");
    public static final QName UNSIGNEDSHORT_QNAME = new QName(XSD_NAMESPACE_URI, "unsignedShort");

    private XmlSchemaConstants() {
        //utility class
    }
    
}
