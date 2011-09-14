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
import org.apache.neethi.Assertion;
import org.apache.neethi.PolicyComponent;

public abstract class AbstractSecurityAssertion implements Assertion {
    protected final SPConstants constants;

    private boolean isOptional;
    private boolean ignorable;
    private boolean normalized;

    public AbstractSecurityAssertion(SPConstants version) {
        constants = version;
    }
    public final SPConstants getSPConstants() {
        return constants;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public void setOptional(boolean optional) {
        this.isOptional = optional;
    }
    public boolean isIgnorable() {
        return ignorable;
    }

    public void setIgnorable(boolean ignorable) {
        this.ignorable = ignorable;
    }

    public short getType() {
        return org.apache.neethi.Constants.TYPE_ASSERTION;
    }

    public boolean equal(PolicyComponent policyComponent) {
        return policyComponent == this;
    }

    public void setNormalized(boolean normalized) {
        this.normalized = normalized;
    }

    public boolean isNormalized() {
        return normalized;
    }

    public PolicyComponent normalize() {
        return this;
    }

}
