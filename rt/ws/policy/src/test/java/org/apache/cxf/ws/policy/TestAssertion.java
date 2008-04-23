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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.neethi.Constants;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;

/**
 * 
 */
public class TestAssertion implements PolicyAssertion {
    
    private QName name;
    private boolean optional;
    
    public TestAssertion() {
        this(null);
    }
    
    public TestAssertion(QName n) {
        name = n;
    }
    
    public QName getName() {
        return name;
    }

    public boolean isOptional() {
        return optional;
    }

    public PolicyComponent normalize() {
        return this;
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
    }

    public boolean equal(PolicyComponent policyComponent) {
        return this == policyComponent;
    }

    public short getType() {
        return Constants.TYPE_ASSERTION;
    }

    public Policy getPolicy() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isAsserted(AssertionInfoMap aim) {
        // TODO Auto-generated method stub
        return false;
    }
}
