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
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.neethi.Assertion;
import org.apache.neethi.Constants;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyOperator;
import org.apache.neethi.builders.xml.XMLPrimitiveAssertionBuilder;

/**
 * Implementation of an assertion that required exactly one (possibly empty) child element
 * of type Policy (as does for examples the wsam:Addressing assertion).
 * 
 */
public class NestedPrimitiveAssertion 
    extends org.apache.neethi.builders.PolicyContainingPrimitiveAssertion implements PolicyAssertion {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(NestedPrimitiveAssertion.class);
    
    
    private boolean assertionRequired = true;
    private PolicyBuilder builder;

    public NestedPrimitiveAssertion(QName name, 
                                    boolean optional,
                                    PolicyBuilder b) {
        this(name, optional, false, null, true, b);
    }
    
    public NestedPrimitiveAssertion(QName name, 
                                    boolean optional,
                                    boolean ignorable,
                                    PolicyBuilder b) {
        this(name, optional, ignorable, null, true, b);
    }
    
    public NestedPrimitiveAssertion(QName name, 
                                    boolean optional,
                                    Policy p, 
                                    boolean assertionRequired,
                                    PolicyBuilder b) {
        this(name, optional, false, p, assertionRequired, b);
    }
    
    public NestedPrimitiveAssertion(QName name, 
                                    boolean optional,
                                    boolean ignorable, 
                                    Policy p, 
                                    boolean assertionRequired,
                                    PolicyBuilder b) {
        super(name, optional, ignorable, p);
        this.assertionRequired = assertionRequired;
        builder = b;
    }

    public NestedPrimitiveAssertion(Element elem, PolicyBuilder builder) {
        this(elem, builder, true);
    }
    
    public NestedPrimitiveAssertion(Element elem, PolicyBuilder builder, boolean assertionRequired) {
        super(new QName(elem.getNamespaceURI(), elem.getLocalName()),
              XMLPrimitiveAssertionBuilder.isOptional(elem), 
              XMLPrimitiveAssertionBuilder.isIgnorable(elem), null);
        this.builder = builder;
        this.assertionRequired = assertionRequired;
        
        // expect exactly one child element of type Policy
        Element policyElem = null;
        for (Node nd = elem.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType()) {
                QName qn = new QName(nd.getNamespaceURI(), nd.getLocalName());
                if (Constants.isPolicyElement(qn)
                    && null == policyElem) {
                    policyElem = (Element)nd;
                } else {
                    throw new PolicyException(new Message("UNEXPECTED_CHILD_ELEMENT_EXC", BUNDLE, 
                                                          Constants.ELEM_POLICY));
                }                
            }
        }
        if (null == policyElem) {
            throw new PolicyException(new Message("UNEXPECTED_CHILD_ELEMENT_EXC", BUNDLE, 
                                                  Constants.ELEM_POLICY));
        }
        nested = builder.getPolicy(policyElem);  
    }
    protected Assertion clone(boolean opt, Policy n) {
        return new NestedPrimitiveAssertion(name, opt, ignorable, n, assertionRequired, builder);
    }

    public boolean equal(PolicyComponent policyComponent) {
        
        if (!super.equal(policyComponent)) {
            return false;
        }
        NestedPrimitiveAssertion other = (NestedPrimitiveAssertion)policyComponent;
        return getPolicy().equal(other.getPolicy());
    }
    
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
