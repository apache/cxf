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

package org.apache.cxf.ws.policy.attachment;

import java.util.ResourceBundle;

import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.PolicyEngineImpl;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.policy.PolicyProvider;
import org.apache.cxf.ws.policy.attachment.reference.ReferenceResolver;
import org.apache.cxf.ws.policy.attachment.reference.RemoteReferenceResolver;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyReference;
import org.apache.neethi.PolicyRegistry;

/**
 * 
 */
public abstract class AbstractPolicyProvider implements PolicyProvider {
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(AbstractPolicyProvider.class);
    
    protected PolicyBuilder builder;
    protected PolicyRegistry registry; 
    protected Bus bus;
    
    protected AbstractPolicyProvider() {
        this(null);
    }
    
    protected AbstractPolicyProvider(Bus b) {
        setBus(b);
    }
    
    public final void setBus(Bus b) {
        bus = b;
        if (null != bus) {
            setBuilder(bus.getExtension(PolicyBuilder.class));
            PolicyEngine pe = (PolicyEngine)bus.getExtension(PolicyEngine.class);
            if (pe != null) {
                setRegistry(pe.getRegistry());
                ((PolicyEngineImpl)pe).getPolicyProviders().add(this);
            }
        }
    }
    
    public final void setBuilder(PolicyBuilder b) {
        builder = b;
    }
    
    public final void setRegistry(PolicyRegistry r) {
        registry = r;
    }  
    
    
    protected Policy resolveExternal(PolicyReference ref,  String baseURI) {
        Policy resolved = registry.lookup(ref.getURI());
        if (null != resolved) {
            return resolved;
        }
        ReferenceResolver resolver = new RemoteReferenceResolver(baseURI, builder);
        return resolver.resolveReference(ref.getURI());
    }
    
    protected boolean isExternal(PolicyReference ref) {
        return !ref.getURI().startsWith("#");
    }
    
    protected void checkResolved(PolicyReference ref, Policy p) {
        if (null == p) {
            throw new PolicyException(new Message("UNRESOLVED_POLICY_REFERENCE_EXC", BUNDLE, ref.getURI()));
        }
    }
}
