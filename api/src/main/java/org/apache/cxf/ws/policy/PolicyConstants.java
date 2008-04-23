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

package org.apache.cxf.ws.policy;

import javax.xml.namespace.QName;

import org.apache.cxf.extension.BusExtension;

/**
 * Encapsulation of version-specific WS-Policy constants.
 */
public final class PolicyConstants implements BusExtension {
    
    public static final String NAMESPACE_WS_POLICY
        = "http://www.w3.org/ns/ws-policy";
    
    public static final String NAMESPACE_W3_200607
        = "http://www.w3.org/2006/07/ws-policy";
    
    public static final String NAMESPACE_XMLSOAP_200409
        = "http://schemas.xmlsoap.org/ws/2004/09/policy";
    
    
    public static final String CLIENT_POLICY_OUT_INTERCEPTOR_ID
        = "org.apache.cxf.ws.policy.ClientPolicyOutInterceptor";
    public static final String CLIENT_POLICY_IN_INTERCEPTOR_ID
        = "org.apache.cxf.ws.policy.ClientPolicyInInterceptor";
    public static final String CLIENT_POLICY_IN_FAULT_INTERCEPTOR_ID
        = "org.apache.cxf.ws.policy.ClientPolicyInFaultInterceptor";

    public static final String SERVER_POLICY_IN_INTERCEPTOR_ID
        = "org.apache.cxf.ws.policy.ServerPolicyInInterceptor";
    public static final String SERVER_POLICY_OUT_INTERCEPTOR_ID
        = "org.apache.cxf.ws.policy.ServerPolicyOutInterceptor";
    public static final String SERVER_POLICY_OUT_FAULT_INTERCEPTOR_ID
        = "org.apache.cxf.ws.policy.ServerPolicyOutFaultInterceptor";
    
    public static final String CLIENT_OUT_ASSERTIONS
        = "org.apache.cxf.ws.policy.client.out.assertions";
    public static final String CLIENT_IN_ASSERTIONS
        = "org.apache.cxf.ws.policy.client.in.assertions";
    public static final String CLIENT_INFAULT_ASSERTIONS
        = "org.apache.cxf.ws.policy.client.infault.assertions";
    
    public static final String SERVER_IN_ASSERTIONS
        = "org.apache.cxf.ws.policy.server.in.assertions";
    public static final String SERVER_OUT_ASSERTIONS
        = "org.apache.cxf.ws.policy.server.out.assertions";
    public static final String SERVER_OUTFAULT_ASSERTIONS
        = "org.apache.cxf.ws.policy.server.outfault.assertions";
    
    private static String namespaceURI; 
    
    private static final String POLICY_ELEM_NAME = "Policy";
    
    private static final String ALL_ELEM_NAME = "All";
    
    private static final String EXACTLYONE_ELEM_NAME = "ExactlyOne";
    
    private static final String POLICYREFERENCE_ELEM_NAME = "PolicyReference";
    
    private static final String POLICYATTACHMENT_ELEM_NAME = "PolicyAttachment";
    
    private static final String APPLIESTO_ELEM_NAME = "AppliesTo";
    
    private static final String OPTIONAL_ATTR_NAME = "Optional"; 
    
    private static final String POLICYURIS_ATTR_NAME = "PolicyURIs";
    
    private static QName policyElemQName;
    
    private static QName allElemQName;
    
    private static QName exactlyOneElemQName;
    
    private static QName policyReferenceElemQName;
    
    private static QName policyAttachmentElemQName;
    
    private static QName appliesToElemQName;
    
    private static QName optionalAttrQName;
    
    private static QName policyURIsAttrQName;
    
    private static final String WSU_NAMESPACE_URI = 
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    
    private static final String WSU_ID_ATTR_NAME = "Id";
    
    private static final QName WSU_ID_ATTR_QNAME =
        new QName(WSU_NAMESPACE_URI, WSU_ID_ATTR_NAME);
    
    
    public PolicyConstants() {
        setNamespace(NAMESPACE_WS_POLICY);
    }
    
    public Class<?> getRegistrationType() {
        return PolicyConstants.class;
    }

    public void setNamespace(String uri) {
        namespaceURI = uri;
        
        // update qnames
        
        policyElemQName = new QName(namespaceURI, POLICY_ELEM_NAME);
        allElemQName = new QName(namespaceURI, ALL_ELEM_NAME);
        exactlyOneElemQName = new QName(namespaceURI, EXACTLYONE_ELEM_NAME);
        policyReferenceElemQName = new QName(namespaceURI, POLICYREFERENCE_ELEM_NAME);
        policyAttachmentElemQName = new QName(namespaceURI, POLICYATTACHMENT_ELEM_NAME);
        appliesToElemQName = new QName(namespaceURI, APPLIESTO_ELEM_NAME);
        optionalAttrQName = new QName(namespaceURI, OPTIONAL_ATTR_NAME);
        policyURIsAttrQName = new QName(namespaceURI, POLICYURIS_ATTR_NAME);
        
    }
  
    public String getNamespace() {
        return namespaceURI;
    } 
    
    public String getWSUNamespace() {
        return WSU_NAMESPACE_URI;
    }
    
    public String getPolicyElemName() {
        return POLICY_ELEM_NAME;
    }
    
    public String getAllElemName() {
        return ALL_ELEM_NAME;
    }
    
    public String getExactlyOneElemName() {
        return EXACTLYONE_ELEM_NAME;
    }
    
    public String getPolicyReferenceElemName() {
        return POLICYREFERENCE_ELEM_NAME;
    }
    
    public String getPolicyAttachmentElemName() {
        return POLICYATTACHMENT_ELEM_NAME;
    }
    
    public String getAppliesToElemName() {
        return APPLIESTO_ELEM_NAME;
    }
    
    public String getOptionalAttrName() {
        return OPTIONAL_ATTR_NAME;
    }
    
    public String getPolicyURIsAttrName() {
        return POLICYURIS_ATTR_NAME;
    }
    
    public String getIdAttrName() {
        return WSU_ID_ATTR_NAME;
    }
    
    public QName getPolicyElemQName() {
        return policyElemQName;
    }
    
    public QName getAllElemQName() {
        return allElemQName;
    }
    
    public QName getExactlyOneElemQName() {
        return exactlyOneElemQName;
    }
    
    public QName getPolicyReferenceElemQName() {
        return policyReferenceElemQName;
    }
    
    public QName getPolicyAttachmentElemQName() {
        return policyAttachmentElemQName;
    }
    
    public QName getAppliesToElemQName() {
        return appliesToElemQName;
    }
    
    public QName getOptionalAttrQName() {
        return optionalAttrQName;
    }
    
    public QName getPolicyURIsAttrQName() {
        return policyURIsAttrQName;
    }
    
    public QName getIdAttrQName() {
        return WSU_ID_ATTR_QNAME;
    } 
    
    
   
}
