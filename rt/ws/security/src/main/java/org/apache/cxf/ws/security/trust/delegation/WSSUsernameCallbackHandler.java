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

package org.apache.cxf.ws.security.trust.delegation;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.dom.message.token.UsernameToken;

/**
 * This CallbackHandler implementation obtains a username via the jaxws property
 * "security.username", as defined in SecurityConstants, and creates a wss UsernameToken
 * (with no password) to be used as the delegation token.
 */
public class WSSUsernameCallbackHandler implements CallbackHandler {

    public void handle(Callback[] callbacks)
        throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof DelegationCallback) {
                DelegationCallback callback = (DelegationCallback) callbacks[i];
                Message message = callback.getCurrentMessage();

                String username =
                    (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.USERNAME, message);
                if (username != null) {
                    Node contentNode = message.getContent(Node.class);
                    final Document doc;
                    if (contentNode != null) {
                        doc = contentNode.getOwnerDocument();
                    } else {
                        doc = DOMUtils.getEmptyDocument();
                    }
                    UsernameToken usernameToken = createWSSEUsernameToken(username, doc);
                    callback.setToken(usernameToken.getElement());
                }
            } else {
                throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
            }
        }
    }

    private UsernameToken createWSSEUsernameToken(String username, Document doc) {
        UsernameToken usernameToken = new UsernameToken(true, doc, null);
        usernameToken.setName(username);
        usernameToken.addWSUNamespace();
        usernameToken.addWSSENamespace();
        usernameToken.setID("id-" + username);

        return usernameToken;
    }

}
