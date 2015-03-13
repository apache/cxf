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

package org.apache.cxf.systest.ws.saml;

import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.rt.security.xacml.XACMLConstants;
import org.apache.cxf.rt.security.xacml.pdp.api.PolicyDecisionPoint;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.xacml.XACMLObjectBuilder;
import org.opensaml.xacml.ctx.AttributeType;
import org.opensaml.xacml.ctx.DecisionType;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResponseType;
import org.opensaml.xacml.ctx.ResultType;
import org.opensaml.xacml.ctx.StatusCodeType;
import org.opensaml.xacml.ctx.StatusType;
import org.opensaml.xacml.ctx.SubjectType;

/**
 * A test implementation of PolicyDecisionPoint. It just mocks up a Response
 * object based on the role of the Subject. If the role is "manager" then it permits the
 * request, otherwise it denies it.
 */
public class PolicyDecisionPointMockImpl implements PolicyDecisionPoint {
    
    public PolicyDecisionPointMockImpl() {
        org.apache.wss4j.common.saml.OpenSAMLUtil.initSamlEngine();
    }
    
    @Override
    public Source evaluate(Source request) {
        RequestType requestType = requestSourceToRequestType(request);
        
        XMLObjectBuilderFactory builderFactory = 
            XMLObjectProviderRegistrySupport.getBuilderFactory();
        
        @SuppressWarnings("unchecked")
        XACMLObjectBuilder<ResponseType> responseTypeBuilder = 
            (XACMLObjectBuilder<ResponseType>)
            builderFactory.getBuilder(ResponseType.DEFAULT_ELEMENT_NAME);
        
        @SuppressWarnings("unchecked")
        XACMLObjectBuilder<ResultType> resultTypeBuilder = 
            (XACMLObjectBuilder<ResultType>)
            builderFactory.getBuilder(ResultType.DEFAULT_ELEMENT_NAME);
        
        @SuppressWarnings("unchecked")
        XACMLObjectBuilder<DecisionType> decisionTypeBuilder =
            (XACMLObjectBuilder<DecisionType>)
            builderFactory.getBuilder(DecisionType.DEFAULT_ELEMENT_NAME);
        
        @SuppressWarnings("unchecked")
        XACMLObjectBuilder<StatusType> statusTypeBuilder = 
            (XACMLObjectBuilder<StatusType>)
            builderFactory.getBuilder(StatusType.DEFAULT_ELEMENT_NAME);
        
        @SuppressWarnings("unchecked")
        XACMLObjectBuilder<StatusCodeType> statusCodeTypeBuilder =
            (XACMLObjectBuilder<StatusCodeType>)
            builderFactory.getBuilder(StatusCodeType.DEFAULT_ELEMENT_NAME);
            
        DecisionType decisionType = decisionTypeBuilder.buildObject();
        
        String role = getSubjectRole(requestType);
        if ("manager".equals(role)) {
            decisionType.setDecision(DecisionType.DECISION.Permit); 
        } else {
            decisionType.setDecision(DecisionType.DECISION.Deny);
        }
        
        ResultType result = resultTypeBuilder.buildObject();
        result.setDecision(decisionType);
        
        StatusType status = statusTypeBuilder.buildObject();
        StatusCodeType statusCode = statusCodeTypeBuilder.buildObject();
        statusCode.setValue("urn:oasis:names:tc:xacml:1.0:status:ok");
        status.setStatusCode(statusCode);
        result.setStatus(status);
        
        ResponseType response = responseTypeBuilder.buildObject();
        response.getResults().add(result);
        
        return responseType2Source(response);
    }
    
    private RequestType requestSourceToRequestType(Source requestSource) {
        try {
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            DOMResult res = new DOMResult();
            trans.transform(requestSource, res);
            Node nd = res.getNode();
            if (nd instanceof Document) {
                nd = ((Document)nd).getDocumentElement();
            }
            return (RequestType)OpenSAMLUtil.fromDom((Element)nd);
        } catch (Exception e) {
            throw new RuntimeException("Error converting pdp response to ResponseType", e);
        }
    }
    
    private Source responseType2Source(ResponseType response) {
        Document doc = DOMUtils.createDocument();
        Element responseElement;
        try {
            responseElement = OpenSAMLUtil.toDom(response, doc);
        } catch (WSSecurityException e) {
            throw new RuntimeException("Error converting PDP RequestType to Dom", e);
        }
        return new DOMSource(responseElement);
    }
    
    private String getSubjectRole(RequestType request) {
        List<SubjectType> subjects = request.getSubjects();
        if (subjects != null) {
            for (SubjectType subject : subjects) {
                List<AttributeType> attributes = subject.getAttributes();
                if (attributes != null) {
                    for (AttributeType attribute : attributes) {
                        if (XACMLConstants.SUBJECT_ROLE.equals(attribute.getAttributeId())) {
                            return attribute.getAttributeValues().get(0).getValue();
                        }
                    }
                }
            }
        }
        return null;
    }

    
}
