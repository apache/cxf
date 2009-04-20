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

package org.apache.cxf.tools.common;

import javax.xml.namespace.QName;

public final class ToolConstants {

    //public static final String TOOLSPECS_BASE = "/org/apache/cxf/tools/common/toolspec/toolspecs/";
    public static final String TOOLSPECS_BASE = "/org/apache/cxf/tools/";
    public static final String SCHEMA_URI = "http://www.w3.org/2001/XMLSchema";
    public static final String XML_NAMESPACE_URI = "http://www.w3.org/XML/1998/namespace";
    public static final String WSDL_NAMESPACE_URI = "http://schemas.xmlsoap.org/wsdl/";
    public static final String WSA_NAMESPACE_URI = "http://www.w3.org/2005/08/addressing";
    
    /**
     * Tools permit caller to pass in additional bean definitions.
     */
    public static final String CFG_BEAN_CONFIG = "beans";

    public static final String DEFAULT_TEMP_DIR = "gen_tmp";
    public static final String CFG_OUTPUTDIR = "outputdir";
    public static final String CFG_OUTPUTFILE = "outputfile";
    public static final String CFG_WSDLURL = "wsdlurl";
    public static final String CFG_WSDLLOCATION = "wsdlLocation";
    public static final String CFG_WSDLLIST = "wsdlList";
    public static final String CFG_NAMESPACE = "namespace";
    public static final String CFG_VERBOSE = "verbose";
    public static final String CFG_PORT = "port";
    public static final String CFG_BINDING = "binding";
    public static final String CFG_AUTORESOLVE = "autoNameResolution";
    public static final String CFG_WEBSERVICE = "webservice";
    public static final String CFG_SERVER = "server";
    public static final String CFG_CLIENT = "client";
    public static final String CFG_ALL = "all";
    public static final String CFG_IMPL = "impl";
    public static final String CFG_PACKAGENAME = "packagename";
    public static final String CFG_JSPACKAGEPREFIX = "jspackageprefix";
    public static final String CFG_NINCLUDE = "ninclude";
    public static final String CFG_NEXCLUDE = "nexclude";
    public static final String CFG_CMD_ARG = "args";
    public static final String CFG_INSTALL_DIR = "install.dir";
    public static final String CFG_PLATFORM_VERSION = "platform.version";
    public static final String CFG_COMPILE = "compile";
    public static final String CFG_CLASSDIR = "classdir";
    public static final String CFG_EXTRA_SOAPHEADER = "exsoapheader";
    public static final String CFG_DEFAULT_NS = "defaultns";
    public static final String CFG_DEFAULT_EX = "defaultex";
    public static final String CFG_XJC_ARGS = "xjc";
    public static final String CFG_CATALOG = "catalog";
    public static final String CFG_DEFAULT_VALUES = "defaultValues";
    public static final String CFG_JAVASCRIPT_UTILS =  "javascriptUtils";


    public static final String CFG_VALIDATE_WSDL = "validate";
    public static final String CFG_CREATE_XSD_IMPORTS = "createxsdimports";
    /**
     * Front-end selection command-line option to java2ws.
     */
    public static final String CFG_FRONTEND = "frontend";

    public static final String CFG_DATABINDING = "databinding";
    
    public static final String DEFAULT_ADDRESS = "http://localhost:9090";

    // WSDL2Java Constants

    public static final String CFG_TYPES = "types";
    public static final String CFG_INTERFACE = "interface";
    public static final String CFG_NIGNOREEXCLUDE = "nignoreexclude";
    public static final String CFG_ANT = "ant";
    public static final String CFG_LIB_REF = "library.references";
    public static final String CFG_ANT_PROP = "ant.prop";
    public static final String CFG_NO_ADDRESS_BINDING = "noAddressBinding";



