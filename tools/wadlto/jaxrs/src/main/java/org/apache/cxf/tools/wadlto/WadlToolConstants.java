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

package org.apache.cxf.tools.wadlto;

import org.apache.cxf.tools.common.ToolConstants;

public final class WadlToolConstants {

    public static final String CFG_OUTPUTDIR = ToolConstants.CFG_OUTPUTDIR;
    public static final String CFG_COMPILE = ToolConstants.CFG_COMPILE;
    public static final String CFG_CLASSDIR = ToolConstants.CFG_CLASSDIR;
    public static final String CFG_XJC_ARGS = ToolConstants.CFG_XJC_ARGS;
    public static final String CFG_VERBOSE = ToolConstants.CFG_VERBOSE;
    public static final String CFG_ENCODING = ToolConstants.CFG_ENCODING;

    /**
     * Default
     */
    public static final String CFG_INTERFACE = ToolConstants.CFG_INTERFACE;
    public static final String CFG_IMPL = ToolConstants.CFG_IMPL;
    public static final String CFG_TYPES = ToolConstants.CFG_TYPES;
    public static final String CFG_PACKAGENAME = ToolConstants.CFG_PACKAGENAME;
    public static final String CFG_SCHEMA_PACKAGENAME = "schemaPackagename";
    public static final String CFG_RESOURCENAME = "resourcename";

    public static final String CFG_SCHEMA_TYPE_MAP = "schemaTypeMap";
    public static final String CFG_MEDIA_TYPE_MAP = "mediaTypeMap";
    public static final String CFG_MULTIPLE_XML_REPS = "supportMultipleXmlReps";
    public static final String CFG_BEAN_VALIDATION = "beanValidation";

    public static final String CFG_CATALOG = ToolConstants.CFG_CATALOG;
    public static final String CFG_BINDING = ToolConstants.CFG_BINDING;
    public static final String CFG_AUTHENTICATION = "authentication";

    public static final String CFG_NO_TYPES = ToolConstants.CFG_NO_TYPES;
    public static final String CFG_NO_VOID_FOR_EMPTY_RESPONSES = "noVoidForEmptyResponses";
    public static final String CFG_NO_ADDRESS_BINDING = ToolConstants.CFG_NO_ADDRESS_BINDING;

    public static final String CFG_WADL_NAMESPACE = "wadlNamespace";
    public static final String CFG_GENERATE_ENUMS = "generateEnums";
    public static final String CFG_INHERIT_PARAMS = "inheritResourceParams";
    public static final String CFG_JAXB_CLASS_NAME_SUFFIX = "jaxbClassNameSuffix";
    public static final String CFG_CREATE_JAVA_DOCS = "javaDocs";
    public static final String CFG_GENERATE_RESPONSE_IF_HEADERS_SET = "generateResponseIfHeadersSet";
    public static final String CFG_GENERATE_RESPONSE_FOR_METHODS = "generateResponseForMethods";
    public static final String CFG_VALIDATE_WADL = "validate";
    public static final String CFG_ONEWAY = "oneway";

    // JAX-RS 2.0 @Suspended AsyncResponse
    public static final String CFG_SUSPENDED_ASYNC = "async";

    // JAX-RS 2.1 Reactive Extensions
    public static final String CFG_RX = "rx";

    public static final String CFG_WADLURL = "wadl";



    private WadlToolConstants() {
        //utility class
    }
}
