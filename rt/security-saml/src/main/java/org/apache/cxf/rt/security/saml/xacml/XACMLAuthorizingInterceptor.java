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

package org.apache.cxf.rt.security.saml.xacml;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.saml.xacml.pdp.api.PolicyDecisionPoint;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResponseType;

/**
 * An interceptor to perform an XACML authorization request to a remote PDP,
 * and make an authorization decision based on the response. It takes the principal and roles
 * from the SecurityContext, and uses the XACMLRequestBuilder to construct an XACML Request
 * statement. 
 */
public class XACMLAuthorizingInterceptor extends AbstractXACMLAuthorizingInterceptor {
    private PolicyDecisionPoint pdp;
    
    public XACMLAuthorizingInterceptor(PolicyDecisionPoint pdp) {
        super();
        this.pdp = pdp;
    }

    @Override
    public ResponseType performRequest(RequestType request, Message message) throws Exception {
        Source requestSource = requestType2Source(request);
        Source responseSource = this.pdp.evaluate(requestSource);
        return responseSourceToResponseType(responseSource);
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

    private ResponseType responseSourceToResponseType(Source responseSource) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = transformerFactory.newTransformer();
            
            DOMResult res = new DOMResult();
            transformer.transform(responseSource, res);
            Node nd = res.getNode();
            if (nd instanceof Document) {
                nd = ((Document)nd).getDocumentElement();
            }
            return (ResponseType)OpenSAMLUtil.fromDom((Element)nd);
        } catch (Exception e) {
            throw new RuntimeException("Error converting pdp response to ResponseType", e);
        }
    }
}
