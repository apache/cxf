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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

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
        super(assertions.size() < 6 ? 6 : assertions.size());
        for (PolicyAssertion a : assertions) {
            putAssertionInfo(a);
        }
    }

    private void putAssertionInfo(PolicyAssertion a) {
        Policy p = a.getPolicy();
        if (p != null) {
            List<PolicyAssertion> pcs = new ArrayList<PolicyAssertion>();
            getAssertions(p, pcs);
            for (PolicyAssertion na : pcs) {
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
    
    public boolean supportsAlternative(PolicyAssertion assertion,
                                       List<QName> errors) {
        boolean pass = true;
        PolicyAssertion a = (PolicyAssertion)assertion;
        if (!a.isAsserted(this) && !a.isOptional()) {
            errors.add(a.getName());
            pass = false;
        }
        Policy p = a.getPolicy();
        if (p != null) {
            Iterator it = p.getAlternatives();
            while (it.hasNext()) {
                List<PolicyAssertion> lst = CastUtils.cast((List<?>)it.next());
                for (PolicyAssertion p2 : lst) {
                    pass &= supportsAlternative(p2, errors);
                }
            }
        }
        return pass || a.isOptional();
    }
    public boolean supportsAlternative(Collection<PolicyAssertion> alternative,
                                       List<QName> errors) {
        boolean pass = true;
        for (PolicyAssertion a : alternative) {
            pass &= supportsAlternative(a, errors);
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
        
        Set<String> msgs = new LinkedHashSet<String>();
        
        for (QName name : errors) {
            Collection<AssertionInfo> ais = getAssertionInfo(name);
            for (AssertionInfo ai : ais) {
                if (!ai.isAsserted()) {
                    String s = name.toString();
                    if (ai.getErrorMessage() != null) {
                        s += ": " + ai.getErrorMessage();
                    }
                    msgs.add(s);
                }
            }
        }
        StringBuilder error = new StringBuilder("\n");
        for (String msg : msgs) {
            error.append("\n").append(msg);
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
        Collection<PolicyAssertion> assertions = new ArrayList<PolicyAssertion>();
        getAssertions(p, assertions);
        return assertions;
    }
    
    private static void getAssertions(PolicyOperator p, Collection<PolicyAssertion> assertions) {
        List<PolicyComponent> pcs = 
            CastUtils.cast(p.getPolicyComponents(), PolicyComponent.class);
        for (PolicyComponent pc : pcs) {
            if (pc instanceof PolicyAssertion) {
                assertions.add((PolicyAssertion)pc);
            } else {
                getAssertions((PolicyOperator)pc, assertions);
            }
        }
    }
}
