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
package org.apache.cxf.binding.corba.runtime;

import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;

import org.apache.cxf.binding.corba.types.CorbaTypeEventProducer;

public class CorbaStreamReader implements XMLStreamReader {
     
    private CorbaTypeEventProducer eventProducer;
    private int currentState;
    
    public CorbaStreamReader(CorbaTypeEventProducer evProducer) {
        eventProducer = evProducer;
        currentState = XMLStreamReader.START_DOCUMENT;
    }

    public QName getName() {
        return eventProducer.getName();
    }

    public char[] getTextCharacters() {
        return eventProducer.getText().toCharArray();
    }

    public int getEventType() {
        return currentState;
    }

    public String getLocalName() {
        return getName().getLocalPart();
    }

    public String getNamespaceURI() {
        return getName().getNamespaceURI();
    }

    public boolean hasNext() throws XMLStreamException {
        if (currentState == XMLStreamReader.START_DOCUMENT) {
            return true;
        }
        boolean hasNextEvent = eventProducer.hasNext();
        if (!hasNextEvent && currentState != XMLStreamReader.END_DOCUMENT) {
            currentState = XMLStreamReader.END_DOCUMENT;
            hasNextEvent = true;
        }
        return hasNextEvent;
    }

    public int next() throws XMLStreamException {
        if (currentState == XMLStreamReader.START_DOCUMENT) {
            currentState = 0;
            return XMLStreamReader.START_DOCUMENT;
        }
        // ensure we catch end_document state
        hasNext();
        if (currentState != XMLStreamReader.END_DOCUMENT) {
            currentState = eventProducer.next();
        }
        return currentState;
    }

    public int getTextLength() {
        return eventProducer.getText().length();
    }

    public boolean isStartElement() {
        return currentState == XMLStreamConstants.START_ELEMENT;
    }

    public boolean isCharacters() {
        return currentState == XMLStreamConstants.CHARACTERS;
    }

    public boolean isEndElement() {
        return currentState == XMLStreamConstants.END_ELEMENT;
    }

    public void close() throws XMLStreamException {
        // TODO Auto-generated method stub
    }

    public int getAttributeCount() {
        List<Attribute> currentAttributes =  eventProducer.getAttributes();
        if (currentAttributes != null) {
            return currentAttributes.size();
        }
        return 0;
    }

    public String getAttributeLocalName(int arg0) {
        String ret = null;
        List<Attribute> currentAttributes = eventProducer.getAttributes();
        if (currentAttributes != null) {
            Attribute a = currentAttributes.get(arg0);
            if (a != null) {
                ret = a.getName().getLocalPart();
            }
        }
        return ret;
    }

    public QName getAttributeName(int arg0) {
        QName ret = null;
        List<Attribute> currentAttributes = eventProducer.getAttributes();
        if (currentAttributes != null) {
            Attribute a = currentAttributes.get(arg0);
            if (a != null) {
                ret = a.getName();
            }
        }
        return ret;
    }

 
    public String getAttributeNamespace(int arg0) {
        String ret = null;
        List<Attribute> currentAttributes = eventProducer.getAttributes();
        if (currentAttributes != null) {
            Attribute a = currentAttributes.get(arg0);
            if (a != null) {
                ret = a.getName().getNamespaceURI();
            }
        }
        return ret;
    }

    public String getAttributePrefix(int arg0) {
        return null;
    }

    public String getAttributeType(int arg0) {
        return "CDATA";
    }

    public String getAttributeValue(int arg0) {
        String ret = null;
        List<Attribute> currentAttributes = eventProducer.getAttributes();
        if (currentAttributes != null) {
            Attribute a = currentAttributes.get(arg0);
            if (a != null) {
                ret = a.getValue();
            }
        }
        return ret;
    }

    public String getAttributeValue(String arg0, String arg1) {
        return null;
    }

    public String getCharacterEncodingScheme() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getElementText() throws XMLStreamException {
        return null;
    }

    public String getEncoding() {
        // TODO Auto-generated method stub
        return null;
    }

    public Location getLocation() {
        // TODO Auto-generated method stub
        return new Location() {

            public int getCharacterOffset() {
                // TODO Auto-generated method stub
                return -1;
            }

            public int getColumnNumber() {
                // TODO Auto-generated method stub
                return -1;
            }

            public int getLineNumber() {
                // TODO Auto-generated method stub
                return -1;
            }

            public String getPublicId() {
                // TODO Auto-generated method stub
                return null;
            }

            public String getSystemId() {
                // TODO Auto-generated method stub
                return null;
            }
            
        };
    }

    public NamespaceContext getNamespaceContext() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getNamespaceCount() {
        List<Namespace> namespaces = eventProducer.getNamespaces();
        if (namespaces != null) {
            return namespaces.size();
        }
        return 0;
    }

    public String getNamespacePrefix(int arg0) {
        List<Namespace> namespaces = eventProducer.getNamespaces();
        if (namespaces == null) {
            return null;
        }

        Namespace ns = namespaces.get(arg0);
        if (ns != null) {
            return ns.getPrefix();
        }
        return null;
    }

    public String getNamespaceURI(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getNamespaceURI(int arg0) {
        List<Namespace> namespaces = eventProducer.getNamespaces();
        if (namespaces == null) {
            return null;
        }

        Namespace ns = namespaces.get(arg0);
        if (ns != null) {
            return ns.getNamespaceURI();
        }
        return null;
    }

    public String getPIData() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getPITarget() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getPrefix() {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getProperty(String arg0) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getText() {
        return eventProducer.getText();
    }

    public int getTextCharacters(int arg0, char[] arg1, int arg2, int arg3) throws XMLStreamException {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public int getTextStart() {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean hasName() {
        throw new RuntimeException("Not Implemented");
    }

    public boolean hasText() {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public boolean isAttributeSpecified(int arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isStandalone() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isWhiteSpace() {
        // TODO Auto-generated method stub
        return false;
    }

    public int nextTag() throws XMLStreamException {
        throw new RuntimeException("Not Implemented");
    }

    public void require(int arg0, String arg1, String arg2) throws XMLStreamException {
        // TODO Auto-generated method stub        
    }

    public boolean standaloneSet() {
        // TODO Auto-generated method stub
        return false;
    }

}
