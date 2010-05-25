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


package org.apache.cxf.ws.addressing;

import javax.xml.namespace.QName;


/**
 * A container for WS-Addressing constants.
 */
public final class JAXWSAConstants {
    
    public static final String WSAW_PREFIX = "wsaw";
    public static final String NS_WSAW = "http://www.w3.org/2006/05/addressing/wsdl";
    public static final String WSAM_PREFIX = "wsam";
    public static final String NS_WSAM = "http://www.w3.org/2007/05/addressing/metadata";

    public static final QName WSAW_ACTION_QNAME = new QName(NS_WSAW,  "Action");
    public static final QName WSAM_ACTION_QNAME = new QName(NS_WSAM,  "Action");
    
    public static final QName WSAW_USINGADDRESSING_QNAME = new QName(NS_WSAW, "UsingAddressing");
    
    public static final String NS_WSA = "http://www.w3.org/2005/08/addressing";
    public static final String WSA_PREFIX = "wsa";
    public static final String WSA_XSD = "http://www.w3.org/2006/03/addressing/ws-addr.xsd";
    public static final String WSA_ERF_NAME = "EndpointReference";
    public static final String WSA_REFERENCEPARAMETERS_NAME = "ReferenceParameters";
    public static final String WSA_METADATA_NAME = "Metadata";
    public static final String WSA_ADDRESS_NAME = "Address";
        
    public static final String WSAM_SERVICENAME_NAME = "ServiceName";
    public static final String WSAM_INTERFACE_NAME = "InterfaceName";
    public static final String WSAM_ENDPOINT_NAME = "EndpointName";    


    public static final String WSDLI_PFX = "wsdli";
    public static final String WSDLI_WSDLLOCATION = "wsdlLocation";
    public static final String NS_WSDLI = "http://www.w3.org/ns/wsdl-instance";

    /**
     * Well-known Property names for AddressingProperties in BindingProvider
     * Context.
     */
    public static final String CLIENT_ADDRESSING_PROPERTIES = 
        "javax.xml.ws.addressing.context";
    
    /**
     * Well-known Property names for AddressingProperties in Handler
     * Context.
     */
    public static final String CLIENT_ADDRESSING_PROPERTIES_INBOUND = 
        "javax.xml.ws.addressing.context.inbound";
    public static final String CLIENT_ADDRESSING_PROPERTIES_OUTBOUND = 
        "javax.xml.ws.addressing.context.outbound";
    public static final String SERVER_ADDRESSING_PROPERTIES_INBOUND = 
        "javax.xml.ws.addressing.context.inbound";
    public static final String SERVER_ADDRESSING_PROPERTIES_OUTBOUND = 
        "javax.xml.ws.addressing.context.outbound";
    
    /**
     * Used by AddressingBuilder factory method.
     */
    public static final String DEFAULT_ADDRESSING_BUILDER =
        "org.apache.cxf.ws.addressing.AddressingBuilderImpl";

    /**
     * Prevents instantiation. 
     */
    private JAXWSAConstants() {
    }
}
