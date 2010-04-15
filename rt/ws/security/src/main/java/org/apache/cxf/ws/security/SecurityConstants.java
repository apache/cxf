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

package org.apache.cxf.ws.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 */
public final class SecurityConstants {
    public static final String USERNAME = "ws-security.username";
    public static final String PASSWORD = "ws-security.password";
    public static final String CALLBACK_HANDLER = "ws-security.callback-handler";
    
    public static final String SIGNATURE_USERNAME = "ws-security.signature.username";
    public static final String SIGNATURE_PROPERTIES = "ws-security.signature.properties";
    
    public static final String ENCRYPT_USERNAME = "ws-security.encryption.username";
    public static final String ENCRYPT_PROPERTIES = "ws-security.encryption.properties";
    
    public static final String SIGNATURE_CRYPTO = "ws-security.signature.crypto";
    public static final String ENCRYPT_CRYPTO = "ws-security.encryption.crypto";

    public static final String TOKEN = "ws-security.token";
    public static final String TOKEN_ID = "ws-security.token.id";

    public static final String STS_CLIENT = "ws-security.sts.client";
    public static final String STS_APPLIES_TO = "ws-security.sts.applies-to";
    
    public static final String TIMESTAMP_TTL = "ws-security.timestamp.timeToLive";
    
    //WebLogic and WCF always encrypt UsernameTokens whenever possible
    //See:  http://e-docs.bea.com/wls/docs103/webserv_intro/interop.html
    //Be default, we will encrypt as well for interop reasons.  However, this
    //setting can be set to false to turn that off. 
    public static final String ALWAYS_ENCRYPT_UT = "ws-security.username-token.always.encrypted";
    
    /**
     * WCF's trust server sometimes will encrypt the token in the response IN ADDITION TO
     * the full security on the message. These properties control the way the STS client
     * will decrypt the EncryptedData elements in the response
     * 
     * These are also used by the STSClient to send/process any RSA/DSAKeyValue tokens 
     * used if the KeyType is "PublicKey" 
     */
    public static final String STS_TOKEN_CRYPTO = "ws-security.sts.token.crypto";
    public static final String STS_TOKEN_PROPERTIES = "ws-security.sts.token.properties";
    public static final String STS_TOKEN_USERNAME = "ws-security.sts.token.username";
    
    public static final String STS_TOKEN_DO_CANCEL = "ws-security.sts.token.do.cancel";

    public static final Set<String> ALL_PROPERTIES;
    
    static {
        Set<String> s = new HashSet<String>(Arrays.asList(new String[] {
            USERNAME, PASSWORD, CALLBACK_HANDLER, 
            SIGNATURE_USERNAME, SIGNATURE_PROPERTIES, SIGNATURE_CRYPTO,
            ENCRYPT_USERNAME, ENCRYPT_PROPERTIES, ENCRYPT_CRYPTO,
            TOKEN, TOKEN_ID, STS_CLIENT, STS_TOKEN_PROPERTIES, STS_TOKEN_CRYPTO,
            STS_TOKEN_DO_CANCEL, TIMESTAMP_TTL, ALWAYS_ENCRYPT_UT
        }));
        ALL_PROPERTIES = Collections.unmodifiableSet(s);
    }
    
    private SecurityConstants() {
        //utility class
    }
}
