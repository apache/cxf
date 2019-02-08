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
package org.apache.cxf.sts.operation;

import org.apache.cxf.sts.token.realm.UsernameTokenRealmCodec;
import org.apache.wss4j.dom.message.token.UsernameToken;

/**
 * This class defines a pluggable way to return a realm associated with a UsernameToken.
 */
public class CustomUsernameTokenRealmCodec implements UsernameTokenRealmCodec {

    /**
     * Get the realm associated with the UsernameToken parameter
     * @param usernameToken a WSS4J UsernameToken object
     * @return the realm associated with the UsernameToken parameter
     */
    public String getRealmFromToken(UsernameToken usernameToken) {
        if ("alice".equals(usernameToken.getName())) {
            return "A";
        }
        return null;
    }

}
