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

import org.w3c.dom.Element;

import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.builders.AssertionBuilder;

public class PrimitiveAssertionBuilder implements AssertionBuilder<Element> {

    private QName knownElements[] = {};
    
    public PrimitiveAssertionBuilder() {
    }
    public PrimitiveAssertionBuilder(Collection<QName> els) {
        knownElements = els.toArray(new QName[els.size()]);
    }
    public PrimitiveAssertionBuilder(QName els[]) {
        knownElements = els;
    }
    
    
    public Assertion build(Element element, AssertionBuilderFactory fact) { 
        return new PrimitiveAssertion(element);
    }
    
    public QName[] getKnownElements() {
        return knownElements;
    }
    
    public void setKnownElements(Collection<QName> k) {
        knownElements = k.toArray(new QName[k.size()]);
    }
    public void setKnownElements(QName k[]) {
        knownElements = k;
    }

}
