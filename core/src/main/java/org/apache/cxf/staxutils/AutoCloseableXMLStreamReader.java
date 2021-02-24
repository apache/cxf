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

package org.apache.cxf.staxutils;

import java.io.Closeable;
import java.io.IOException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class AutoCloseableXMLStreamReader implements XMLStreamReader, AutoCloseable {
    private final XMLStreamReader delegate;
    private final Closeable source;
    
    AutoCloseableXMLStreamReader(XMLStreamReader delegate, Closeable source) {
        this.delegate = delegate;
        this.source = source;
    }

    @Override
    public void close() throws XMLStreamException {
        if (delegate != null) {
            delegate.close();
        }
        try {
            if (source != null) {
                source.close();
            }
        } catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return delegate.getProperty(name);
    }

    @Override
    public int next() throws XMLStreamException {
        return delegate.next();
    }

    @Override
    public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
        delegate.require(type, namespaceURI, localName);
    }

    @Override
    public String getElementText() throws XMLStreamException {
        return delegate.getElementText();
    }

    @Override
    public int nextTag() throws XMLStreamException {
        return delegate.nextTag();
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        return delegate.hasNext();
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return delegate.getNamespaceURI(prefix);
    }

    @Override
    public boolean isStartElement() {
        return delegate.isStartElement();
    }

    @Override
    public boolean isEndElement() {
        return delegate.isEndElement();
    }

    @Override
    public boolean isCharacters() {
        return delegate.isCharacters();
    }

    @Override
    public boolean isWhiteSpace() {
        return delegate.isWhiteSpace();
    }

    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        return delegate.getAttributeValue(namespaceURI, localName);
    }

    @Override
    public int getAttributeCount() {
        return delegate.getAttributeCount();
    }

    @Override
    public QName getAttributeName(int index) {
        return delegate.getAttributeName(index);
    }

    @Override
    public String getAttributeNamespace(int index) {
        return delegate.getAttributeNamespace(index);
    }

    @Override
    public String getAttributeLocalName(int index) {
        return delegate.getAttributeLocalName(index);
    }

    @Override
    public String getAttributePrefix(int index) {
        return delegate.getAttributePrefix(index);
    }

    @Override
    public String getAttributeType(int index) {
        return delegate.getAttributeType(index);
    }

    @Override
    public String getAttributeValue(int index) {
        return delegate.getAttributeValue(index);
    }

    @Override
    public boolean isAttributeSpecified(int index) {
        return delegate.isAttributeSpecified(index);
    }

    @Override
    public int getNamespaceCount() {
        return delegate.getNamespaceCount();
    }

    @Override
    public String getNamespacePrefix(int index) {
        return delegate.getNamespacePrefix(index);
    }

    @Override
    public String getNamespaceURI(int index) {
        return delegate.getNamespaceURI(index);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return delegate.getNamespaceContext();
    }

    @Override
    public int getEventType() {
        return delegate.getEventType();
    }

    @Override
    public String getText() {
        return delegate.getText();
    }

    @Override
    public char[] getTextCharacters() {
        return delegate.getTextCharacters();
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) 
            throws XMLStreamException {
        return delegate.getTextCharacters(sourceStart, target, targetStart, length);
    }

    @Override
    public int getTextStart() {
        return delegate.getTextStart();
    }

    @Override
    public int getTextLength() {
        return delegate.getTextLength();
    }

    @Override
    public String getEncoding() {
        return delegate.getEncoding();
    }

    @Override
    public boolean hasText() {
        return delegate.hasText();
    }

    @Override
    public Location getLocation() {
        return delegate.getLocation();
    }

    @Override
    public QName getName() {
        return delegate.getName();
    }

    @Override
    public String getLocalName() {
        return delegate.getLocalName();
    }

    @Override
    public boolean hasName() {
        return delegate.hasName();
    }

    @Override
    public String getNamespaceURI() {
        return delegate.getNamespaceURI();
    }

    @Override
    public String getPrefix() {
        return delegate.getPrefix();
    }

    @Override
    public String getVersion() {
        return delegate.getVersion();
    }

    @Override
    public boolean isStandalone() {
        return delegate.isStandalone();
    }

    @Override
    public boolean standaloneSet() {
        return delegate.standaloneSet();
    }

    @Override
    public String getCharacterEncodingScheme() {
        return delegate.getCharacterEncodingScheme();
    }

    @Override
    public String getPITarget() {
        return delegate.getPITarget();
    }

    @Override
    public String getPIData() {
        return delegate.getPIData();
    }
}