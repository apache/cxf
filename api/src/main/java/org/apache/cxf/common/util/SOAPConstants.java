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

/**
 * SOAP constants from the specifications.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 * @since Feb 18, 2004
 */
public final class SOAPConstants {
    
    /** Document styles. */

    public static final String WSDL11_NS = "http://schemas.xmlsoap.org/wsdl/";
    
    public static final String WSDL11_SOAP_NS = "http://schemas.xmlsoap.org/wsdl/soap/";

    /**
     * Constant used to specify a rpc binding style.
     */
    public static final String STYLE_RPC = "rpc";

    /**
     * Constant used to specify a document binding style.
     */
    public static final String STYLE_DOCUMENT = "document";

    /**
     * Constant used to specify a wrapped binding style.
     */
    public static final String STYLE_WRAPPED = "wrapped";

    /**
     * Constant used to specify a message binding style.
     */
    public static final String STYLE_MESSAGE = "message";

    /**
     * Constant used to specify a literal binding use.
     */
    public static final String USE_LITERAL = "literal";

    /**
     * Constant used to specify a encoded binding use.
     */
    public static final String USE_ENCODED = "encoded";

    /**
     * XML Schema Namespace.
     */
    public static final String XSD = "http://www.w3.org/2001/XMLSchema";
    public static final String XSD_PREFIX = "xsd";

    public static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String XSI_PREFIX = "xsi";

    public static final String MEP_ROBUST_IN_OUT = "urn:xfire:mep:in-out";
    public static final String MEP_IN = "urn:xfire:mep:in";

    public static final String SOAP_ACTION = "SOAPAction";

    /**
     * Whether or not MTOM should be enabled for each service.
     */
    public static final String MTOM_ENABLED = "mtom-enabled";

    
    private SOAPConstants() {
        //utility class
    }
    
}
