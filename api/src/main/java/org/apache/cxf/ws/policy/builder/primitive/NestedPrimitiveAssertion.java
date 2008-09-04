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


import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.neethi.All;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyOperator;

/**
 * Implementation of an assertion that required exactly one (possibly empty) child element
 * of type Policy (as does for examples the wsam:Addressing assertion).
 * 
 */
public class NestedPrimitiveAssertion extends PrimitiveAssertion {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(NestedPrimitiveAssertion.class);    
    private Policy nested;
    private boolean assertionRequired = true;
    public NestedPrimitiveAssertion(QName name, boolean optional) {
        this(name, optional, null, true);
    }
    
    public NestedPrimitiveAssertion(QName name, boolean optional, 
                                    Policy p, boolean assertionRequired) {
        super(name, optional);
        this.assertionRequired = assertionRequired;
        this.nested = p;
    }

    public NestedPrimitiveAssertion(Element elem, PolicyBuilder builder) {
        this(elem, builder, true);
    }
    
    public NestedPrimitiveAssertion(Element elem, PolicyBuilder builder, boolean assertionRequired) {
        super(elem);
        this.assertionRequired = assertionRequired;
        
        // expect exactly one child element of type Policy
       
        Element policyElem = null;
        for (Node nd = elem.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType()) {
                QName qn = new QName(nd.getNamespaceURI(), nd.getLocalName());
                if (PolicyConstants.isPolicyElem(qn)
                    && null == policyElem) {
                    policyElem = (Element)nd;
                } else {
                    throw new PolicyException(new Message("UNEXPECTED_CHILD_ELEMENT_EXC", BUNDLE, 
                                                          PolicyConstants.POLICY_ELEM_NAME));
                }                
            }
        }
        if (null == policyElem) {
            throw new PolicyException(new Message("UNEXPECTED_CHILD_ELEMENT_EXC", BUNDLE, 
                                                  PolicyConstants.POLICY_ELEM_NAME));
        }
        
        nested = builder.getPolicy(policyElem);  
    }
    
    public PolicyComponent normalize() {
        Policy normalisedNested = (Policy)nested.normalize(true);
        
        Policy p = new Policy();
        ExactlyOne ea = new ExactlyOne();
        p.addPolicyComponent(ea);
        if (isOptional()) {
            ea.addPolicyComponent(new All());
        }
        // for all alternatives in normalised nested policy
        Iterator alternatives = normalisedNested.getAlternatives();
        while (alternatives.hasNext()) {
            All all = new All();
            List<PolicyAssertion> alternative = 
                CastUtils.cast((List)alternatives.next(), PolicyAssertion.class);
            NestedPrimitiveAssertion a = new NestedPrimitiveAssertion(getName(), false);
            a.nested = new Policy();
            ExactlyOne nea = new ExactlyOne();
            a.nested.addPolicyComponent(nea);
            All na = new All();
            nea.addPolicyComponent(na);
            na.addPolicyComponents(alternative);
            all.addPolicyComponent(a);
            ea.addPolicyComponent(all);            
        } 
        return p;      
    } 
    
    @Override
    public boolean equal(PolicyComponent policyComponent) {
        
        if (!super.equal(policyComponent)) {
            return false;
        }
        NestedPrimitiveAssertion other = (NestedPrimitiveAssertion)policyComponent;
        return getPolicy().equal(other.getPolicy());
    }
    
    protected void setPolicy(Policy n) {
        nested = n;
    }
    
    @Override
    public Policy getPolicy() {
        return nested;
    }

    @Override
    public boolean isAsserted(AssertionInfoMap aim) {
        
        if (assertionRequired) {
            Collection<AssertionInfo> ail = aim.getAssertionInfo(name);
            for (AssertionInfo ai : ail) {
                if (ai.isAsserted() && isPolicyAsserted(nested, aim)) {
                    return true;
                }
            }
            return false;
        }
        
        return isPolicyAsserted(nested, aim);
    }
    
    protected boolean isPolicyAsserted(PolicyOperator p, AssertionInfoMap aim) {
        if (p == null) {
            return true;
        }
        List<PolicyComponent> pcs = 
            CastUtils.cast(p.getPolicyComponents(), PolicyComponent.class);
        if (pcs.size() == 0) {
            return true;
        }
        
        if (pcs.get(0) instanceof PolicyAssertion) {
            List<PolicyAssertion> assertions = 
                CastUtils.cast(pcs, PolicyAssertion.class);
            for (PolicyAssertion pa : assertions) {
                if (!pa.isAsserted(aim)) {
                    return false;
                }
            }
            return true;
        } else {
            List<PolicyOperator> assertions = 
                CastUtils.cast(pcs, PolicyOperator.class);
            for (PolicyOperator po : assertions) {
                if (isPolicyAsserted(po, aim)) {
                    return true;
                }
            }
            return false;
        }
    }
}
