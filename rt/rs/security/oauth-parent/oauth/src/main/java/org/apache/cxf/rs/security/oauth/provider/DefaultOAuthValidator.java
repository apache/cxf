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

package org.apache.cxf.rs.security.oauth.provider;

import java.io.IOException;
import java.net.URISyntaxException;

import net.oauth.OAuth;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.SimpleOAuthValidator;
import org.apache.cxf.rs.security.oauth.data.Token;

/**
 * The utility OAuth validator which is primarily used
 * by the runtime to validate that the issued tokens have not expired.
 * Note that the runtime does validate OAuth signatures separately.
 */
public class DefaultOAuthValidator extends SimpleOAuthValidator {

    public DefaultOAuthValidator() {
    }

    public void checkSingleParameter(OAuthMessage message) throws OAuthException, IOException,
        URISyntaxException {
        super.checkSingleParameters(message);
    }

    public void validateToken(Token token, OAuthDataProvider provider)
        throws OAuthProblemException {
        if (token == null) {
            throw new OAuthProblemException(OAuth.Problems.TOKEN_REJECTED);
        }
        long issuedAt = token.getIssuedAt();
        long lifetime = token.getLifetime();
        if (lifetime != -1
            && (issuedAt + lifetime < (System.currentTimeMillis() / 1000L))) {
            provider.removeToken(token);
            throw new OAuthProblemException(OAuth.Problems.TOKEN_EXPIRED);
        }
    }
}