    //Internal Flag to generate
    public static final String CFG_IMPL_CLASS = "implClass";
    public static final String CFG_GEN_CLIENT = "genClient";
    public static final String CFG_GEN_SERVER = "genServer";
    public static final String CFG_GEN_IMPL = "genImpl";
    public static final String CFG_GEN_TYPES = "genTypes";
    public static final String CFG_GEN_SEI = "genSEI";
    public static final String CFG_GEN_ANT = "genAnt";
    public static final String CFG_GEN_SERVICE = "genService";
    public static final String CFG_GEN_OVERWRITE = "overwrite";
    public static final String CFG_GEN_FAULT = "genFault";

    public static final String CFG_GEN_NEW_ONLY = "newonly";

    // Java2WSDL Constants

    public static final String CFG_CLASSPATH = "classpath";
    public static final String CFG_TNS = "tns";
    public static final String CFG_SERVICENAME = "servicename";
    public static final String CFG_SCHEMANS = "schemans";
    public static final String CFG_USETYPES = "usetypes";
    public static final String CFG_CLASSNAME = "classname";
    public static final String CFG_PORTTYPE = "porttype";
    public static final String CFG_SOURCEDIR = "sourcedir";
    public static final String CFG_WSDL = "wsdl";
    public static final String CFG_WRAPPERBEAN = "wrapperbean";

    // WSDL2Service Constants
    public static final String CFG_ADDRESS = "address";
    public static final String CFG_TRANSPORT = "transport";
    public static final String CFG_SERVICE = "service";
    public static final String CFG_BINDING_ATTR = "attrbinding";
    public static final String CFG_SOAP12 = "soap12";

    // WSDL2Soap Constants
    public static final String CFG_STYLE = "style";
    public static final String CFG_USE = "use";

    // XSD2WSDL Constants
    public static final String CFG_XSDURL = "xsdurl";
    public static final String CFG_NAME = "name";


    // WsdlValidator
    public static final String CFG_DEEP = "deep";
    public static final String CFG_SCHEMA_DIR = "schemaDir";
    public static final String CFG_SCHEMA_URL = "schemaURL";
    public static final String CXF_SCHEMA_DIR = "cxf_schema_dir";
    public static final String CXF_SCHEMAS_DIR_INJAR = "schemas/wsdl/";
    public static final String CFG_SUPPRESS_WARNINGS = "suppressWarnings";


    // WSDL2Java Processor Constants
    public static final String SEI_GENERATOR = "sei.generator";
    public static final String FAULT_GENERATOR = "fault.generator";
    public static final String TYPE_GENERATOR = "type.generator";
    public static final String IMPL_GENERATOR = "impl.generator";
    public static final String SVR_GENERATOR = "svr.generator";
    public static final String CLT_GENERATOR = "clt.generator";
    public static final String SERVICE_GENERATOR = "service.generator";
    public static final String ANT_GENERATOR = "ant.generator";
    public static final String HANDLER_GENERATOR = "handler.generator";

    // Binding namespace
    public static final String NS_JAXWS_BINDINGS = "http://java.sun.com/xml/ns/jaxws";
    public static final String NS_JAXB_BINDINGS = "http://java.sun.com/xml/ns/jaxb";
    public static final QName  JAXWS_BINDINGS = new QName(NS_JAXWS_BINDINGS, "bindings");
    public static final QName  JAXB_BINDINGS = new QName(NS_JAXB_BINDINGS, "bindings");
    public static final String JAXWS_BINDINGS_WSDL_LOCATION = "wsdlLocation";
    public static final String JAXWS_BINDING_NODE = "node";
    public static final String JAXWS_BINDING_VERSION = "version";

    public static final String ASYNC_METHOD_SUFFIX = "Async";

    public static final String HANDLER_CHAINS_URI = "http://java.sun.com/xml/ns/javaee";
    public static final String HANDLER_CHAIN = "handler-chain";
    public static final String HANDLER_CHAINS = "handler-chains";

    //public static final String RAW_JAXB_MODEL = "rawjaxbmodel";

