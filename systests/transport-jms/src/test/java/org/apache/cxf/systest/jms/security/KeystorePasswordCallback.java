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

package org.apache.cxf.systest.jms.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;

/**
 * A CallbackHandler implementation for keystores.
 */
public class KeystorePasswordCallback implements CallbackHandler {

    private Map<String, String> passwords =
        new HashMap<>();

    public KeystorePasswordCallback() {
        passwords.put("Alice", "abcd!1234");
        passwords.put("alice", "password");
        passwords.put("Bob", "abcd!1234");
        passwords.put("bob", "password");
        passwords.put("abcd", "dcba");
        passwords.put("6e0e88f36ebb8744d470f62f604d03ea4ebe5094", "password");
        passwords.put("wss40rev", "security");
        passwords.put("morpit", "password");
    }

    /**
     * It attempts to get the password from the private
     * alias/passwords map.
     */
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            WSPasswordCallback pc = (WSPasswordCallback)callbacks[i];
            if (pc.getUsage() == WSPasswordCallback.PASSWORD_ENCRYPTOR_PASSWORD) {
                pc.setPassword("this-is-a-secret");
            } else {
                String pass = passwords.get(pc.getIdentifier());
                if (pass != null) {
                    pc.setPassword(pass);
                    return;
                }
                pc.setPassword("password");
            }
        }
    }


}
