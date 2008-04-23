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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.cxf.helpers.CastUtils;
import org.apache.neethi.All;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;

/**
 * This class contains methods dealing with policy intersection.
 * Intersection of two assertions, i.e. computation if a compatible assertion,
 * is domain specific and relies on AssertionBuilder.buildCompatible.
 * See Section 4.5 in http://www.w3.org/TR/2006/WD-ws-policy-20061117.
 */
public class Intersector {
    
    private AssertionBuilderRegistry assertionBuilderRegistry;
    private boolean strict;
    
    public Intersector(AssertionBuilderRegistry abr) {
        assertionBuilderRegistry = abr;
        strict = true;
    }
    
    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean s) {
        strict = s;
    }

    boolean compatibleAssertions(PolicyAssertion a1, PolicyAssertion a2) {
        AssertionBuilder ab = assertionBuilderRegistry.get(a1.getName());
        if (null == ab) {
            return false;
        }
        return null != ab.buildCompatible(a1, a2);
    }
        
    boolean compatibleAlternatives(Collection<PolicyAssertion> alt1, 
                                   Collection<PolicyAssertion> alt2) {
        if (alt1.isEmpty() || alt2.isEmpty()) {
            return true;
        }
        if (strict) {
            for (PolicyAssertion a1 : alt1) {
                if (null == findCompatibleAssertion(a1, alt2)) {
                    return false;
                }
            }
            for (PolicyAssertion a2 : alt2) {
                if (null == findCompatibleAssertion(a2, alt1)) {
                    return false;
                }
            }
            return true;
        }
        // Lax intersection not supported as neethi does not support Ignorable yet.
        throw new UnsupportedOperationException("Lax intersection of assertions is not supported "
                                                + "because the Ignorable attribute is not supported.");
    }
    
    boolean compatiblePolicies(Policy p1, Policy p2) {
        Iterator i1 = p1.getAlternatives();
        while (i1.hasNext()) {
            Collection<PolicyAssertion> alt1 = 
                CastUtils.cast((Collection)i1.next(), PolicyAssertion.class);
            Iterator i2 = p2.getAlternatives();
            while (i2.hasNext()) {                
                Collection<PolicyAssertion> alt2 = 
                    CastUtils.cast((Collection)i2.next(), PolicyAssertion.class);
                if (compatibleAlternatives(alt1, alt2)) {
                    return true;                    
                }
            }             
            return false;
        }        
        return true;
    }
    
    public PolicyAssertion intersect(PolicyAssertion a1, PolicyAssertion a2) {
        AssertionBuilder ab = assertionBuilderRegistry.get(a1.getName());
        if (null == ab) {
            return null;
        }
        return ab.buildCompatible(a1, a2);
    }
    
    public Collection<PolicyAssertion> intersect(Collection<PolicyAssertion> alt1, 
                                                 Collection<PolicyAssertion> alt2) {
        if (!compatibleAlternatives(alt1, alt2)) {
            return null;
        }
        Collection<PolicyAssertion> intersection = 
            new ArrayList<PolicyAssertion>();
        intersection.addAll(alt1);
        intersection.addAll(alt2);
        return intersection;
    }
    
    public Policy intersect(Policy p1, Policy p2) {
        if (!compatiblePolicies(p1, p2)) {
            return  null;
        }
        
        Policy compatible = new Policy();
        ExactlyOne eo = new ExactlyOne();
                
        Iterator i1 = p1.getAlternatives();
        while (i1.hasNext()) {
            List<PolicyAssertion> alt1 = 
                CastUtils.cast((List)i1.next(), PolicyAssertion.class);
            Iterator i2 = p2.getAlternatives();
            while (i2.hasNext()) {                
                List<PolicyAssertion> alt2 = 
                    CastUtils.cast((List)i2.next(), PolicyAssertion.class);
                if (compatibleAlternatives(alt1, alt2)) {
                    All all = new All();
                    all.addPolicyComponents(alt1);
                    all.addPolicyComponents(alt2);
                    eo.addPolicyComponent(all);
                }
            }            
        }
        
        if (!eo.isEmpty()) {
            compatible.addPolicyComponent(eo);
        }
        
        return compatible;
    }
    
    private PolicyAssertion findCompatibleAssertion(
        PolicyAssertion assertion, Collection<PolicyAssertion> alt) {
        for (PolicyAssertion a : alt) {
            PolicyAssertion compatible = intersect(assertion, a);
            if (null != compatible) {
                return compatible;
            }
        }
        return null;
    } 
}
