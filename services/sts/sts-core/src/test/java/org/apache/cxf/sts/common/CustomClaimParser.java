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
package org.apache.cxf.sts.common;

import java.net.URI;

import org.w3c.dom.Element;

import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.sts.claims.ClaimsParser;

public class CustomClaimParser implements ClaimsParser {

    public static final String CLAIMS_DIALECT = "http://my.custom.org/my/custom/namespace";

    public Claim parse(Element claim) {

        String claimLocalName = claim.getLocalName();
        String claimNS = claim.getNamespaceURI();
        if (CLAIMS_DIALECT.equals(claimNS) && "MyElement".equals(claimLocalName)) {
            String claimTypeUri = claim.getAttributeNS(null, "Uri");
            CustomRequestClaim response = new CustomRequestClaim();
            response.setClaimType(URI.create(claimTypeUri));
            String claimValue = claim.getAttributeNS(null, "value");
            response.addValue(claimValue);
            String scope = claim.getAttributeNS(null, "scope");
            response.setScope(scope);
            return response;
        }
        return null;
    }

    public String getSupportedDialect() {
        return CLAIMS_DIALECT;
    }

    /**
     * Extends RequestClaim class to add additional attributes
     */
    public class CustomRequestClaim extends Claim {
        /**
         *
         */
        private static final long serialVersionUID = 7407723714936495457L;
        private String scope;

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }

}
