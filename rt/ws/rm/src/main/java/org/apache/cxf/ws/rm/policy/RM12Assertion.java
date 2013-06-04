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

package org.apache.cxf.ws.rm.policy;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.neethi.Assertion;
import org.apache.neethi.PolicyComponent;

/**
 * Representation of WS-RMP 1.2 assertion data.
 */
public class RM12Assertion implements Assertion {
    public static final String POLICY_NS = "http://www.w3.org/ns/ws-policy";
    public static final String POLICY_LOCAL = "Policy";
    public static final String POLICY_PREFIX = "wsp";
    
    public static final String SEQUENCESTR_NAME = "SequenceSTR";
    public static final String SEQUENCETRANSEC_NAME = "SequenceTransportSecurity";
    public static final String DELIVERYASSURANCE_NAME = "DeliveryAssurance";
    public static final String INORDER_NAME = "InOrder";
    
    enum Order {
        ExactlyOnce, AtLeastOnce, AtMostOnce
    };

    private boolean isOptional;
    private boolean ignorable;
    private boolean normalized;
    private boolean isSequenceSTR;
    private boolean isSequenceTransportSecurity;
    private Order order;
    private boolean inOrder;

    public QName getName() {
        return RM11Constants.WSRMP_RMASSERTION_QNAME;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public void setOptional(boolean optional) {
        this.isOptional = optional;
    }
    
    public boolean isIgnorable() {
        return ignorable;
    }

    public void setIgnorable(boolean ignorable) {
        this.ignorable = ignorable;
    }

    public boolean isSequenceSTR() {
        return isSequenceSTR;
    }

    public void setSequenceSTR(boolean str) {
        isSequenceSTR = str;
    }

    public boolean isSequenceTransportSecurity() {
        return isSequenceTransportSecurity;
    }

    public void setSequenceTransportSecurity(boolean sts) {
        isSequenceTransportSecurity = sts;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public boolean isInOrder() {
        return inOrder;
    }

    public void setInOrder(boolean inOrder) {
        this.inOrder = inOrder;
    }

    public short getType() {
        return org.apache.neethi.Constants.TYPE_ASSERTION;
    }

    public boolean equal(PolicyComponent policyComponent) {
        return policyComponent == this;
    }

    public void setNormalized(boolean normalized) {
        this.normalized = normalized;
    }

    public boolean isNormalized() {
        return normalized;
    }
    
    public boolean isAssuranceSet() {
        return order != null || inOrder;
    }

    public PolicyComponent normalize() {
        return this;
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {

        String localName = RM11Constants.RMASSERTION_NAME;
        String namespaceURI = RM11Constants.WSRMP_NAMESPACE_URI;
        String prefix = writer.getPrefix(namespaceURI);
        if (prefix == null) {
            prefix = "rmp12";
            writer.setPrefix(prefix, namespaceURI);
        }

        // <rmp12:RMAssertion>
        writer.writeStartElement(prefix, localName, namespaceURI);
        String wspPrefix = writer.getPrefix(POLICY_NS);
        if (wspPrefix == null) {
            wspPrefix = POLICY_PREFIX;
            writer.setPrefix(wspPrefix, POLICY_NS);
        }

        // <wsp:Policy>
        writer.writeStartElement(wspPrefix, POLICY_LOCAL, POLICY_NS);
        
        // <rmp12:SequenceSTR>
        if (isSequenceSTR) {
            writer.writeEmptyElement(prefix, SEQUENCESTR_NAME, namespaceURI);
        }
        
        // <rmp12:SequenceTransportSecurity>
        if (isSequenceTransportSecurity) {
            writer.writeEmptyElement(prefix, SEQUENCETRANSEC_NAME, namespaceURI);
        }
        
        // <rmp12:DeliveryAssurance> <wsp:Policy> ... </wsp:Policy> </rmp12:DeliveryAssurance>
        if (isAssuranceSet()) {
            writer.writeStartElement(prefix, DELIVERYASSURANCE_NAME, namespaceURI);
            writer.writeStartElement(wspPrefix, POLICY_LOCAL, POLICY_NS);
            if (order != null) {
                writer.writeEmptyElement(prefix, order.name(), namespaceURI);
            }
            if (inOrder) {
                writer.writeEmptyElement(prefix, INORDER_NAME, namespaceURI);
            }
            writer.writeEndElement();
            writer.writeEndElement();
        }

        // </wsp:Policy>
        writer.writeEndElement();

        // </rmp12:RMAssertion>
        writer.writeEndElement();
    }
}