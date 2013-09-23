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
public class Claim implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -1151700035195497499L;
    private URI claimType;
    private String issuer;
    private String originalIssuer;
    private transient Principal principal;
    private List<String> values = new ArrayList<String>(1);
    private List<?> customValues;

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

    public List<?> getCustomValues() {
        return customValues;
    }

    public void setCustomValues(List<?> customValues) {
        this.customValues = customValues;
    }

}
