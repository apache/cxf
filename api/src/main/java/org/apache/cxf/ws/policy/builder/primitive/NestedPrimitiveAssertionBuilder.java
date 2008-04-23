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

package org.apache.cxf.ws.policy.builder.primitive;

import org.w3c.dom.Element;

import org.apache.cxf.ws.policy.AssertionBuilderRegistry;
import org.apache.cxf.ws.policy.Intersector;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.neethi.Policy;

public class NestedPrimitiveAssertionBuilder extends PrimitiveAssertionBuilder {
    
    private PolicyBuilder builder;    
    private AssertionBuilderRegistry assertionBuilderRegistry;
    
    public void setPolicyBuilder(PolicyBuilder b) {
        builder = b;
    }
        
    public void setAssertionBuilderRegistry(AssertionBuilderRegistry abr) {
        assertionBuilderRegistry = abr;
    }
    
    @Override
    public PolicyAssertion build(Element elem) {
        return new NestedPrimitiveAssertion(elem, builder, getPolicyConstants()); 
    }

    @Override
    /**
     * If the nested policies in both assertions are empty, the compatible policy
     * . 
     * The compatible policy is optional iff both assertions are optional.
     */
    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        if (!getKnownElements().contains(a.getName()) || !a.getName().equals(b.getName())) {
            return null;
        }
        
        if (null == assertionBuilderRegistry) {
            return null;
        }
                
       
        NestedPrimitiveAssertion na = (NestedPrimitiveAssertion)a;
        NestedPrimitiveAssertion nb = (NestedPrimitiveAssertion)b;        
        
        Intersector intersector = new Intersector(assertionBuilderRegistry);
        
        Policy nested = intersector.intersect(na.getPolicy(), nb.getPolicy());        
        if (null == nested) {
            return  null;
        }
        
        NestedPrimitiveAssertion compatible = 
            new NestedPrimitiveAssertion(a.getName(), a.isOptional() && b.isOptional());
        compatible.setPolicy(nested);
        
        return compatible;
    }
    
    
}
