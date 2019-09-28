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

package org.apache.cxf.ws.security.trust.claims;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;

/**
 * This CallbackHandler implementation creates a Claims Element for a "role" ClaimType and
 * stores it on the ClaimsCallback object.
 */
public class RoleClaimsCallbackHandler implements CallbackHandler {

    public void handle(Callback[] callbacks)
        throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof ClaimsCallback) {
                ClaimsCallback callback = (ClaimsCallback) callbacks[i];
                callback.setClaims(createClaims());

            } else {
                throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
            }
        }
    }

    /**
     * Create a Claims Element for a "role"
     */
    private Element createClaims() {
        Document doc = DOMUtils.getEmptyDocument();
        Element claimsElement =
            doc.createElementNS("http://docs.oasis-open.org/ws-sx/ws-trust/200512", "Claims");
        claimsElement.setAttributeNS(null, "Dialect", "http://schemas.xmlsoap.org/ws/2005/05/identity");
        Element claimType =
            doc.createElementNS("http://schemas.xmlsoap.org/ws/2005/05/identity", "ClaimType");
        claimType.setAttributeNS(null, "Uri", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role");
        claimsElement.appendChild(claimType);
        return claimsElement;
    }

}
