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
package org.apache.cxf.aegis.xml.jdom;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.xml.AbstractMessageWriter;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.jdom.Attribute;

public class AttributeWriter extends AbstractMessageWriter {
    private Attribute att;

    public AttributeWriter(Attribute att) {
        this.att = att;
    }

    public void writeValue(Object value) {
        att.setValue(value.toString());
    }

    public MessageWriter getAttributeWriter(String name) {
        throw new IllegalStateException();
    }

    public MessageWriter getAttributeWriter(String name, String namespace) {
        throw new IllegalStateException();
    }

    public MessageWriter getAttributeWriter(QName qname) {
        throw new IllegalStateException();
    }

    public MessageWriter getElementWriter(String name) {
        throw new IllegalStateException();
    }

    public MessageWriter getElementWriter(String name, String namespace) {
        throw new IllegalStateException();
    }

    public MessageWriter getElementWriter(QName qname) {
        throw new IllegalStateException();
    }

    public String getPrefixForNamespace(String namespace) {
        throw new IllegalStateException();
    }

    public String getPrefixForNamespace(String namespace, String hint) {
        throw new IllegalStateException();
    }

    public void close() {
    }
}
