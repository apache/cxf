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
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.kerberos.KerberosContextAndServiceNameCallback;

/**
 *  A CallbackHandler implementation for the kerberos service.
 */
public class KerberosServicePasswordCallback extends KeystorePasswordCallback {

    private String username = "bob";
    private String password = "bob";

    public KerberosServicePasswordCallback() {
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof WSPasswordCallback) {
                super.handle(new Callback[]{callbacks[i]});
            } else if (callbacks[i] instanceof KerberosContextAndServiceNameCallback) {
                KerberosContextAndServiceNameCallback pc =
                    (KerberosContextAndServiceNameCallback)callbacks[i];
                pc.setContextName("bob");
                pc.setServiceName("bob@service.ws.apache.org");
            } else if (callbacks[i] instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback)callbacks[i];
                nameCallback.setName(username);
            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback passwordCallback = (PasswordCallback)callbacks[i];
                passwordCallback.setPassword(password.toCharArray());
            }
        }
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
