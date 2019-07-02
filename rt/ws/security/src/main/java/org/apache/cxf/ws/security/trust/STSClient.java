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

package org.apache.cxf.ws.security.trust;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.wss4j.AttachmentCallbackHandler;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.util.WSSecurityUtil;

/**
 * A extension of AbstractSTSClient to communicate with an STS and return a SecurityToken
 * to the client.
 */
public class STSClient extends AbstractSTSClient {
    private static final Logger LOG = LogUtils.getL7dLogger(STSClient.class);

    public STSClient(Bus b) {
        super(b);
    }

    public SecurityToken requestSecurityToken() throws Exception {
        return requestSecurityToken(null);
    }

    public SecurityToken requestSecurityToken(String appliesTo) throws Exception {
        return requestSecurityToken(appliesTo, null);
    }

    public SecurityToken requestSecurityToken(String appliesTo, String binaryExchange) throws Exception {
        return requestSecurityToken(appliesTo, null, "/Issue", binaryExchange);
    }

    public SecurityToken requestSecurityToken(
        String appliesTo, String action, String requestType, String binaryExchange
    ) throws Exception {
        STSResponse response = issue(appliesTo, action, requestType, binaryExchange);

        SecurityToken token =
            createSecurityToken(getDocumentElement(response.getResponse()), response.getEntropy());
        inlineAttachments(token, response.getAttachments());

        if (response.getCert() != null) {
            token.setX509Certificate(response.getCert(), response.getCrypto());
        }
        if (token.getTokenType() == null) {
            String tokenTypeFromTemplate = getTokenTypeFromTemplate();
            if (tokenTypeFromTemplate != null) {
                token.setTokenType(tokenTypeFromTemplate);
            } else if (tokenType != null) {
                token.setTokenType(tokenType);
            }
        }
        return token;
    }

    public SecurityToken renewSecurityToken(SecurityToken tok) throws Exception {
        STSResponse response = renew(tok);

        SecurityToken token = createSecurityToken(getDocumentElement(response.getResponse()), null);
        inlineAttachments(token, response.getAttachments());

        if (token.getTokenType() == null) {
            String tokenTypeFromTemplate = getTokenTypeFromTemplate();
            if (tokenTypeFromTemplate != null) {
                token.setTokenType(tokenTypeFromTemplate);
            } else if (tokenType != null) {
                token.setTokenType(tokenType);
            }
        }
        return token;
    }

    public List<SecurityToken> validateSecurityToken(SecurityToken tok) throws Exception {
        String validateTokenType = tokenType;
        if (validateTokenType == null) {
            validateTokenType = namespace + "/RSTR/Status";
        }
        return validateSecurityToken(tok, validateTokenType);
    }

    private void inlineAttachments(SecurityToken token, Collection<Attachment> attachments) throws WSSecurityException {
        Message msg = PhaseInterceptorChain.getCurrentMessage();
        if (AttachmentUtil.isMtomEnabled(msg) && attachments != null) {
            Element requestedSecurityTokenElement = token.getToken();
            if (requestedSecurityTokenElement != null) {
                // Look for xop:Include Nodes + inline the contents
                List<Element> includeElements =
                    XMLUtils.findElements(requestedSecurityTokenElement.getFirstChild(), "Include", WSConstants.XOP_NS);
                WSSecurityUtil.inlineAttachments(includeElements, new AttachmentCallbackHandler(attachments), true);
            }
        }
    }

    protected List<SecurityToken> validateSecurityToken(SecurityToken tok, String tokentype)
        throws Exception {
        STSResponse response = validate(tok, tokentype);

        Element el = getDocumentElement(response.getResponse());
        if ("RequestSecurityTokenResponseCollection".equals(el.getLocalName())) {
            el = DOMUtils.getFirstElement(el);
        }
        if (!"RequestSecurityTokenResponse".equals(el.getLocalName())) {
            throw new Fault("Unexpected element " + el.getLocalName(), LOG);
        }
        el = DOMUtils.getFirstElement(el);
        String reason = null;
        boolean valid = false;
        List<SecurityToken> tokens = new LinkedList<>();
        while (el != null) {
            if ("Status".equals(el.getLocalName())) {
                Element e2 = DOMUtils.getFirstChildWithName(el, el.getNamespaceURI(), "Code");
                String s = DOMUtils.getContent(e2);
                valid = s.endsWith("/status/valid");

                e2 = DOMUtils.getFirstChildWithName(el, el.getNamespaceURI(), "Reason");
                if (e2 != null) {
                    reason = DOMUtils.getContent(e2);
                }
            } else if ("RequestedSecurityToken".equals(el.getLocalName())) {
                SecurityToken token =
                    createSecurityToken(getDocumentElement(response.getResponse()), response.getEntropy());

                if (response.getCert() != null) {
                    token.setX509Certificate(response.getCert(), response.getCrypto());
                }
                if (token.getTokenType() == null) {
                    String tokenTypeFromTemplate = getTokenTypeFromTemplate();
                    if (tokenTypeFromTemplate != null) {
                        token.setTokenType(tokenTypeFromTemplate);
                    } else if (tokenType != null) {
                        token.setTokenType(tokenType);
                    }
                }

                tokens.add(token);
            }
            el = DOMUtils.getNextElement(el);
        }
        if (!valid) {
            throw new TrustException(LOG, "VALIDATION_FAILED", reason);
        }
        if (tokens.isEmpty()) {
            tokens.add(tok);
        }
        return tokens;
    }

    public boolean cancelSecurityToken(SecurityToken token) throws Exception {
        try {
            cancel(token);
            return true;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Problem cancelling token", ex);
            return false;
        }
    }

    private String getTokenTypeFromTemplate() {
        if (template != null && DOMUtils.getFirstElement(template) != null) {
            Element tl = DOMUtils.getFirstElement(template);
            while (tl != null) {
                if ("TokenType".equals(tl.getLocalName())) {
                    return DOMUtils.getContent(tl);
                }
                tl = DOMUtils.getNextElement(tl);
            }
        }
        return null;
    }

}
