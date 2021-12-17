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
package org.apache.cxf.rs.security.jose.jaxrs;

import java.io.IOException;
import java.util.logging.Logger;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwt.JoseJwtConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;
import org.apache.cxf.security.SecurityContext;

@PreMatching
@Priority(Priorities.AUTHENTICATION)
public abstract class AbstractJwtAuthenticationFilter extends JoseJwtConsumer implements ContainerRequestFilter {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractJwtAuthenticationFilter.class);

    private String roleClaim;
    private boolean validateAudience = true;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String encodedJwtToken = getEncodedJwtToken(requestContext);
        JwtToken token = super.getJwtToken(encodedJwtToken);

        SecurityContext securityContext = configureSecurityContext(token);
        if (securityContext != null) {
            JAXRSUtils.getCurrentMessage().put(SecurityContext.class, securityContext);
        }
    }

    protected abstract String getEncodedJwtToken(ContainerRequestContext requestContext);

    protected SecurityContext configureSecurityContext(JwtToken jwt) {
        Message m = JAXRSUtils.getCurrentMessage();
        boolean enableUnsignedJwt =
            MessageUtils.getContextualBoolean(m, JoseConstants.ENABLE_UNSIGNED_JWT_PRINCIPAL, false);

        // The token must be signed/verified with a public key to set up the security context,
        // unless we directly configure otherwise
        if (jwt.getClaims().getSubject() != null
            && (isVerifiedWithAPublicKey(jwt) || enableUnsignedJwt)) {
            return new JwtTokenSecurityContext(jwt, roleClaim);
        }
        return null;
    }

    private boolean isVerifiedWithAPublicKey(JwtToken jwt) {
        if (isJwsRequired()) {
            String alg = (String)jwt.getJwsHeader(JoseConstants.HEADER_ALGORITHM);
            SignatureAlgorithm sigAlg = SignatureAlgorithm.getAlgorithm(alg);
            return SignatureAlgorithm.isPublicKeyAlgorithm(sigAlg);
        }

        return false;
    }


    @Override
    protected void validateToken(JwtToken jwt) {
        JwtUtils.validateTokenClaims(jwt.getClaims(), getTtl(), getClockOffset(), isValidateAudience());
    }

    public String getRoleClaim() {
        return roleClaim;
    }

    public void setRoleClaim(String roleClaim) {
        this.roleClaim = roleClaim;
    }

    public boolean isValidateAudience() {
        return validateAudience;
    }

    public void setValidateAudience(boolean validateAudience) {
        this.validateAudience = validateAudience;
    }

}
