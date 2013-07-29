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

package org.apache.cxf.rt.security.xacml;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.apache.cxf.interceptor.security.SAMLSecurityContext;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.SecurityContext;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.joda.time.DateTime;
import org.opensaml.xacml.ctx.ActionType;
import org.opensaml.xacml.ctx.AttributeType;
import org.opensaml.xacml.ctx.AttributeValueType;
import org.opensaml.xacml.ctx.EnvironmentType;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResourceType;
import org.opensaml.xacml.ctx.SubjectType;


/**
 * This class constructs an XACML Request given a Principal, list of roles and MessageContext, 
 * following the SAML 2.0 profile of XACML 2.0. The principal name is inserted as the Subject ID,
 * and the list of roles associated with that principal are inserted as Subject roles. The action
 * to send defaults to "execute". 
 * 
 * For a SOAP Service, the resource-id Attribute refers to the 
 * "{serviceNamespace}serviceName#{operationNamespace}operationName" String (shortened to
 * "{serviceNamespace}serviceName#operationName" if the namespaces are identical). The 
 * "{serviceNamespace}serviceName", "{operationNamespace}operationName" and resource URI are also
 * sent to simplify processing at the PDP side.
 * 
 * For a REST service the request URI is the resource. You can also configure the ability to 
 * send the full request URL instead for a SOAP or REST service. The current DateTime is 
 * also sent in an Environment, however this can be disabled via configuration.
 */
public class DefaultXACMLRequestBuilder implements XACMLRequestBuilder {
    
    private String action = "execute";
    private boolean sendDateTime = true;
    private boolean sendFullRequestURL;
    
    /**
     * Set a new Action String to use
     */
    public void setAction(String newAction) {
        action = newAction;
    }
    
    /**
     * Get the Action String currently in use
     */
    public String getAction() {
        return action;
    }
    
    /**
     * Create an XACML Request given a Principal, list of roles and Message.
     */
    public RequestType createRequest(
        Principal principal, List<String> roles, Message message
    ) throws Exception {
        String issuer = getIssuer(message);
        String actionToUse = getAction(message);
        
        // Subject
        List<AttributeType> attributes = new ArrayList<AttributeType>();
        AttributeValueType subjectIdAttributeValue = 
            RequestComponentBuilder.createAttributeValueType(principal.getName());
        AttributeType subjectIdAttribute = 
            RequestComponentBuilder.createAttributeType(
                    XACMLConstants.SUBJECT_ID,
                    XACMLConstants.XS_STRING,
                    issuer,
                    Collections.singletonList(subjectIdAttributeValue)
            );
        attributes.add(subjectIdAttribute);
        
        if (roles != null) {
            List<AttributeValueType> roleAttributes = new ArrayList<AttributeValueType>();
            for (String role : roles) {
                if (role != null) {
                    AttributeValueType subjectRoleAttributeValue = 
                        RequestComponentBuilder.createAttributeValueType(role);
                    roleAttributes.add(subjectRoleAttributeValue);
                }
            }
            
            if (!roleAttributes.isEmpty()) {
                AttributeType subjectRoleAttribute = 
                    RequestComponentBuilder.createAttributeType(
                            XACMLConstants.SUBJECT_ROLE,
                            XACMLConstants.XS_ANY_URI,
                            issuer,
                            roleAttributes
                    );
                attributes.add(subjectRoleAttribute);
            }
        }
        SubjectType subjectType = RequestComponentBuilder.createSubjectType(attributes, null);
        
        // Resource
        ResourceType resourceType = createResourceType(message);
        
        // Action
        AttributeValueType actionAttributeValue = 
            RequestComponentBuilder.createAttributeValueType(actionToUse);
        AttributeType actionAttribute = 
            RequestComponentBuilder.createAttributeType(
                    XACMLConstants.ACTION_ID,
                    XACMLConstants.XS_STRING,
                    null,
                    Collections.singletonList(actionAttributeValue)
            );
        attributes.clear();
        attributes.add(actionAttribute);
        ActionType actionType = RequestComponentBuilder.createActionType(attributes);
        
        // Environment
        attributes.clear();
        if (sendDateTime) {
            DateTime dateTime = new DateTime();
            AttributeValueType environmentAttributeValue = 
                RequestComponentBuilder.createAttributeValueType(dateTime.toString());
            AttributeType environmentAttribute = 
                RequestComponentBuilder.createAttributeType(
                        XACMLConstants.CURRENT_DATETIME,
                        XACMLConstants.XS_DATETIME,
                        null,
                        Collections.singletonList(environmentAttributeValue)
                );
            attributes.add(environmentAttribute);
        }
        EnvironmentType environmentType = 
            RequestComponentBuilder.createEnvironmentType(attributes);
        
        // Request
        RequestType request = 
            RequestComponentBuilder.createRequestType(
                Collections.singletonList(subjectType), 
                Collections.singletonList(resourceType), 
                actionType, 
                environmentType
            );
        
        return request;
    }
    
