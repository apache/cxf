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

package org.apache.cxf.rt.security.saml.xacml.pep;

import java.security.Principal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.saml.xacml.pdp.api.PolicyDecisionPoint;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.opensaml.xacml.ctx.DecisionType.DECISION;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResponseType;
import org.opensaml.xacml.ctx.ResultType;
import org.opensaml.xacml.ctx.StatusType;

/**
 * An interceptor to perform an XACML 2.0 authorization request to a remote PDP using OpenSAML,
 * and make an authorization decision based on the response. It takes the principal and roles
 * from the SecurityContext, and uses the XACMLRequestBuilder to construct an XACML Request
 * statement. 
 */
public class OpenSAMLXACMLAuthorizingInterceptor extends AbstractXACMLAuthorizingInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(OpenSAMLXACMLAuthorizingInterceptor.class);
    
    private PolicyDecisionPoint pdp;
    
    public OpenSAMLXACMLAuthorizingInterceptor(PolicyDecisionPoint pdp) {
        super();
        this.pdp = pdp;
    }

    /**
     * Perform a (remote) authorization decision and return a boolean depending on the result
     */
    @Override
    protected boolean authorize(
        Object xacmlRequest, Principal principal, Message message
    ) throws Exception {
        if (!(xacmlRequest instanceof RequestType)) {
            String error = "XACMLRequest parameter is not an instance of OpenSAML RequestType!";
            LOG.warning(error);
            throw new Exception(error);
        }
        
        RequestType request = (RequestType)xacmlRequest;
        if (LOG.isLoggable(Level.FINE)) {
            Document doc = DOMUtils.createDocument();
            Element requestElement = OpenSAMLUtil.toDom(request, doc);
            LOG.log(Level.FINE, DOM2Writer.nodeToString(requestElement));
        }
        
        // Evaluate the request
        Source responseSource = this.pdp.evaluate(requestType2Source(request));
        
        // Parse the Response into an OpenSAML ResponseType Object
        ResponseType response = responseSourceToResponseType(responseSource);
        List<ResultType> results = response.getResults();
        
        if (results == null) {
            return false;
        }
        
        for (ResultType result : results) {
            // Handle any Obligations returned by the PDP
            handleObligations(request, principal, message, result);
            
            DECISION decision = result.getDecision() != null ? result.getDecision().getDecision() : DECISION.Deny; 
            String code = "";
            String statusMessage = "";
            if (result.getStatus() != null) {
                StatusType status = result.getStatus();
                code = status.getStatusCode() != null ? status.getStatusCode().getValue() : "";
                statusMessage = status.getStatusMessage() != null ? status.getStatusMessage().getValue() : "";
            }
            LOG.fine("XACML authorization result: " + decision + ", code: " + code + ", message: " + statusMessage);
            return decision == DECISION.Permit;
        }
        
        return false;
    }
    
    private ResponseType responseSourceToResponseType(Source responseSource) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer trans = transformerFactory.newTransformer();
            
            DOMResult res = new DOMResult();
            trans.transform(responseSource, res);
            Node nd = res.getNode();
            if (nd instanceof Document) {
                nd = ((Document)nd).getDocumentElement();
            }
            return (ResponseType)OpenSAMLUtil.fromDom((Element)nd);
        } catch (Exception e) {
            throw new RuntimeException("Error converting pdp response to ResponseType", e);
        }
    }
    
    private Source requestType2Source(RequestType request) {
        Document doc = DOMUtils.createDocument();
        Element requestElement;
        try {
            requestElement = OpenSAMLUtil.toDom(request, doc);
        } catch (WSSecurityException e) {
            throw new RuntimeException("Error converting PDP RequestType to Dom", e);
        }
        return new DOMSource(requestElement);
    }
    
    /**
     * Handle any Obligations returned by the PDP
     */
    protected void handleObligations(
        RequestType request,
        Principal principal,
        Message message,
        ResultType result
    ) throws Exception {
        // Do nothing by default
    }
    
}
