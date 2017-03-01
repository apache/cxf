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

package org.apache.cxf.ws.policy;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.extension.BusExtension;


/**
 * PolicyBuilderImpl is an implementation of the PolicyBuilder interface,
 * provides methods to create Policy and PolicyReferenceObjects
 * from DOM elements, but also from an input stream etc.
 */
@NoJSR250Annotations
public class PolicyBuilderImpl extends org.apache.neethi.PolicyBuilder
    implements PolicyBuilder, BusExtension {
    private Bus bus;

    public PolicyBuilderImpl() {
    }

    public PolicyBuilderImpl(Bus theBus) {
        super(null);
        setBus(theBus);
    }

    public Class<?> getRegistrationType() {
        return PolicyBuilder.class;
    }

    public void setAssertionBuilderRegistry(AssertionBuilderRegistry reg) {
        factory = reg;
    }

    public final void setBus(Bus theBus) {
        bus = theBus;
        if (bus != null) {
            theBus.setExtension(this, PolicyBuilder.class);
            AssertionBuilderRegistry reg = theBus.getExtension(AssertionBuilderRegistry.class);
            if (reg != null) {
                factory = reg;
            }
            org.apache.cxf.ws.policy.PolicyEngine e
                = bus.getExtension(org.apache.cxf.ws.policy.PolicyEngine.class);
            if (e != null) {
                this.setPolicyRegistry(e.getRegistry());
            }
        }
    }

    public Bus getBus() {
        return bus;
    }

}
