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
package org.apache.cxf.sts.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import org.apache.cxf.rt.security.claims.ClaimCollection;

/**
 * This class contains values that have been extracted from a RequestSecurityToken corresponding to
 * various token requirements.
 */
public class TokenRequirements {

    private String tokenType;
    private Element appliesTo;
    private String context;
    private ReceivedToken validateTarget;
    private ReceivedToken onBehalfOf;
    private ReceivedToken actAs;
    private ReceivedToken cancelTarget;
    private ReceivedToken renewTarget;
    private Lifetime lifetime;
    private ClaimCollection primaryClaims;
    private ClaimCollection secondaryClaims;
    private Renewing renewing;
    private Participants participants;
    private final List<Object> customContent = new ArrayList<>();

    public Renewing getRenewing() {
        return renewing;
    }

    public void setRenewing(Renewing renewing) {
        this.renewing = renewing;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public ReceivedToken getCancelTarget() {
        return cancelTarget;
    }

    public void setCancelTarget(ReceivedToken cancelTarget) {
        this.cancelTarget = cancelTarget;
    }

    public ReceivedToken getRenewTarget() {
        return renewTarget;
    }

    public void setRenewTarget(ReceivedToken renewTarget) {
        this.renewTarget = renewTarget;
    }

    public Element getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(Element appliesTo) {
        this.appliesTo = appliesTo;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public ReceivedToken getValidateTarget() {
        return validateTarget;
    }

    public void setValidateTarget(ReceivedToken validateTarget) {
        this.validateTarget = validateTarget;
    }

    public ReceivedToken getOnBehalfOf() {
        return onBehalfOf;
    }

    public void setOnBehalfOf(ReceivedToken onBehalfOf) {
        this.onBehalfOf = onBehalfOf;
    }

    public ReceivedToken getActAs() {
        return actAs;
    }

    public void setActAs(ReceivedToken actAs) {
        this.actAs = actAs;
    }

    public Lifetime getLifetime() {
        return lifetime;
    }

    public void setLifetime(Lifetime lifetime) {
        this.lifetime = lifetime;
    }

    public ClaimCollection getPrimaryClaims() {
        return primaryClaims;
    }

    public void setPrimaryClaims(ClaimCollection primaryClaims) {
        this.primaryClaims = primaryClaims;
    }

    public ClaimCollection getSecondaryClaims() {
        return secondaryClaims;
    }

    public void setSecondaryClaims(ClaimCollection secondaryClaims) {
        this.secondaryClaims = secondaryClaims;
    }

    public Participants getParticipants() {
        return participants;
    }

    public void setParticipants(Participants participants) {
        this.participants = participants;
    }

    public List<Object> getCustomContent() {
        return Collections.unmodifiableList(customContent);
    }

    public void addCustomContent(Object customElement) {
        if (customElement != null) {
            this.customContent.add(customElement);
        }
    }

}