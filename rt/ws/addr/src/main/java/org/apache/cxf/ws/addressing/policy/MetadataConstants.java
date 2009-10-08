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

package org.apache.cxf.ws.addressing.policy;

import javax.xml.namespace.QName;

/**
 *
 */
public final class MetadataConstants {
    
    public static final String NAMESPACE_URI = 
        "http://www.w3.org/2007/02/addressing/metadata";
    public static final String ADDRESSING_ELEM_NAME = "Addressing";
    public static final String ANON_RESPONSES_ELEM_NAME = "AnonymousResponses";
    public static final String NON_ANON_RESPONSES_ELEM_NAME = "NonAnonymousResponses";
    public static final String USING_ADDRESSING_ELEM_NAME = "UsingAddressing";
    
    public static final String ADDR_POLICY_2004_NAMESPACE_URI = 
        "http://schemas.xmlsoap.org/ws/2004/08/addressing/policy";
    public static final String ADDR_WSDL_2005_NAMESPACE_URI = 
        "http://www.w3.org/2005/02/addressing/wsdl";
    public static final String ADDR_WSDL_2006_NAMESPACE_URI = 
        "http://www.w3.org/2006/05/addressing/wsdl";
       
    public static final QName ADDRESSING_ASSERTION_QNAME
        = new QName(NAMESPACE_URI, ADDRESSING_ELEM_NAME);
    public static final QName ANON_RESPONSES_ASSERTION_QNAME
        = new QName(NAMESPACE_URI, ANON_RESPONSES_ELEM_NAME);
    public static final QName NON_ANON_RESPONSES_ASSERTION_QNAME
        = new QName(NAMESPACE_URI, NON_ANON_RESPONSES_ELEM_NAME);
    
    public static final QName USING_ADDRESSING_2004_QNAME
        = new QName(ADDR_POLICY_2004_NAMESPACE_URI, USING_ADDRESSING_ELEM_NAME);
    public static final QName USING_ADDRESSING_2005_QNAME
        = new QName(ADDR_WSDL_2005_NAMESPACE_URI, USING_ADDRESSING_ELEM_NAME);
    public static final QName USING_ADDRESSING_2006_QNAME
        = new QName(ADDR_WSDL_2006_NAMESPACE_URI, USING_ADDRESSING_ELEM_NAME);
    
    private MetadataConstants() {        
    }
    
}
