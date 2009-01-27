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

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.message.token.SecurityContextToken;
import org.apache.ws.security.processor.Processor;

/**
 * a custom processor that inserts itself into the results vector
 */
public class CustomProcessor implements Processor {
    
    @SuppressWarnings("unchecked")
    public final void 
    handleToken(
        final org.w3c.dom.Element elem, 
        final Crypto crypto, 
        final Crypto decCrypto,
        final javax.security.auth.callback.CallbackHandler cb, 
        final WSDocInfo wsDocInfo, 
        final java.util.Vector returnResults,
        final WSSConfig config
    ) throws WSSecurityException {
        final java.util.Map result = 
            new WSSecurityEngineResult(
                WSConstants.SIGN, 
                (SecurityContextToken) null
            );
        result.put("foo", this);
        returnResults.add(result);
    }

    public final String getId() {
        return getClass().getName();
    }
}
