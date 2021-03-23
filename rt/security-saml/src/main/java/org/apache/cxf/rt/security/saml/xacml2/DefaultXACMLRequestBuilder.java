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

package org.apache.cxf.rt.security.saml.xacml2;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.saml.xacml.CXFMessageParser;
import org.apache.cxf.rt.security.saml.xacml.XACMLConstants;
import org.joda.time.DateTime;
import org.opensaml.xacml.ctx.ActionType;
import org.opensaml.xacml.ctx.AttributeType;
import org.opensaml.xacml.ctx.AttributeValueType;
import org.opensaml.xacml.ctx.EnvironmentType;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResourceType;
import org.opensaml.xacml.ctx.SubjectType;

/**
 * This class constructs an XACML 2.0 Request given a Principal, list of roles and MessageContext,
 * following the SAML 2.0 profile of XACML 2.0. The principal name is inserted as the Subject ID,
 * and the list of roles associated with that principal are inserted as Subject roles. The current
 * DateTime is also sent in an Environment, however this can be disabled via configuration.
 *
 * For a SOAP Service, the resource-id Attribute refers to the
 * "{serviceNamespace}serviceName#{operationNamespace}operationName" String (shortened to
 * "{serviceNamespace}serviceName#operationName" if the namespaces are identical). The
 * "{serviceNamespace}serviceName", "{operationNamespace}operationName" and resource URI are also
 * sent to simplify processing at the PDP side. The action to send defaults to "execute".
 *
 * For a REST service the request URL is the resource. You can also configure the ability to
 * send the truncated request URI instead for a SOAP or REST service. The action to send defaults
 * to the HTTP verb.
 */
public class DefaultXACMLRequestBuilder implements XACMLRequestBuilder {

    private boolean sendDateTime = true;
    private String action = "execute";
    private boolean sendFullRequestURL = true;

    /**
     * Create an XACML Request given a Principal, list of roles and Message.
     */
    public RequestType createRequest(Principal principal, List<String> roles, Message message)
        throws Exception {
        CXFMessageParser messageParser = new CXFMessageParser(message);
        String issuer = messageParser.getIssuer();

        String actionToUse = messageParser.getAction(action);

        SubjectType subjectType = createSubjectType(principal, roles, issuer);
        ResourceType resourceType = createResourceType(messageParser);
        AttributeType actionAttribute = createAttribute(XACMLConstants.ACTION_ID, XACMLConstants.XS_STRING,
                                                        null, actionToUse);
        ActionType actionType = RequestComponentBuilder.createActionType(Collections.singletonList(actionAttribute));

        return RequestComponentBuilder.createRequestType(Collections.singletonList(subjectType),
                                                         Collections.singletonList(resourceType),
                                                         actionType,
                                                         createEnvironmentType());
    }

    private ResourceType createResourceType(CXFMessageParser messageParser) {
        List<AttributeType> attributes = new ArrayList<>();

        // Resource-id
        String resourceId;
        boolean isSoapService = messageParser.isSOAPService();
        if (isSoapService) {
            QName serviceName = messageParser.getWSDLService();
            QName operationName = messageParser.getWSDLOperation();

            if (serviceName != null) {
                resourceId = serviceName.toString() + '#';
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
            resourceId = messageParser.getResourceURI(sendFullRequestURL);
        }

        attributes.add(createAttribute(XACMLConstants.RESOURCE_ID, XACMLConstants.XS_STRING, null,
                                           resourceId));

        if (isSoapService) {
            // WSDL Service
            QName wsdlService = messageParser.getWSDLService();
            if (wsdlService != null) {
                attributes.add(createAttribute(XACMLConstants.RESOURCE_WSDL_SERVICE_ID, XACMLConstants.XS_STRING, null,
                                           wsdlService.toString()));
            }

            // WSDL Operation
            QName wsdlOperation = messageParser.getWSDLOperation();
            attributes.add(createAttribute(XACMLConstants.RESOURCE_WSDL_OPERATION_ID, XACMLConstants.XS_STRING, null,
                                           wsdlOperation.toString()));

            // WSDL Endpoint
            String endpointURI = messageParser.getResourceURI(sendFullRequestURL);
            attributes.add(createAttribute(XACMLConstants.RESOURCE_WSDL_ENDPOINT, XACMLConstants.XS_STRING, null,
                                           endpointURI));
        }

        return RequestComponentBuilder.createResourceType(attributes, null);
    }

    private EnvironmentType createEnvironmentType() {
        if (sendDateTime) {
            List<AttributeType> attributes = new ArrayList<>();
            AttributeType environmentAttribute = createAttribute(XACMLConstants.CURRENT_DATETIME,
                                                                 XACMLConstants.XS_DATETIME, null,
                                                                 new DateTime().toString());
            attributes.add(environmentAttribute);
            return RequestComponentBuilder.createEnvironmentType(attributes);
        }

        List<AttributeType> attributes = Collections.emptyList();
        return RequestComponentBuilder.createEnvironmentType(attributes);
    }

    private SubjectType createSubjectType(Principal principal, List<String> roles, String issuer) {
        List<AttributeType> attributes = new ArrayList<>();
        attributes.add(createAttribute(XACMLConstants.SUBJECT_ID, XACMLConstants.XS_STRING, issuer,
                                       principal.getName()));

        if (roles != null) {
            List<AttributeValueType> roleAttributes = new ArrayList<>();
            for (String role : roles) {
                if (role != null) {
                    AttributeValueType subjectRoleAttributeValue =
                        RequestComponentBuilder.createAttributeValueType(role);
                    roleAttributes.add(subjectRoleAttributeValue);
                }
            }

            if (!roleAttributes.isEmpty()) {
                AttributeType subjectRoleAttribute =
                    createAttribute(
                        XACMLConstants.SUBJECT_ROLE,
                        XACMLConstants.XS_ANY_URI,
                        issuer,
                        roleAttributes
                    );
                attributes.add(subjectRoleAttribute);
            }
        }

        return RequestComponentBuilder.createSubjectType(attributes, null);
    }

    private AttributeType createAttribute(String id, String type, String issuer, List<AttributeValueType> values) {
        return RequestComponentBuilder.createAttributeType(id, type, issuer, values);
    }

    private AttributeType createAttribute(String id, String type, String issuer, String value) {
        return createAttribute(id, type, issuer,
                               Collections.singletonList(RequestComponentBuilder.createAttributeValueType(value)));
    }

    /**
     * Set a new Action String to use
     */
    public void setAction(String action) {
        this.action = action;
    }

    public void setSendDateTime(boolean sendDateTime) {
        this.sendDateTime = sendDateTime;
    }

    /**
     * Whether to send the full Request URL as the resource or not. If set to true,
     * the full Request URL will be sent for both a JAX-WS and JAX-RS service. If set
     * to false (the default), a JAX-WS service will send the "{namespace}operation" QName,
     * and a JAX-RS service will send the RequestURI (i.e. minus the initial https:<ip> prefix).
     */
    public void setSendFullRequestURL(boolean sendFullRequestURL) {
        this.sendFullRequestURL = sendFullRequestURL;
    }

}
