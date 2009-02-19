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

package org.apache.cxf.common;

import javax.xml.namespace.QName;

public final class WSDLConstants {

    public static final String WSDL_PREFIX = "wsdl";
    public static final String NS_WSDL11 = "http://schemas.xmlsoap.org/wsdl/";
    

    public static final String NP_XMLNS = "xmlns";
    public static final String NS_XMLNS = "http://www.w3.org/2000/xmlns/";

    // XML Schema (CR) datatypes + structures
    public static final String NP_SCHEMA_XSD = "xsd";
    public static final String NS_SCHEMA_XSD = "http://www.w3.org/2001/XMLSchema";

    public static final QName QNAME_SCHEMA = new QName(NS_SCHEMA_XSD, "schema");

    // XML Schema instance
    public static final String NP_SCHEMA_XSI = "xsi";
    public static final String NS_SCHEMA_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    
    public static final String A_XSI_TYPE = "type";
    public static final String A_XSI_NIL = "nil";
    
    // XML Schema attribute names
    public static final QName NA_XSI_TYPE = new QName(NP_SCHEMA_XSI, A_XSI_TYPE, NS_SCHEMA_XSI);
    public static final QName NA_XSI_NIL = new QName(NP_SCHEMA_XSI, A_XSI_NIL, NS_SCHEMA_XSI);



    public static final String NS_SOAP = "http://schemas.xmlsoap.org/wsdl/soap/";
    public static final String NS_SOAP11 = NS_SOAP;
    public static final String NS_SOAP12 = "http://schemas.xmlsoap.org/wsdl/soap12/";
    public static final String SOAP11_PREFIX = "soap";
    public static final String SOAP12_PREFIX = "soap12";
    
    public static final String NS_SOAP11_HTTP_TRANSPORT = "http://schemas.xmlsoap.org/soap/http";
    
    public static final QName QNAME_SOAP_BINDING = new QName(NS_SOAP, "binding");
    public static final QName QNAME_SOAP_OPERATION = new QName(NS_SOAP, "operation");
    public static final QName QNAME_SOAP_BODY = new QName(NS_SOAP, "body");
    public static final QName QNAME_SOAP_FAULT = new QName(NS_SOAP, "fault");
    public static final QName QNAME_SOAP_BINDING_ADDRESS = new QName(NS_SOAP, "address");


    public static final String NS_SOAP12_HTTP_TRANSPORT = "http://www.w3.org/2003/05/soap/bindings/HTTP/";
    
    public static final QName QNAME_SOAP12_BINDING = new QName(NS_SOAP12, "binding");
    public static final QName QNAME_SOAP12_BINDING_ADDRESS = new QName(NS_SOAP12, "address");
    

    public static final String DOCUMENT = "document";
    public static final String RPC = "rpc";
    public static final String LITERAL = "literal";
    public static final String REPLACE_WITH_ACTUAL_URL = "REPLACE_WITH_ACTUAL_URL";

    public static final String JMS_PREFIX = "jms";
    public static final String TNS_PREFIX = "tns";

    // WSDL 1.1 definitions
    public static final QName QNAME_BINDING = new QName(NS_WSDL11, "binding");
    public static final QName QNAME_DEFINITIONS = new QName(NS_WSDL11, "definitions");
    public static final QName QNAME_DOCUMENTATION = new QName(NS_WSDL11, "documentation");
    public static final QName QNAME_IMPORT = new QName(NS_WSDL11, "import");
    public static final QName QNAME_MESSAGE = new QName(NS_WSDL11, "message");
    public static final QName QNAME_PART = new QName(NS_WSDL11, "part");
    public static final QName QNAME_OPERATION = new QName(NS_WSDL11, "operation");
    public static final QName QNAME_INPUT = new QName(NS_WSDL11, "input");
    public static final QName QNAME_OUTPUT = new QName(NS_WSDL11, "output");

    public static final QName QNAME_PORT = new QName(NS_WSDL11, "port");
    public static final QName QNAME_ADDRESS = new QName(NS_WSDL11, "address");
    public static final QName QNAME_PORT_TYPE = new QName(NS_WSDL11, "portType");
    public static final QName QNAME_FAULT = new QName(NS_WSDL11, "fault");
    public static final QName QNAME_SERVICE = new QName(NS_WSDL11, "service");
    public static final QName QNAME_TYPES = new QName(NS_WSDL11, "types");

    // WSDL Validation
    public static final String ATTR_PART_ELEMENT = "element";
    public static final String ATTR_PART_TYPE = "type";
    public static final String ATTR_TYPE = "type";

    //For Stax2DOM getUserData(location)
    public static final String NODE_LOCATION = "location";

    public static final int DOC_WRAPPED = 1;
    public static final int DOC_BARE = 2;
    public static final int RPC_WRAPPED = 3;
    public static final int ERORR_STYLE_USE = -1;

    public static final String NS_BINDING_XML = "http://cxf.apache.org/bindings/xformat";
    public static final QName QNAME_XMLHTTP_BINDING_ADDRESS = 
        new QName("http://schemas.xmlsoap.org/wsdl/http/", "address");
    
    public static final String ATTR_TRANSPORT = "transport";
    public static final String ATTR_LOCATION = "location";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_NAMESPACE = "namespace";
    public static final String ATTR_TNS = "targetNamespace";
    // usual prefix for the targetNamespace.
    public static final String CONVENTIONAL_TNS_PREFIX = "tns";
    
    public static final String WSDL11 = "1.1";
    public static final String WSDL20 = "2.0";

    public enum WSDLVersion {
        WSDL11,
        WSDL20,
        UNKNOWN
    };

    private WSDLConstants() {
    }
    
    public static WSDLVersion getVersion(String version) {
        if (WSDL11.equals(version)) {
            return WSDLVersion.WSDL11;
        }
        if (WSDL20.equals(version)) {
            return WSDLVersion.WSDL20;
        }
        return WSDLVersion.UNKNOWN;
    }
    
}