    // JMS address
    public static final String NS_JMS_ADDRESS = "http://cxf.apache.org/transports/jms";
    public static final QName  JMS_ADDRESS = new QName(NS_JMS_ADDRESS, "address");

    // JBI address
    public static final String NS_JBI_ADDRESS = "http://cxf.apache.org/transports/jbi";
    public static final QName  JBI_ADDRESS = new QName(NS_JBI_ADDRESS, "address");


    public static final String JMS_ADDR_DEST_STYLE = "destinationStyle";
    public static final String JMS_ADDR_JNDI_URL = "jndiProviderURL";
    public static final String JMS_ADDR_JNDI_FAC = "jndiConnectionFactoryName";
    public static final String JMS_ADDR_JNDI_DEST = "jndiDestinationName";
    public static final String JMS_ADDR_MSG_TYPE = "messageType";
    public static final String JMS_ADDR_INIT_CTX = "initialContextFactory";
    public static final String JMS_ADDR_SUBSCRIBER_NAME = "durableSubscriberName";
    public static final String JMS_ADDR_MSGID_TO_CORRID = "useMessageIDAsCorrelationID";

    // XML Binding
    public static final String XMLBINDING_ROOTNODE = "rootNode";
    public static final String XMLBINDING_HTTP_LOCATION = "location";
    public static final String NS_XML_FORMAT = "http://cxf.apache.org/bindings/xformat";
    public static final String XML_FORMAT_PREFIX = "xformat";
    public static final String NS_XML_HTTP = "http://schemas.xmlsoap.org/wsdl/http/";
    public static final String XML_HTTP_PREFIX = "http";
    public static final QName  XML_HTTP_ADDRESS = new QName(NS_XML_HTTP, "address");
    public static final QName  XML_FORMAT = new QName(NS_XML_FORMAT, "body");
    public static final QName  XML_BINDING_FORMAT = new QName(NS_XML_FORMAT, "binding");




    public static final String XML_SCHEMA_COLLECTION = "xmlSchemaCollection";
    public static final String PORTTYPE_MAP = "portTypeMap";
    public static final String SCHEMA_TARGET_NAMESPACES = "schemaTargetNameSpaces";
    public static final String WSDL_DEFINITION = "wsdlDefinition";
    public static final String IMPORTED_DEFINITION = "importedDefinition";
    public static final String IMPORTED_PORTTYPE = "importedPortType";
    public static final String IMPORTED_SERVICE = "importedService";
    public static final String BINDING_GENERATOR = "BindingGenerator";
   

    // Tools framework
    public static final String FRONTEND_PLUGIN = "frontend";
    public static final String DATABINDING_PLUGIN = "databinding";

    public static final String CFG_WSDL_VERSION = "wsdlversion";

    // Suppress the code generation, in this case you can just get the generated code model
    public static final String CFG_SUPPRESS_GEN = "suppress";
    public static final String DEFAULT_PACKAGE_NAME = "defaultnamespace";
    
    //For java2ws tool
    public static final String SERVICE_LIST = "serviceList";
    public static final String GEN_FROM_SEI = "genFromSEI";
    public static final String JAXWS_FRONTEND = "jaxws";
    public static final String SIMPLE_FRONTEND = "simple";
    public static final String JAXB_DATABINDING = "jaxb";
    public static final String AEGIS_DATABINDING = "aegis";
    //For Simple FrontEnd
    public static final String SEI_CLASS = "seiClass";
    public static final String IMPL_CLASS = "implClass";
    public static final String SERVICE_NAME = "serviceName";
    public static final String PORT_NAME = "portName";
    public static final String DEFAULT_DATA_BINDING_NAME = "jaxb";
    public static final String DATABIND_BEAN_NAME_SUFFIX = "DatabindingBean";


    public static final String CLIENT_CLASS = "clientClass";
    public static final String SERVER_CLASS = "serverClass";
    public static final String CFG_JSPREFIXMAP = "javascriptPrefixMap";
    
    private ToolConstants() {
        //utility class
    }
}
