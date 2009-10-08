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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.helpers.CastUtils;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyOperator;

public class AssertionInfoMap extends HashMap<QName, Collection<AssertionInfo>> {
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(AssertionInfoMap.class, "APIMessages");
    
    public AssertionInfoMap(Policy p) {
        this(getAssertions(p));
    }
    
    public AssertionInfoMap(Collection<PolicyAssertion> assertions) {
        super(assertions.size());
        for (PolicyAssertion a : assertions) {
            putAssertionInfo(a);
        }
    }
    
    private void putAssertionInfo(PolicyAssertion a) {
        Policy p = a.getPolicy();
        if (p != null) {
            for (PolicyAssertion na : getAssertions(p)) {
                putAssertionInfo(na);
            }
        }
        AssertionInfo ai = new AssertionInfo(a);
        Collection<AssertionInfo> ail = get(a.getName());
        if (ail == null) {
            ail = new ArrayList<AssertionInfo>();
            put(a.getName(), ail);
        }
        ail.add(ai);
    }
    
    public Collection<AssertionInfo> getAssertionInfo(QName name) {
        Collection<AssertionInfo> ail = get(name);
        return ail != null ? ail
            : CastUtils.cast(Collections.EMPTY_LIST, AssertionInfo.class);

    }
    
    public boolean supportsAlternative(Collection<PolicyAssertion> alternative,
                                       List<QName> errors) {
        boolean pass = true;
        for (PolicyAssertion a : alternative) {          
            if (!a.isAsserted(this)) {
                errors.add(a.getName());
                pass = false;
            }
        }
        return pass;
    }
    
    public void checkEffectivePolicy(Policy policy) {
        List<QName> errors = new ArrayList<QName>();
        Iterator alternatives = policy.getAlternatives();
        while (alternatives.hasNext()) {      
            List<PolicyAssertion> alternative = CastUtils.cast((List)alternatives.next(), 
                                                               PolicyAssertion.class);
            if (supportsAlternative(alternative, errors)) {
                return;
            }
        }
        StringBuilder error = new StringBuilder("\n");
        for (QName name : errors) {
            Collection<AssertionInfo> ais = getAssertionInfo(name);
            for (AssertionInfo ai : ais) {
                if (!ai.isAsserted()) {
                    error.append("\n      ");
                    error.append(name.toString());
                    if (ai.getErrorMessage() != null) {
                        error.append(": ").append(ai.getErrorMessage());
                    }
                }
            }
        }
        
        
        throw new PolicyException(new Message("NO_ALTERNATIVE_EXC", BUNDLE, error.toString()));
    }

    
    public void check() {
        for (Collection<AssertionInfo> ais : values()) {
            for (AssertionInfo ai : ais) {
                if (!ai.isAsserted()) {
                    throw new PolicyException(new org.apache.cxf.common.i18n.Message(
                        "NOT_ASSERTED_EXC", BUNDLE, ai.getAssertion().getName()));
                }
            }
        }
    }
    
    private static Collection<PolicyAssertion> getAssertions(PolicyOperator p) {
        List<PolicyComponent> pcs = 
            CastUtils.cast(p.getPolicyComponents(), PolicyComponent.class);
        if (pcs.size() == 0 || pcs.get(0) instanceof PolicyAssertion) {
            return CastUtils.cast(pcs, PolicyAssertion.class);
        }
        Collection<PolicyAssertion> assertions = new ArrayList<PolicyAssertion>();
        for (PolicyComponent pc : pcs) {
            assertions.addAll(getAssertions((PolicyOperator)pc));
        }
        return assertions;   
    }
}
