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

package org.apache.cxf.sts.operation;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.provider.TokenProviderUtils;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.dom.WSConstants;

/**
 * A Dummy TokenProvider for use in the unit tests. It mocks up a dummy BinarySecurityToken.
 */
public class DummyTokenProvider implements TokenProvider {

    public static final String TOKEN_TYPE =
        "http://dummy-token-type.com/dummy";
    public static final String BASE64_NS =
        WSConstants.SOAPMESSAGE_NS + "#Base64Binary";

    public boolean canHandleToken(String tokenType) {
        return TOKEN_TYPE.equals(tokenType);
    }

    public boolean canHandleToken(String tokenType, String realm) {
        return canHandleToken(tokenType);
    }

    public TokenProviderResponse createToken(TokenProviderParameters tokenParameters) {
        try {
            Document doc = DOMUtils.getEmptyDocument();

            // Mock up a dummy BinarySecurityToken
            String id = "BST-1234";
            BinarySecurity bst = new BinarySecurity(doc);
            bst.addWSSENamespace();
            bst.addWSUNamespace();
            bst.setID(id);
            bst.setValueType(TOKEN_TYPE);
            bst.setEncodingType(BASE64_NS);
            bst.setToken("12345678".getBytes());

            TokenProviderResponse response = new TokenProviderResponse();
            response.setToken(bst.getElement());
            response.setTokenId(id);

            if (tokenParameters.isEncryptToken()) {
                Element el = TokenProviderUtils.encryptToken(bst.getElement(), response.getTokenId(),
                                                        tokenParameters.getStsProperties(),
                                                        tokenParameters.getEncryptionProperties(),
                                                        tokenParameters.getKeyRequirements(),
                                                        tokenParameters.getMessageContext());
                response.setToken(el);
            } else {
                response.setToken(bst.getElement());
            }

            return response;
        } catch (Exception e) {
            throw new STSException("Can't serialize SAML assertion", e, STSException.REQUEST_FAILED);
        }
    }

}
