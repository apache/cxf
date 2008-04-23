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

public interface CorbaConstants {

    String IDL_VERSION = ":1.0";
    String REPO_STRING = "IDL:";

    String NS_CORBA_TYPEMAP = "typemap";
    String NP_WSDL_CORBA = "corba";
    String NU_WSDL_CORBA = "http://cxf.apache.org/bindings/corba";
    String NP_TM_CORBA = "corbatm";
    String NU_TM_CORBA = "http://cxf.apache.org/bindings/corba/typemap/corba/";
    String SCHEMA_NS_URI = "http://cxf.apache.org/bindings/corba/idltypes/";
    String WSDL_NS_URI = "http://cxf.apache.org/bindings/corba/idl/";
    
    // CORBA Binding Extensibility Elements
    QName NE_CORBA_ADDRESS = new QName(NU_WSDL_CORBA, "address", NP_WSDL_CORBA);
    QName NE_CORBA_POLICY = new QName(NU_WSDL_CORBA, "policy", NP_WSDL_CORBA);
    QName NE_CORBA_BINDING = new QName(NU_WSDL_CORBA, "binding", NP_WSDL_CORBA);
    QName NE_CORBA_OPERATION = new QName(NU_WSDL_CORBA, "operation", NP_WSDL_CORBA);
    QName NE_CORBA_PARAM = new QName(NU_WSDL_CORBA, "param", NP_WSDL_CORBA);
    QName NE_CORBA_RETURN = new QName(NU_WSDL_CORBA, "return", NP_WSDL_CORBA);
    QName NE_CORBA_RAISES = new QName(NU_WSDL_CORBA, "raises", NP_WSDL_CORBA);

    // CORBA Type Mapping Extensiblity Elements
    QName NE_CORBA_TYPEMAPPING = new QName(NU_WSDL_CORBA, "typeMapping", NP_WSDL_CORBA);
    QName NE_CORBA_STRUCT = new QName(NU_WSDL_CORBA, "struct", NP_WSDL_CORBA);
    QName NE_CORBA_STRUCT_MEM = new QName(NU_WSDL_CORBA, "member", NP_WSDL_CORBA);
    QName NE_CORBA_UNION = new QName(NU_WSDL_CORBA, "union", NP_WSDL_CORBA);
    QName NE_CORBA_UNION_BRANCH = new QName(NU_WSDL_CORBA, "unionbranch", NP_WSDL_CORBA);
    QName NE_CORBA_UNION_CASE = new QName(NU_WSDL_CORBA, "case", NP_WSDL_CORBA);
    QName NE_CORBA_ALIAS = new QName(NU_WSDL_CORBA, "alias", NP_WSDL_CORBA);
    QName NE_CORBA_FIXED = new QName(NU_WSDL_CORBA, "fixed", NP_WSDL_CORBA);
    QName NE_CORBA_ANONFIXED = new QName(NU_WSDL_CORBA, "anonfixed", NP_WSDL_CORBA);
    QName NE_CORBA_CONST = new QName(NU_WSDL_CORBA, "const", NP_WSDL_CORBA);
    QName NE_CORBA_ENUM = new QName(NU_WSDL_CORBA, "enum", NP_WSDL_CORBA);
    QName NE_CORBA_ENUMERATOR = new QName(NU_WSDL_CORBA, "enumerator", NP_WSDL_CORBA);
    QName NE_CORBA_SEQUENCE = new QName(NU_WSDL_CORBA, "sequence", NP_WSDL_CORBA);
    QName NE_CORBA_ANONSEQUENCE = new QName(NU_WSDL_CORBA, "anonsequence", NP_WSDL_CORBA);
    QName NE_CORBA_ARRAY = new QName(NU_WSDL_CORBA, "array", NP_WSDL_CORBA);
    QName NE_CORBA_ANONARRAY = new QName(NU_WSDL_CORBA, "anonarray", NP_WSDL_CORBA);
    QName NE_CORBA_ANONSTRING = new QName(NU_WSDL_CORBA, "anonstring", NP_WSDL_CORBA);
    QName NE_CORBA_ANONWSTRING = new QName(NU_WSDL_CORBA, "anonwstring", NP_WSDL_CORBA);
    QName NE_CORBA_EXCEPTION = new QName(NU_WSDL_CORBA, "exception", NP_WSDL_CORBA);
    QName NE_CORBA_INTERFACE = new QName(NU_WSDL_CORBA, "object", NP_WSDL_CORBA);

