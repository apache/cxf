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

import org.apache.cxf.common.security.UsernameToken;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.message.Message;
import org.apache.wss4j.common.principal.UsernameTokenPrincipal;

public final class WSS4JTokenConverter {

    private WSS4JTokenConverter() {

    }

    public static void convertToken(Message msg, Principal p) {
        if (p instanceof UsernameTokenPrincipal) {
            UsernameTokenPrincipal utp = (UsernameTokenPrincipal)p;
            String nonce = null;
            if (utp.getNonce() != null) {
                nonce = Base64Utility.encode(utp.getNonce());
            }
            msg.put(org.apache.cxf.common.security.SecurityToken.class,
                    new UsernameToken(utp.getName(),
                                      utp.getPassword(),
                                      utp.getPasswordType(),
                                      utp.isPasswordDigest(),
                                      nonce,
                                      utp.getCreatedTime()));

        }
    }
}
