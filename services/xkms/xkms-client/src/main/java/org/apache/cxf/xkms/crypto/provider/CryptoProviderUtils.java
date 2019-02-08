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

package org.apache.cxf.xkms.crypto.provider;

import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.xkms.crypto.CryptoProviderException;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSPasswordCallback;

final class CryptoProviderUtils {

    private CryptoProviderUtils() {
    }

    public static Properties loadKeystoreProperties(Message message, String propKey) {
        Object o = SecurityUtils.getSecurityPropertyValue(propKey, message);
        if (o == null) {
            throw new CryptoProviderException("Keystore properties path is not defined");
        }

        return SecurityUtils.loadProperties(o);
    }

    public static String getKeystoreAlias(Properties keystoreProps) {
        String keystoreAlias = null;

        if (keystoreProps.containsKey(Merlin.KEYSTORE_ALIAS)) {
            keystoreAlias = keystoreProps.getProperty(Merlin.KEYSTORE_ALIAS);
        }

        if (keystoreAlias == null) {
            throw new CryptoProviderException("Alias is not found in keystore properties file: "
                                              + Merlin.KEYSTORE_ALIAS);
        }

        return keystoreAlias;
    }

    public static String getCallbackPwdFromMessage(Message message, String userName, int usage) {
        // Then try to get the password from the given callback handler
        try {
            CallbackHandler handler = SecurityUtils.getCallbackHandler(message);
            if (handler == null) {
                throw new CryptoProviderException("No callback handler and no password available");
            }

            return getCallbackPwd(userName, usage, handler);
        } catch (Exception ex) {
            throw new CryptoProviderException("No callback handler and no password available", ex);
        }
    }

    public static String getCallbackPwd(String userName, int usage, CallbackHandler handler) {
        if (handler == null) {
            return null;
        }
        WSPasswordCallback[] cb = {
            new WSPasswordCallback(userName, usage)
        };
        try {
            handler.handle(cb);
        } catch (Exception e) {
            throw new CryptoProviderException("Cannot get password from callback: " + e, e);
        }

        // get the password
        return cb[0].getPassword();
    }

}
