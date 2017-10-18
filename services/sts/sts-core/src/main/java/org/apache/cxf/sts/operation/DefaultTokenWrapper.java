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
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;

/**
 * The default implementation of TokenWrapper. For DOM Elements it just set the token directly on the
 * RSTT. If it's a String (as per the case of JWT Tokens), it puts a "TokenWrapper" wrapper around the
 * token.
 */
public class DefaultTokenWrapper implements TokenWrapper {

    /**
     * Wrap the Token parameter and set it on the RequestedSecurityTokenType parameter
     */
    public void wrapToken(Object token, RequestedSecurityTokenType requestedTokenType) {
        if (token instanceof String) {
            Document doc = DOMUtils.getEmptyDocument();
            Element tokenWrapper = doc.createElementNS(null, "TokenWrapper");
            tokenWrapper.setTextContent((String)token);
            requestedTokenType.setAny(tokenWrapper);
        } else {
            requestedTokenType.setAny(token);
        }
    }

}
