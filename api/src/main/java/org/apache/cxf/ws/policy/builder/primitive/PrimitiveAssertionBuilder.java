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

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.ws.policy.AssertionBuilder;
import org.apache.cxf.ws.policy.PolicyAssertion;

public class PrimitiveAssertionBuilder implements AssertionBuilder {

    protected Bus bus;
    private Collection<QName> knownElements = new ArrayList<QName>();
    
    public PrimitiveAssertionBuilder() {
        knownElements = new ArrayList<QName>();
    }
    public PrimitiveAssertionBuilder(Collection<QName> els) {
        knownElements = els;
    }
    
    public void setBus(Bus b) {
        bus = b;
    }
    
    public PolicyAssertion build(Element element) {  
        return new PrimitiveAssertion(element);
    }

    public Collection<QName> getKnownElements() {
        return knownElements;
    }
    
    public void setKnownElements(Collection<QName> k) {
        knownElements = k;
    }

    /**
     * If the two assertions are equal, they are also compatible. 
     * The compatible policy is optional iff both assertions are optional.
     */
    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        if (knownElements.contains(a.getName()) && a.getName().equals(b.getName())) {
            return new PrimitiveAssertion(a.getName(), a.isOptional() && b.isOptional());
        }
        return  null;
    }   

}
