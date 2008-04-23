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

package org.apache.cxf.binding.corba.wsdl;

import javax.xml.namespace.QName;

public interface W3CConstants {

    // XML Namespaces
    String NP_XMLNS = "xmlns";
    String NU_XMLNS = "http://www.w3.org/2000/xmlns/";    

    // XML Schema(CR) datatypes + structures
    String NP_SCHEMA_XSD = "xsd";
    String NU_SCHEMA_XSD = "http://www.w3.org/2001/XMLSchema";

    // XML Schema instance
    String NP_SCHEMA_XSI = "xsi";
    String NU_SCHEMA_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    String A_XSI_TYPE = "type";

    // XML Constants
    String USE_OPTIONAL = "optional";
    String USE_PROHIBITED = "prohibited";
    String USE_REQUIRED = "required";
    String ELEMENT_FORM_DEFAULT = "elementFormDefault";
    String ATTRIBUTE_FORM_DEFAULT = "attributeFormDefault";
    String QUALIFIED = "qualified";
    String UNQUALIFIED = "unqualified";

    // XML Schema attribute names
    QName NA_XSI_TYPE = new QName(NU_SCHEMA_XSI, A_XSI_TYPE);

    // XML Schema element names
    QName NE_SCHEMA_LIST = new QName(NU_SCHEMA_XSD, "list", NP_SCHEMA_XSD);
    QName NE_SCHEMA_UNION = new QName(NU_SCHEMA_XSD, "union", NP_SCHEMA_XSD);
    QName NE_SCHEMA_SCHEMA = new QName(NU_SCHEMA_XSD, "schema", NP_SCHEMA_XSD);
    QName NE_SCHEMA_ELEMENT = new QName(NU_SCHEMA_XSD, "element", NP_SCHEMA_XSD);
    QName NE_SCHEMA_COMPLEXTYPE = new QName(NU_SCHEMA_XSD, "complexType", NP_SCHEMA_XSD);
    QName NE_SCHEMA_SIMPLETYPE = new QName(NU_SCHEMA_XSD, "simpleType", NP_SCHEMA_XSD);
    QName NE_SCHEMA_ANY = new QName(NU_SCHEMA_XSD, "any", NP_SCHEMA_XSD);

    QName NE_SCHEMA_SEQUENCE = new QName(NU_SCHEMA_XSD, "sequence", NP_SCHEMA_XSD);
    QName NE_SCHEMA_ALL = new QName(NU_SCHEMA_XSD, "all", NP_SCHEMA_XSD);
    QName NE_SCHEMA_CHOICE = new QName(NU_SCHEMA_XSD, "choice", NP_SCHEMA_XSD);

    QName NE_SCHEMA_IMPORT = new QName(NU_SCHEMA_XSD, "import", NP_SCHEMA_XSD);
    QName NE_SCHEMA_INCLUDE = new QName(NU_SCHEMA_XSD, "include", NP_SCHEMA_XSD);

    QName NE_SCHEMA_ANNOTATION = new QName(NU_SCHEMA_XSD, "annotation", NP_SCHEMA_XSD);
    QName NE_SCHEMA_DOCUMENTATION = new QName(NU_SCHEMA_XSD, "documentation", NP_SCHEMA_XSD);
    QName NE_SCHEMA_APPINFO = new QName(NU_SCHEMA_XSD, "appinfo", NP_SCHEMA_XSD);

