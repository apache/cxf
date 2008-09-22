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

/**
 * 
 */
public final class SecurityConstants {
    public static final String USERNAME = "ws-security.username";
    public static final String PASSWORD = "ws-security.password";
    public static final String CALLBACK_HANDLER = "ws-security.callback-handler";
    
    public static final String SIGNATURE_PROPERTIES = "ws-security.signature.properties";
    public static final String ENCRYPT_USERNAME = "ws-security.encryption.username";
    public static final String ENCRYPT_PROPERTIES = "ws-security.encryption.properties";
    
    private SecurityConstants() {
        //utility class
    }
}
