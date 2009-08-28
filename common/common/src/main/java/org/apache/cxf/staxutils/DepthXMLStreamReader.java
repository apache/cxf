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

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class DepthXMLStreamReader implements XMLStreamReader {

    protected XMLStreamReader reader;
    private int depth;

    public DepthXMLStreamReader(XMLStreamReader r) {
        this.reader = r;
    }

    public XMLStreamReader getReader() {
        return this.reader;
    }

    public int getDepth() {
        return depth;
    }

    public void close() throws XMLStreamException {
        reader.close();
    }

    public int getAttributeCount() {
        return reader.getAttributeCount();
    }

    public String getAttributeLocalName(int arg0) {
        return reader.getAttributeLocalName(arg0);
    }

    public QName getAttributeName(int arg0) {
        return reader.getAttributeName(arg0);
    }

    public String getAttributeNamespace(int arg0) {
        return reader.getAttributeNamespace(arg0);
    }

    public String getAttributePrefix(int arg0) {
        return reader.getAttributePrefix(arg0);
    }

    public String getAttributeType(int arg0) {
        return reader.getAttributeType(arg0);
    }

    public String getAttributeValue(int arg0) {
        return reader.getAttributeValue(arg0);
    }

    public String getAttributeValue(String namespace, String localName) {
        return reader.getAttributeValue(namespace, localName);
    }

    public String getCharacterEncodingScheme() {
        return reader.getCharacterEncodingScheme();
    }

    public String getElementText() throws XMLStreamException {
        String ret = reader.getElementText();
        //workaround bugs in some readers that aren't properly advancing to 
        //the END_ELEMENT (*cough*jettison*cough*)
        while (reader.getEventType() != XMLStreamReader.END_ELEMENT) {
            reader.next();
        }
        depth--;
        return ret;
    }

    public String getEncoding() {
        return reader.getEncoding();
    }

    public int getEventType() {
        return reader.getEventType();
    }

    public String getLocalName() {
        return reader.getLocalName();
    }

    public Location getLocation() {
        return reader.getLocation();
    }

    public QName getName() {
        return reader.getName();
    }

    public NamespaceContext getNamespaceContext() {
        return reader.getNamespaceContext();
    }

    public int getNamespaceCount() {
        return reader.getNamespaceCount();
    }

    public String getNamespacePrefix(int arg0) {
        return reader.getNamespacePrefix(arg0);
    }

    public String getNamespaceURI() {
        return reader.getNamespaceURI();
    }

    public String getNamespaceURI(int arg0) {

        return reader.getNamespaceURI(arg0);
    }

    public String getNamespaceURI(String arg0) {
        return reader.getNamespaceURI(arg0);
    }

    public String getPIData() {
        return reader.getPIData();
    }

    public String getPITarget() {
        return reader.getPITarget();
    }

    public String getPrefix() {
        return reader.getPrefix();
    }

    public Object getProperty(String arg0) throws IllegalArgumentException {

        return reader.getProperty(arg0);
    }

    public String getText() {
        return reader.getText();
    }

    public char[] getTextCharacters() {
        return reader.getTextCharacters();
    }

    public int getTextCharacters(int arg0, char[] arg1, int arg2, int arg3) throws XMLStreamException {
        return reader.getTextCharacters(arg0, arg1, arg2, arg3);
    }

    public int getTextLength() {
        return reader.getTextLength();
    }

    public int getTextStart() {
        return reader.getTextStart();
    }

    public String getVersion() {
        return reader.getVersion();
    }

    public boolean hasName() {
        return reader.hasName();
    }

    public boolean hasNext() throws XMLStreamException {
        return reader.hasNext();
    }

    public boolean hasText() {
        return reader.hasText();
    }

    public boolean isAttributeSpecified(int arg0) {
        return reader.isAttributeSpecified(arg0);
    }

    public boolean isCharacters() {
        return reader.isCharacters();
    }

    public boolean isEndElement() {
        return reader.isEndElement();
    }

    public boolean isStandalone() {
        return reader.isStandalone();
    }

    public boolean isStartElement() {
        return reader.isStartElement();
    }

    public boolean isWhiteSpace() {
        return reader.isWhiteSpace();
    }

    public int next() throws XMLStreamException {
        int next = reader.next();
        
        if (next == START_ELEMENT) {
            depth++;
        } else if (next == END_ELEMENT) {
            depth--;
        }
        
        return next;
    }

    public int nextTag() throws XMLStreamException {
        int eventType = next();
        while ((eventType == XMLStreamConstants.CHARACTERS && isWhiteSpace())
                || (eventType == XMLStreamConstants.CDATA && isWhiteSpace())
                // skip whitespace
                || eventType == XMLStreamConstants.SPACE
                || eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
                || eventType == XMLStreamConstants.COMMENT) {
            eventType = next();
        }
        if (eventType != XMLStreamConstants.START_ELEMENT && eventType != XMLStreamConstants.END_ELEMENT) {
            throw new XMLStreamException("expected start or end tag", getLocation());
        }
        return eventType;
    }

    public void require(int arg0, String arg1, String arg2) throws XMLStreamException {
        reader.require(arg0, arg1, arg2);
    }

    public boolean standaloneSet() {
        return reader.standaloneSet();
    }

    public int hashCode() {
        return reader.hashCode();
    }

    public boolean equals(Object arg0) {
        return reader.equals(arg0);
    }

    public String toString() {
        return reader.toString();
    }
}