    QName NE_SCHEMA_COMPLEX_CONTENT = new QName(NU_SCHEMA_XSD, "complexContent", NP_SCHEMA_XSD);
    QName NE_SCHEMA_SIMPLE_CONTENT = new QName(NU_SCHEMA_XSD, "simpleContent", NP_SCHEMA_XSD);
    QName NE_SCHEMA_RESTRICTION = new QName(NU_SCHEMA_XSD, "restriction", NP_SCHEMA_XSD);
    QName NE_SCHEMA_ENUMERATION = new QName(NU_SCHEMA_XSD, "enumeration", NP_SCHEMA_XSD);
    QName NE_SCHEMA_EXTENSION = new QName(NU_SCHEMA_XSD, "extension", NP_SCHEMA_XSD);
    QName NE_SCHEMA_ATTRIBUTE = new QName(NU_SCHEMA_XSD, "attribute", NP_SCHEMA_XSD);
    QName NE_SCHEMA_ANY_ATTRIBUTE = new QName(NU_SCHEMA_XSD, "anyAttribute", NP_SCHEMA_XSD);
    QName NE_SCHEMA_ATTRIBUTEGROUP = new QName(NU_SCHEMA_XSD, "attributeGroup", NP_SCHEMA_XSD);

    QName NE_SCHEMA_GROUP = new QName(NU_SCHEMA_XSD, "group", NP_SCHEMA_XSD);
    
//  XML Schema primitive and derived built-in types
    // Primitives
    QName NT_SCHEMA_STRING = new QName(NU_SCHEMA_XSD, "string", NP_SCHEMA_XSD);
    QName NT_SCHEMA_BOOLEAN = new QName(NU_SCHEMA_XSD, "boolean", NP_SCHEMA_XSD);
    QName NT_SCHEMA_FLOAT = new QName(NU_SCHEMA_XSD, "float", NP_SCHEMA_XSD);
    QName NT_SCHEMA_DOUBLE = new QName(NU_SCHEMA_XSD, "double", NP_SCHEMA_XSD);
    QName NT_SCHEMA_DECIMAL = new QName(NU_SCHEMA_XSD, "decimal", NP_SCHEMA_XSD);
    QName NT_SCHEMA_DUR = new QName(NU_SCHEMA_XSD, "duration", NP_SCHEMA_XSD);
    QName NT_SCHEMA_DATETIME = new QName(NU_SCHEMA_XSD, "dateTime", NP_SCHEMA_XSD);
    QName NT_SCHEMA_TIME = new QName(NU_SCHEMA_XSD, "time", NP_SCHEMA_XSD);
    QName NT_SCHEMA_DATE = new QName(NU_SCHEMA_XSD, "date", NP_SCHEMA_XSD);
    QName NT_SCHEMA_GYMON = new QName(NU_SCHEMA_XSD, "gYearMonth", NP_SCHEMA_XSD);
    QName NT_SCHEMA_GYEAR = new QName(NU_SCHEMA_XSD, "gYear", NP_SCHEMA_XSD);
    QName NT_SCHEMA_GMDAY = new QName(NU_SCHEMA_XSD, "gMonthDay", NP_SCHEMA_XSD);
    QName NT_SCHEMA_GDAY = new QName(NU_SCHEMA_XSD, "gDay", NP_SCHEMA_XSD);
    QName NT_SCHEMA_GMONTH = new QName(NU_SCHEMA_XSD, "gMonth", NP_SCHEMA_XSD);
    QName NT_SCHEMA_HBIN = new QName(NU_SCHEMA_XSD, "hexBinary", NP_SCHEMA_XSD);
    QName NT_SCHEMA_BASE64 = new QName(NU_SCHEMA_XSD, "base64Binary", NP_SCHEMA_XSD);
    QName NT_SCHEMA_AURI = new QName(NU_SCHEMA_XSD, "anyURI", NP_SCHEMA_XSD);
    QName NT_SCHEMA_QNAME = new QName(NU_SCHEMA_XSD, "QName", NP_SCHEMA_XSD);
    QName NT_SCHEMA_NOTATION = new QName(NU_SCHEMA_XSD, "NOTATION", NP_SCHEMA_XSD);