    // CORBA Primitive Types
    QName NT_CORBA_LONG = new QName(NU_WSDL_CORBA, "long", NP_WSDL_CORBA);
    QName NT_CORBA_ULONG = new QName(NU_WSDL_CORBA, "ulong", NP_WSDL_CORBA);
    QName NT_CORBA_LONGLONG = new QName(NU_WSDL_CORBA, "longlong", NP_WSDL_CORBA);
    QName NT_CORBA_ULONGLONG = new QName(NU_WSDL_CORBA, "ulonglong", NP_WSDL_CORBA);
    QName NT_CORBA_SHORT = new QName(NU_WSDL_CORBA, "short", NP_WSDL_CORBA);
    QName NT_CORBA_USHORT = new QName(NU_WSDL_CORBA, "ushort", NP_WSDL_CORBA);
    QName NT_CORBA_FLOAT = new QName(NU_WSDL_CORBA, "float", NP_WSDL_CORBA);
    QName NT_CORBA_DOUBLE = new QName(NU_WSDL_CORBA, "double", NP_WSDL_CORBA);
    QName NT_CORBA_CHAR = new QName(NU_WSDL_CORBA, "char", NP_WSDL_CORBA);
    QName NT_CORBA_WCHAR = new QName(NU_WSDL_CORBA, "wchar", NP_WSDL_CORBA);
    QName NT_CORBA_BOOLEAN = new QName(NU_WSDL_CORBA, "boolean", NP_WSDL_CORBA);
    QName NT_CORBA_OCTET = new QName(NU_WSDL_CORBA, "octet", NP_WSDL_CORBA);
    QName NT_CORBA_STRING = new QName(NU_WSDL_CORBA, "string", NP_WSDL_CORBA);
    QName NT_CORBA_WSTRING = new QName(NU_WSDL_CORBA, "wstring", NP_WSDL_CORBA);
    QName NT_CORBA_ANY = new QName(NU_WSDL_CORBA, "any", NP_WSDL_CORBA);
    QName NT_CORBA_DATETIME = new QName(NU_WSDL_CORBA, "dateTime", NP_WSDL_CORBA);
    QName NT_CORBA_LONGDOUBLE = new QName(NU_WSDL_CORBA, "longdouble", NP_WSDL_CORBA);

    QName NT_CORBA_DATE = new QName(NU_WSDL_CORBA, "date", NP_WSDL_CORBA);
    QName NT_CORBA_TIME = new QName(NU_WSDL_CORBA, "time", NP_WSDL_CORBA);

    QName NT_CORBA_PINT = new QName(NU_WSDL_CORBA, "positiveInteger", NP_WSDL_CORBA);
    QName NT_CORBA_NPINT = new QName(NU_WSDL_CORBA, "nonPositiveInteger", NP_WSDL_CORBA);
    QName NT_CORBA_NINT = new QName(NU_WSDL_CORBA, "negativeInteger", NP_WSDL_CORBA);
    QName NT_CORBA_NNINT = new QName(NU_WSDL_CORBA, "nonNegativeInteger", NP_WSDL_CORBA);

    // CORBA Binding Attribute Constants
    String REPO_ID = "repositoryID";
    String NAME = "name";
    String IDLTYPE = "idltype";
    String MODE = "mode";
    String EXCEPTION = "exception";
    String SERVICE_ID = "serviceid";    
    String BASES = "bases";
    String CORBA_ENDPOINT_OBJECT = "endpoint";
    String ORB = "orb";

    String getValue(String value);

}
