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
package org.apache.cxf.systest.ws.wssec10.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;

/**
 */

public class KeystorePasswordCallback implements CallbackHandler {
    private Map<String, String> passwords = 
        new HashMap<String, String>();
    
    public KeystorePasswordCallback() {
        passwords.put("alice", "password");
        passwords.put("bob", "password");
        passwords.put("abcd", "dcba");
        passwords.put("6e0e88f36ebb8744d470f62f604d03ea4ebe5094", "password");
    }

    /**
     * It attempts to get the password from the private 
     * alias/passwords map.
     */
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            WSPasswordCallback pc = (WSPasswordCallback)callbacks[i];
            try {
                Integer.parseInt(pc.getIdentifier());
                //Its an alias generated for a pfx file,
                //this seems to be the way sun seem to load
                //.pfc files into keystores, assigning an int value
                //as the key aliases,
                //The above is an issue when doing encrypt or signing only.
                //Perhaps using a more suitable keystore format like .jks would be better
                pc.setPassword("password");
                return;
            } catch (NumberFormatException nfe) {
                //not a pfx alias, carry on to next
            }

            String pass = passwords.get(pc.getIdentifier());
            if (pass != null) {
                pc.setPassword(pass);
                return;
            } else {
                pc.setPassword("password");
            }
        }
    } 
}
