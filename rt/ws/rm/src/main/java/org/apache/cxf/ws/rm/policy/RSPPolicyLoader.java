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

package org.apache.cxf.ws.rm.policy;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.ws.policy.AssertionBuilderLoader;
import org.apache.cxf.ws.policy.AssertionBuilderRegistry;
import org.apache.cxf.ws.policy.PolicyInterceptorProviderLoader;
import org.apache.cxf.ws.policy.PolicyInterceptorProviderRegistry;

/**
 * Policy loader for WS-I RSP assertion. This provides the hooks for Neethi to handle the assertion.
 */
@NoJSR250Annotations
public final class RSPPolicyLoader implements PolicyInterceptorProviderLoader, AssertionBuilderLoader {
    Bus bus;

    public RSPPolicyLoader(Bus b) {
        bus = b;
        registerBuilders();
        try {
            registerProviders();
        } catch (Throwable t) {
            // We'll ignore this as the policy framework will then not find the providers and error out at
            // that point. If nothing uses WS-I RSP no warnings/errors will display
        }
    }

    public void registerBuilders() {
        AssertionBuilderRegistry reg = bus.getExtension(AssertionBuilderRegistry.class);
        if (reg == null) {
            return;
        }
        reg.registerBuilder(new RSPAssertionBuilder());
    }

    public void registerProviders() {
        //interceptor provider for the policy
        PolicyInterceptorProviderRegistry reg = bus.getExtension(PolicyInterceptorProviderRegistry.class);
        if (reg == null) {
            return;
        }
        reg.register(new RMPolicyInterceptorProvider(bus));
    }
}