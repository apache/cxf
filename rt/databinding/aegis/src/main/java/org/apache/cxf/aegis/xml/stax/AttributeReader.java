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
package org.apache.cxf.aegis.xml.stax;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.xml.AbstractMessageReader;
import org.apache.cxf.aegis.xml.MessageReader;

public class AttributeReader extends AbstractMessageReader {
    private QName name;
    private String value;

    public AttributeReader(QName name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean hasMoreAttributeReaders() {
        return false;
    }

    public MessageReader getNextAttributeReader() {
        throw new IllegalStateException();
    }

    public MessageReader getAttributeReader(QName qName) {
        throw new IllegalStateException();
    }

    public MessageReader getAttributeReader(String n, String namespace) {
        throw new IllegalStateException();
    }

    public boolean hasMoreElementReaders() {
        return false;
    }

    public MessageReader getNextElementReader() {
        throw new IllegalStateException();
    }

    public QName getName() {
        return name;
    }

    public String getLocalName() {
        return name.getLocalPart();
    }

    public String getNamespace() {
        return name.getNamespaceURI();
    }

    public String getNamespaceForPrefix(String prefix) {
        throw new IllegalStateException();
    }
}
