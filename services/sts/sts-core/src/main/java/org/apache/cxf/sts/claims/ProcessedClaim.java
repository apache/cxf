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

package org.apache.cxf.sts.claims;

import java.security.Principal;

import org.apache.cxf.rt.security.claims.Claim;

/**
 * This represents a Claim that has been processed by a ClaimsHandler instance.
 */
public class ProcessedClaim extends Claim {

    /**
     *
     */
    private static final long serialVersionUID = -336574019841442184L;
    private String issuer;
    private String originalIssuer;
    private transient Principal principal;

    public ProcessedClaim() {
    }

    public ProcessedClaim(ProcessedClaim processedClaim) {
        super(processedClaim);
        issuer = processedClaim.issuer;
        originalIssuer = processedClaim.originalIssuer;
        principal = processedClaim.principal;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getOriginalIssuer() {
        return originalIssuer;
    }

    public void setOriginalIssuer(String originalIssuer) {
        this.originalIssuer = originalIssuer;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(128);
        builder.append("ProcessedClaim [");
        builder.append(super.toString());
        builder.append(", issuer=");
        builder.append(issuer);
        builder.append(", originalIssuer=");
        builder.append(originalIssuer);
        builder.append(", principal=");
        builder.append(principal);
        builder.append(']');
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((issuer == null)
            ? 0
            : issuer.hashCode());
        result = prime * result + ((originalIssuer == null)
            ? 0
            : originalIssuer.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ProcessedClaim)) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        ProcessedClaim other = (ProcessedClaim)obj;
        if (issuer == null) {
            if (other.issuer != null) {
                return false;
            }
        } else if (!issuer.equals(other.issuer)) {
            return false;
        }
        if (originalIssuer == null) {
            if (other.originalIssuer != null) {
                return false;
            }
        } else if (!originalIssuer.equals(other.originalIssuer)) {
            return false;
        }
        return true;
    }

    @Override
    public ProcessedClaim clone() { //NOPMD
        super.clone(); // Checkstyle requires this call
        return new ProcessedClaim(this);
    }
}
