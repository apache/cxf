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
package org.apache.cxf.ws.security.policy.model;

import org.apache.cxf.ws.security.policy.SPConstants;

public abstract class Binding extends AbstractSecurityAssertion implements AlgorithmWrapper {

    private AlgorithmSuite algorithmSuite;
    private boolean includeTimestamp;
    private Layout layout;
    private SupportingToken signedSupportingToken;
    private SupportingToken signedEndorsingSupportingTokens;

    public Binding(SPConstants version) {
        super(version);
        layout = new Layout(version);
    }

    /**
     * @return Returns the algorithmSuite.
     */
    public AlgorithmSuite getAlgorithmSuite() {
        return algorithmSuite;
    }

    /**
     * @param algorithmSuite The algorithmSuite to set.
     */
    public void setAlgorithmSuite(AlgorithmSuite algorithmSuite) {
        this.algorithmSuite = algorithmSuite;
    }

    /**
     * @return Returns the includeTimestamp.
     */
    public boolean isIncludeTimestamp() {
        return includeTimestamp;
    }

    /**
     * @param includeTimestamp The includeTimestamp to set.
     */
    public void setIncludeTimestamp(boolean includeTimestamp) {
        this.includeTimestamp = includeTimestamp;
    }

    /**
     * @return Returns the layout.
     */
    public Layout getLayout() {
        return layout;
    }

    /**
     * @param layout The layout to set.
     */
    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    public SupportingToken getSignedEndorsingSupportingTokens() {
        return signedEndorsingSupportingTokens;
    }

    public void setSignedEndorsingSupportingTokens(SupportingToken signedEndorsingSupportingTokens) {
        this.signedEndorsingSupportingTokens = signedEndorsingSupportingTokens;
    }

    public SupportingToken getSignedSupportingToken() {
        return signedSupportingToken;
    }

    public void setSignedSupportingToken(SupportingToken signedSupportingToken) {
        this.signedSupportingToken = signedSupportingToken;
    }
}
