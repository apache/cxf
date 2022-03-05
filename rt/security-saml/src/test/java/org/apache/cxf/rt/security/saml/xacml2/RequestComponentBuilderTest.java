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

import java.time.Instant;
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
import org.opensaml.xacml.ctx.EnvironmentType;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResourceType;
import org.opensaml.xacml.ctx.SubjectType;

import static org.junit.Assert.assertNotNull;


/**
 * Some unit tests to create a XACML Request using the RequestComponentBuilder.
 */
public class RequestComponentBuilderTest {

    private DocumentBuilder docBuilder;
    static {
        OpenSAMLUtil.initSamlEngine();
    }

    public RequestComponentBuilderTest() throws ParserConfigurationException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        docBuilder = docBuilderFactory.newDocumentBuilder();
    }

    @org.junit.Test
    public void testCreateXACMLRequest() throws Exception {
        Document doc = docBuilder.newDocument();

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

        Element policyElement = OpenSAMLUtil.toDom(request, doc);
        // String outputString = DOM2Writer.nodeToString(policyElement);
        assertNotNull(policyElement);
    }

    @org.junit.Test
    public void testEnvironment() throws Exception {
        Document doc = docBuilder.newDocument();

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

        List<AttributeType> attributes = new ArrayList<>();
        attributes.add(subjectIdAttribute);
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

        // Environment
        Instant dateTime = Instant.now();
        AttributeValueType environmentAttributeValue =
            RequestComponentBuilder.createAttributeValueType(dateTime.toString());
        AttributeType environmentAttribute =
            RequestComponentBuilder.createAttributeType(
                    XACMLConstants.CURRENT_DATETIME,
                    XACMLConstants.XS_DATETIME,
                    null,
                    Collections.singletonList(environmentAttributeValue)
            );
        attributes.clear();
        attributes.add(environmentAttribute);
        EnvironmentType environmentType =
             RequestComponentBuilder.createEnvironmentType(attributes);

        // Request
        RequestType request =
            RequestComponentBuilder.createRequestType(
                    Collections.singletonList(subject),
                    Collections.singletonList(resource),
                    action,
                    environmentType
            );

        Element policyElement = OpenSAMLUtil.toDom(request, doc);
        // String outputString = DOM2Writer.nodeToString(policyElement);
        assertNotNull(policyElement);
    }

}