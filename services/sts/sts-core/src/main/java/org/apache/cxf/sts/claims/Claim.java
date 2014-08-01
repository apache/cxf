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

import java.io.Serializable;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * This represents a Claim that has been processed by a ClaimsHandler instance.
 */
public class Claim implements Serializable, Cloneable {

    /**
     * 
     */
    private static final long serialVersionUID = -1151700035195497499L;
    private URI claimType;
    private String issuer;
    private String originalIssuer;
    private transient Principal principal;
    private List<String> values = new ArrayList<String>(1);

    public Claim() {
    }
    
    /**
     * Create a clone of the provided claim.
     * 
     * @param claim Claim to be cloned. Value cannot be null.
     */
    public Claim(Claim claim) {
        if (claim == null) {
            throw new IllegalArgumentException("Claim cannot be null");
        }
        if (claim.claimType != null) {
            claimType = URI.create(claim.claimType.toString());
        }
        issuer = claim.issuer;
        originalIssuer = claim.originalIssuer;
        values.addAll(claim.values);
        principal = claim.principal;
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

    public URI getClaimType() {
        return claimType;
    }

    public void setClaimType(URI claimType) {
        this.claimType = claimType;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    public void setValues(List<String> values) {
        this.values.clear();
        this.values.addAll(values);
    }

    public void addValue(String s) {
        this.values.add(s);
    }
    
    public List<String> getValues() {
        return values;
    }

    @Deprecated
    public void setValue(String value) {
        this.values.clear();
        if (value != null) {
            this.values.add(value);
        }
    }
    @Deprecated
    public String getValue() {
        if (this.values.size() == 0) {
            return null;
        } else if (this.values.size() == 1) {
            return this.values.get(0);
        }
        throw new IllegalStateException("Claim has multiple values");
    }
    
    @Override
    public Claim clone() {
        try {
            super.clone(); // Checkstyle requires this call
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return new Claim(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((claimType == null)
            ? 0
            : claimType.hashCode());
        result = prime * result + ((issuer == null)
            ? 0
            : issuer.hashCode());
        result = prime * result + ((originalIssuer == null)
            ? 0
            : originalIssuer.hashCode());
        result = prime * result + ((values == null)
            ? 0
            : values.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Claim)) {
            return false;
        }
        Claim other = (Claim)obj;
        if (claimType == null) {
            if (other.claimType != null) {
                return false;
            }
        } else if (!claimType.equals(other.claimType)) {
            return false;
        }
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
        if (values == null) {
            if (other.values != null) {
                return false;
            }
        } else if (!values.equals(other.values)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Claim [values=");
        builder.append(values);
        builder.append(", claimType=");
        builder.append(claimType);
        builder.append(", issuer=");
        builder.append(issuer);
        builder.append(", originalIssuer=");
        builder.append(originalIssuer);
        builder.append(", principal=");
        builder.append(principal);
        builder.append("]");
        return builder.toString();
    }
    
}
