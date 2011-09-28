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

package org.apache.cxf.sts.token.realm;


/**
 * This class defines some properties that are associated with a realm for the SAMLTokenProvider and
 * SAMLTokenValidator.
 */
public class SAMLRealm {
    private String issuer;
    private String signatureAlias;

    /**
     * Get the issuer of this SAML realm
     * @return the issuer of this SAML realm
     */
    public String getIssuer() {
        return issuer;
    }
    
    /**
     * Set the issuer of this SAML realm
     * @param issuer the issuer of this SAML realm
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    
    /**
     * Get the signature alias to use for this SAML realm
     * @return the signature alias to use for this SAML realm
     */
    public String getSignatureAlias() {
        return signatureAlias;
    }
    
    /**
     * Set the signature alias to use for this SAML realm
     * @param signatureAlias the signature alias to use for this SAML realm
     */
    public void setSignatureAlias(String signatureAlias) {
        this.signatureAlias = signatureAlias;
    }
    
}
