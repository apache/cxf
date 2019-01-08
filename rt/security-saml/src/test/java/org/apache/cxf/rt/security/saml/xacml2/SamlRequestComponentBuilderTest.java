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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.rt.security.saml.xacml.XACMLConstants;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.opensaml.xacml.ctx.ActionType;
import org.opensaml.xacml.ctx.AttributeType;
import org.opensaml.xacml.ctx.AttributeValueType;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResourceType;
import org.opensaml.xacml.ctx.SubjectType;
import org.opensaml.xacml.profile.saml.SAMLProfileConstants;
import org.opensaml.xacml.profile.saml.XACMLAuthzDecisionQueryType;

import static org.junit.Assert.assertNotNull;


/**
 * Some unit tests for creating a SAML XACML Request.
 */
public class SamlRequestComponentBuilderTest {

    private DocumentBuilder docBuilder;
    static {
        OpenSAMLUtil.initSamlEngine();
    }

    public SamlRequestComponentBuilderTest() throws ParserConfigurationException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        docBuilder = docBuilderFactory.newDocumentBuilder();
    }

    @org.junit.Test
    public void testCreateXACMLSamlAuthzQueryRequest() throws Exception {
        Document doc = docBuilder.newDocument();

        //
        // Create XACML request
        //

        // Subject
        AttributeValueType subjectIdAttributeValue =
            RequestComponentBuilder.createAttributeValueType(
                    "alice-user@apache.org"
            );
        AttributeType subjectIdAttribute =
            RequestComponentBuilder.createAttributeType(
                    XACMLConstants.SUBJECT_ID,
                    XACMLConstants.RFC_822_NAME,
                    null,
                    Collections.singletonList(subjectIdAttributeValue)
            );

        AttributeValueType subjectGroupAttributeValue =
            RequestComponentBuilder.createAttributeValueType(
                    "manager"
            );
        AttributeType subjectGroupAttribute =
            RequestComponentBuilder.createAttributeType(
                    XACMLConstants.SUBJECT_ROLE,
                    XACMLConstants.XS_ANY_URI,
                    "admin-user@apache.org",
                    Collections.singletonList(subjectGroupAttributeValue)
            );
        List<AttributeType> attributes = new ArrayList<>();
        attributes.add(subjectIdAttribute);
        attributes.add(subjectGroupAttribute);
        SubjectType subject = RequestComponentBuilder.createSubjectType(attributes, null);

        // Resource
        AttributeValueType resourceAttributeValue =
            RequestComponentBuilder.createAttributeValueType(
                    "{http://www.example.org/contract/DoubleIt}DoubleIt"
            );
        AttributeType resourceAttribute =
            RequestComponentBuilder.createAttributeType(
                    XACMLConstants.RESOURCE_ID,
                    XACMLConstants.XS_STRING,
                    null,
                    Collections.singletonList(resourceAttributeValue)
            );
        attributes.clear();
        attributes.add(resourceAttribute);
        ResourceType resource = RequestComponentBuilder.createResourceType(attributes, null);

        // Action
        AttributeValueType actionAttributeValue =
            RequestComponentBuilder.createAttributeValueType(
                    "execute"
            );
        AttributeType actionAttribute =
            RequestComponentBuilder.createAttributeType(
                    XACMLConstants.ACTION_ID,
                    XACMLConstants.XS_STRING,
                    null,
                    Collections.singletonList(actionAttributeValue)
            );
        attributes.clear();
        attributes.add(actionAttribute);
        ActionType action = RequestComponentBuilder.createActionType(attributes);

        // Request
        RequestType request =
            RequestComponentBuilder.createRequestType(
                    Collections.singletonList(subject),
                    Collections.singletonList(resource),
                    action,
                    null
            );

        //
        // Create SAML wrapper
        //

        XACMLAuthzDecisionQueryType authzQuery =
            SamlRequestComponentBuilder.createAuthzDecisionQuery(
                    "Issuer", request, SAMLProfileConstants.SAML20XACML20P_NS
            );

        Element policyElement = OpenSAMLUtil.toDom(authzQuery, doc);
        // String outputString = DOM2Writer.nodeToString(policyElement);
        assertNotNull(policyElement);
    }


}