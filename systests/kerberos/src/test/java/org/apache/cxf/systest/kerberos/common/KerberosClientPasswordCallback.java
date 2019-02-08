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

package org.apache.cxf.systest.kerberos.common;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.message.token.KerberosSecurity;

/**
 * A CallbackHandler implementation for the kerberos client.
 */
public class KerberosClientPasswordCallback implements CallbackHandler {

    private String username = "alice";
    private String password = "alice";
    private String servicePrincipal = "bob@service.ws.apache.org";

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback)callbacks[i];
                nameCallback.setName(username);
            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback passwordCallback = (PasswordCallback)callbacks[i];
                passwordCallback.setPassword(password.toCharArray());
            } else if (callbacks[i] instanceof WSPasswordCallback) {
                WSPasswordCallback wsPasswordCallback = (WSPasswordCallback)callbacks[i];
                // Get a custom (Kerberos) token directly using the WSS4J APIs
                if (wsPasswordCallback.getUsage() == WSPasswordCallback.CUSTOM_TOKEN) {
                    KerberosSecurity kerberosSecurity = new KerberosSecurity(DOMUtils.getEmptyDocument());

                    try {
                        kerberosSecurity.retrieveServiceTicket(username, this, servicePrincipal,
                                                  false, false, null);
                        kerberosSecurity.addWSUNamespace();
                        WSSConfig wssConfig = WSSConfig.getNewInstance();
                        kerberosSecurity.setID(wssConfig.getIdAllocator().createSecureId("BST-", kerberosSecurity));

                        wsPasswordCallback.setCustomToken(kerberosSecurity.getElement());
                    } catch (WSSecurityException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public String getServicePrincipal() {
        return servicePrincipal;
    }

    public void setServicePrincipal(String servicePrincipal) {
        this.servicePrincipal = servicePrincipal;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }



}
