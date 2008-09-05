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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.neethi.All;
import org.apache.neethi.Constants;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;

/**
 * 
 */
public class PrimitiveAssertion implements PolicyAssertion {
    
    protected QName name;
    protected boolean optional;
    
    public PrimitiveAssertion() {
        this((QName)null);
    }
    
    public PrimitiveAssertion(QName n) {
        this(n, false);
    }
    
    public PrimitiveAssertion(QName n, boolean o) {
        name = n;
        optional = o;
    }
    
    public PrimitiveAssertion(Element element) {
        name = new QName(element.getNamespaceURI(), element.getLocalName());
        NamedNodeMap atts = element.getAttributes();
        if (atts != null) {
            for (int x = 0; x < atts.getLength(); x++) {
                Attr att = (Attr)atts.item(x);
                QName qn = new QName(att.getNamespaceURI(), att.getLocalName());
                if (PolicyConstants.isOptionalAttribute(qn)) {
                    optional = Boolean.valueOf(att.getValue());                
                }
            }
        }
    }

    public String toString() {
        return name.toString();
    }
    public boolean equal(PolicyComponent policyComponent) {
        if (policyComponent.getType() != Constants.TYPE_ASSERTION) {
            return false;
        }
        return getName().equals(((PolicyAssertion)policyComponent).getName());
    }

    public short getType() {
        return Constants.TYPE_ASSERTION;
    }

    public QName getName() {
        return name;
    }
    
    public void setName(QName n) {
        name = n;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean o) {
        optional = o;        
    }
    
    public PolicyComponent normalize() {
        if (isOptional()) {
            Policy policy = new Policy();
            ExactlyOne exactlyOne = new ExactlyOne();

            All all = new All();
            all.addPolicyComponent(cloneMandatory());
            exactlyOne.addPolicyComponent(all);
            exactlyOne.addPolicyComponent(new All());
            policy.addPolicyComponent(exactlyOne);

            return policy;
        }

        return cloneMandatory();
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
    }
    
    protected PolicyAssertion cloneMandatory() {
        return new PrimitiveAssertion(name, false);
    }

    public Policy getPolicy() {
        return null;
    }

    public boolean isAsserted(AssertionInfoMap aim) {
        Collection<AssertionInfo> ail = aim.getAssertionInfo(name);
        for (AssertionInfo ai : ail) {
            if (ai.isAsserted() && ai.getAssertion().equal(this)) {
                return true;
            }
        }
        return false;
    }
}