    private ResourceType createResourceType(Message message) {
        List<AttributeType> attributes = new ArrayList<AttributeType>();
        
        // Resource-id
        String resourceId = null;
        boolean isSoapService = isSOAPService(message);
        if (isSoapService) {
            QName serviceName = getWSDLService(message);
            QName operationName = getWSDLOperation(message);
            
            if (serviceName != null) {
                resourceId = serviceName.toString() + "#";
                if (serviceName.getNamespaceURI() != null 
                    && serviceName.getNamespaceURI().equals(operationName.getNamespaceURI())) {
                    resourceId += operationName.getLocalPart();
                } else {
                    resourceId += operationName.toString();
                }
            } else {
                resourceId = operationName.toString();
            }
        } else {
            resourceId = getResourceURI(message, sendFullRequestURL);
        }
        
        attributes.add(createAttribute(XACMLConstants.RESOURCE_ID, XACMLConstants.XS_STRING, null,
                                           resourceId));
        
        if (isSoapService) {
            // WSDL Service
            QName wsdlService = getWSDLService(message);
            attributes.add(createAttribute(XACMLConstants.RESOURCE_WSDL_SERVICE_ID, XACMLConstants.XS_STRING, null,
                                           wsdlService.toString()));
            
            // WSDL Operation
            QName wsdlOperation = getWSDLOperation(message);
            attributes.add(createAttribute(XACMLConstants.RESOURCE_WSDL_OPERATION_ID, XACMLConstants.XS_STRING, null,
                                           wsdlOperation.toString()));
            
            // Resource URI
            String resourceURI = getResourceURI(message, sendFullRequestURL);
            attributes.add(createAttribute(XACMLConstants.RESOURCE_WSDL_URI_ID, XACMLConstants.XS_STRING, null,
                                           resourceURI));
        }
        
        return RequestComponentBuilder.createResourceType(attributes, null);
    }
    
    /**
     * Get the Issuer of the SAML Assertion
     */
    private String getIssuer(Message message) throws WSSecurityException {
        SecurityContext sc = message.get(SecurityContext.class);
        
        if (sc instanceof SAMLSecurityContext) {
            Element assertionElement = ((SAMLSecurityContext)sc).getAssertionElement();
            if (assertionElement != null) {
                AssertionWrapper wrapper = new AssertionWrapper(assertionElement);
                return wrapper.getIssuerString();
            }
        }
        
        return null;
    }

    public boolean isSendDateTime() {
        return sendDateTime;
    }

    public void setSendDateTime(boolean sendDateTime) {
        this.sendDateTime = sendDateTime;
    }

    public boolean isSendFullRequestURL() {
        return sendFullRequestURL;
    }

    /**
     * Whether to send the full Request URL as the resource or not. If set to true,
     * the full Request URL will be sent for both a JAX-WS and JAX-RS service. If set
     * to false, a JAX-WS service will send the "{namespace}operation" QName, and a 
     * JAX-RS service will send the RequestURI (i.e. minus the initial https:<ip> prefix).
     */
    public void setSendFullRequestURL(boolean sendFullRequestURL) {
        this.sendFullRequestURL = sendFullRequestURL;
    }
    
    private boolean isSOAPService(Message message) {
        return getWSDLOperation(message) != null;
    }

    private QName getWSDLOperation(Message message) {
        if (message != null && message.get(Message.WSDL_OPERATION) != null) {
            return (QName)message.get(Message.WSDL_OPERATION);
        }
        return null;
    }
    
    private QName getWSDLService(Message message) {
        if (message != null && message.get(Message.WSDL_SERVICE) != null) {
            return (QName)message.get(Message.WSDL_SERVICE);
        }
        return null;
    }
    
    /**
     * @param fullRequestURL Whether to send the full Request URL as the resource or not. If set to true, the
     *        full Request URL will be sent for both a JAX-WS and JAX-RS service. If set to false, a JAX-WS 
     *        service will send the "{namespace}operation" QName, and a JAX-RS service
     *        will send the RequestURI (i.e. minus the initial https:<ip> prefix)
     */
    private String getResourceURI(Message message, boolean fullRequestURL) {
        String property = fullRequestURL ? Message.REQUEST_URL : Message.REQUEST_URI;
        if (message != null && message.get(property) != null) {
            return (String)message.get(property);
        }
        return null;
    }
    
    private String getAction(Message message) {
        String actionToUse = action;
        // For REST use the HTTP Verb
        if (message.get(Message.WSDL_OPERATION) == null
            && message.get(Message.HTTP_REQUEST_METHOD) != null) {
            actionToUse = (String)message.get(Message.HTTP_REQUEST_METHOD);
        }
        return actionToUse;
    }
    
    private AttributeType createAttribute(String id, String type, String issuer, List<AttributeValueType> values) {
        return RequestComponentBuilder.createAttributeType(id, type, issuer, values);
    }
    
    private AttributeType createAttribute(String id, String type, String issuer, String value) {
        return createAttribute(id, type, issuer, 
                               Collections.singletonList(RequestComponentBuilder.createAttributeValueType(value)));
    }

}
