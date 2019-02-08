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

package org.apache.cxf.ws.security.wss4j;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.processor.BinarySecurityTokenProcessor;
import org.apache.wss4j.policy.model.AbstractToken;

/**
 * An interceptor to add a BinarySecurityToken token to the security header of an outbound request, and to
 * process a BinarySecurityToken on an inbound request. It takes the BinarySecurityToken from the message
 * context on the outbound side.
 */
public class BinarySecurityTokenInterceptor extends AbstractTokenInterceptor {

    public BinarySecurityTokenInterceptor() {
        super();
    }

    protected void processToken(SoapMessage message) {
        Header h = findSecurityHeader(message, false);
        if (h == null) {
            return;
        }
        Element el = (Element)h.getObject();
        Element child = DOMUtils.getFirstElement(el);
        while (child != null) {
            if (WSS4JConstants.BINARY_TOKEN_LN.equals(child.getLocalName())
                && WSS4JConstants.WSSE_NS.equals(child.getNamespaceURI())) {
                try {
                    List<WSSecurityEngineResult> bstResults = processToken(child, message);
                    if (bstResults != null) {
                        List<WSHandlerResult> results = CastUtils.cast((List<?>)message
                                .get(WSHandlerConstants.RECV_RESULTS));
                        if (results == null) {
                            results = new ArrayList<>();
                            message.put(WSHandlerConstants.RECV_RESULTS, results);
                        }
                        WSHandlerResult rResult =
                            new WSHandlerResult(null, bstResults,
                                                Collections.singletonMap(WSConstants.BST, bstResults));
                        results.add(0, rResult);

                        assertTokens(message);

                        Principal principal =
                            (Principal)bstResults.get(0).get(WSSecurityEngineResult.TAG_PRINCIPAL);

                        SecurityContext sc = message.get(SecurityContext.class);
                        if (sc == null || sc.getUserPrincipal() == null) {
                            message.put(SecurityContext.class, new DefaultSecurityContext(principal, null));
                        }

                    }
                } catch (WSSecurityException ex) {
                    throw WSS4JUtils.createSoapFault(message, message.getVersion(), ex);
                }
            }
            child = DOMUtils.getNextElement(child);
        }
    }

    private List<WSSecurityEngineResult> processToken(Element tokenElement, final SoapMessage message)
        throws WSSecurityException {
        RequestData data = new CXFRequestData();
        Object o = SecurityUtils.getSecurityPropertyValue(SecurityConstants.CALLBACK_HANDLER, message);
        try {
            data.setCallbackHandler(SecurityUtils.getCallbackHandler(o));
        } catch (Exception ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
        }
        data.setMsgContext(message);
        data.setWssConfig(WSSConfig.getNewInstance());

        WSDocInfo wsDocInfo = new WSDocInfo(tokenElement.getOwnerDocument());
        data.setWsDocInfo(wsDocInfo);

        BinarySecurityTokenProcessor p = new BinarySecurityTokenProcessor();
        return p.handleToken(tokenElement, data);
    }

    protected AbstractToken assertTokens(SoapMessage message) {
        // Assert tokens here if required
        return null;
    }

    protected void addToken(SoapMessage message) {
        SecurityToken securityToken = getSecurityToken(message);
        if (securityToken == null || securityToken.getToken() == null) {
            // No SecurityToken so just return
            return;
        }

        assertTokens(message);
        Header h = findSecurityHeader(message, true);
        Element el = (Element)h.getObject();
        el.appendChild(el.getOwnerDocument().importNode(securityToken.getToken(), true));
    }

    private SecurityToken getSecurityToken(SoapMessage message) {
        if (message.getContextualProperty(SecurityConstants.TOKEN) instanceof SecurityToken) {
            return (SecurityToken)message.getContextualProperty(SecurityConstants.TOKEN);
        }

        // Get the TokenStore
        TokenStore tokenStore = getTokenStore(message);
        if (tokenStore == null) {
            return null;
        }

        String id = (String)message.getContextualProperty(SecurityConstants.TOKEN_ID);
        if (id != null) {
            return tokenStore.getToken(id);
        }
        return null;
    }

}