    // DerivedTypes
    QName NT_SCHEMA_NSTRING = new QName(NU_SCHEMA_XSD, "normalizedString", NP_SCHEMA_XSD);
    QName NT_SCHEMA_TOKEN = new QName(NU_SCHEMA_XSD, "token", NP_SCHEMA_XSD);
    QName NT_SCHEMA_LANG = new QName(NU_SCHEMA_XSD, "language", NP_SCHEMA_XSD);
    QName NT_SCHEMA_NMTOKEN = new QName(NU_SCHEMA_XSD, "NMTOKEN", NP_SCHEMA_XSD);
    QName NT_SCHEMA_NMTOKENS = new QName(NU_SCHEMA_XSD, "NMTOKENS", NP_SCHEMA_XSD);
    QName NT_SCHEMA_NAME = new QName(NU_SCHEMA_XSD, "Name", NP_SCHEMA_XSD);
    QName NT_SCHEMA_NCNAME = new QName(NU_SCHEMA_XSD, "NCName", NP_SCHEMA_XSD);
    QName NT_SCHEMA_ID = new QName(NU_SCHEMA_XSD, "ID", NP_SCHEMA_XSD);
    QName NT_SCHEMA_IDREF = new QName(NU_SCHEMA_XSD, "IDREF", NP_SCHEMA_XSD);
    QName NT_SCHEMA_IDREFS = new QName(NU_SCHEMA_XSD, "IDREFS", NP_SCHEMA_XSD);
    QName NT_SCHEMA_ENTITY = new QName(NU_SCHEMA_XSD, "ENTITY", NP_SCHEMA_XSD);
    QName NT_SCHEMA_ENTITIES = new QName(NU_SCHEMA_XSD, "ENTITIES", NP_SCHEMA_XSD);
    QName NT_SCHEMA_INTEGER = new QName(NU_SCHEMA_XSD, "integer", NP_SCHEMA_XSD);
    QName NT_SCHEMA_NPINT = new QName(NU_SCHEMA_XSD, "nonPositiveInteger", NP_SCHEMA_XSD);
    QName NT_SCHEMA_NINT = new QName(NU_SCHEMA_XSD, "negativeInteger", NP_SCHEMA_XSD);
    QName NT_SCHEMA_LONG = new QName(NU_SCHEMA_XSD, "long", NP_SCHEMA_XSD);
    QName NT_SCHEMA_INT = new QName(NU_SCHEMA_XSD, "int", NP_SCHEMA_XSD);
    QName NT_SCHEMA_SHORT = new QName(NU_SCHEMA_XSD, "short", NP_SCHEMA_XSD);
    QName NT_SCHEMA_BYTE = new QName(NU_SCHEMA_XSD, "byte", NP_SCHEMA_XSD);
    QName NT_SCHEMA_NNINT = new QName(NU_SCHEMA_XSD, "nonNegativeInteger", NP_SCHEMA_XSD);
    QName NT_SCHEMA_ULONG = new QName(NU_SCHEMA_XSD, "unsignedLong", NP_SCHEMA_XSD);
    QName NT_SCHEMA_UINT = new QName(NU_SCHEMA_XSD, "unsignedInt", NP_SCHEMA_XSD);
    QName NT_SCHEMA_USHORT = new QName(NU_SCHEMA_XSD, "unsignedShort", NP_SCHEMA_XSD);
    QName NT_SCHEMA_UBYTE = new QName(NU_SCHEMA_XSD, "unsignedByte", NP_SCHEMA_XSD);
    QName NT_SCHEMA_PINT = new QName(NU_SCHEMA_XSD, "positiveInteger", NP_SCHEMA_XSD);

    QName NT_SCHEMA_ANYTYPE = new QName(NU_SCHEMA_XSD, "anyType", NP_SCHEMA_XSD);
    QName NT_SCHEMA_ANYSIMPLETYPE = new QName(NU_SCHEMA_XSD, "anySimpleType", NP_SCHEMA_XSD);
    QName NT_SCHEMA_ANY = new QName(NU_SCHEMA_XSD, "any", NP_SCHEMA_XSD); 

    String getValue(String value);
}

