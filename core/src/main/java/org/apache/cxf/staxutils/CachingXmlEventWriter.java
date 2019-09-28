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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

import org.apache.cxf.common.util.StringUtils;


/**
 *
 */
public class CachingXmlEventWriter implements XMLStreamWriter {
    protected XMLEventFactory factory;

    List<XMLEvent> events = new ArrayList<>(1000);
    final Deque<NSContext> contexts = new ArrayDeque<>();
    final Deque<QName> elNames = new ArrayDeque<>();
    QName lastStart = new QName(""); // avoid push null to Deque
    NSContext curContext = new NSContext(null);

    public CachingXmlEventWriter() {
        factory = XMLEventFactory.newInstance();
    }

    protected void addEvent(XMLEvent event) {
        events.add(event);
    }

    public List<XMLEvent> getEvents() {
        return events;
    }

    public void close() throws XMLStreamException {
        //nothing
    }

    public void flush() throws XMLStreamException {
        //nothing
    }

    public NamespaceContext getNamespaceContext() {
        return curContext;
    }

    public String getPrefix(String ns) throws XMLStreamException {
        return curContext.getPrefix(ns);
    }

    public Object getProperty(String name) {
        //nothing
        return null;
    }


    public void setNamespaceContext(NamespaceContext arg0) throws XMLStreamException {
        curContext = new NSContext(arg0);
    }

    public void writeAttribute(String name, String value) throws XMLStreamException {
        addEvent(factory.createAttribute(name, value));
    }

    public void writeAttribute(String pfx, String uri, String name, String value) throws XMLStreamException {
        if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(uri)) {
            if (StringUtils.isEmpty(name)) {
                writeDefaultNamespace(value);
            } else {
                writeNamespace(name, value);
            }
        } else {
            addEvent(factory.createAttribute(pfx, uri, name, value));
        }
    }

    public void writeCData(String arg0) throws XMLStreamException {
        addEvent(factory.createCData(arg0));
    }

    public void writeCharacters(String arg0) throws XMLStreamException {
        addEvent(factory.createCharacters(arg0));
    }

    public void writeCharacters(char[] arg0, int arg1, int arg2) throws XMLStreamException {
        addEvent(factory.createCharacters(new String(arg0, arg1, arg2)));
    }

    public void writeComment(String arg0) throws XMLStreamException {
        addEvent(factory.createComment(arg0));
    }

    public void writeDTD(String arg0) throws XMLStreamException {
        addEvent(factory.createDTD(arg0));
    }

    public void writeEndDocument() throws XMLStreamException {
        addEvent(factory.createEndDocument());
    }


    public void writeEntityRef(String arg0) throws XMLStreamException {
        addEvent(factory.createEntityReference(arg0, null));
    }


    public void writeProcessingInstruction(String arg0) throws XMLStreamException {
        addEvent(factory.createProcessingInstruction(arg0, null));
    }

    public void writeProcessingInstruction(String arg0, String arg1) throws XMLStreamException {
        addEvent(factory.createProcessingInstruction(arg0, arg1));
    }

    public void writeStartDocument() throws XMLStreamException {
        addEvent(factory.createStartDocument());
    }

    public void writeStartDocument(String version) throws XMLStreamException {
        addEvent(factory.createStartDocument(null, version));
    }

    public void writeStartDocument(String arg0, String arg1) throws XMLStreamException {
        addEvent(factory.createStartDocument(arg0, arg1));
    }

    public void setDefaultNamespace(String ns) throws XMLStreamException {
        curContext.addNs("", ns);
    }


    public void writeNamespace(String pfx, String ns) throws XMLStreamException {
        curContext.addNs(pfx, ns);
        if (StringUtils.isEmpty(pfx)) {
            addEvent(factory.createNamespace(ns));
        } else {
            addEvent(factory.createNamespace(pfx, ns));
        }
    }

    public void writeAttribute(String uri, String name, String value) throws XMLStreamException {
        if (!StringUtils.isEmpty(uri)) {
            String pfx = StaxUtils.getUniquePrefix(this, uri, false);
            addEvent(factory.createAttribute(pfx, uri, name, value));
        } else {
            addEvent(factory.createAttribute(name, value));
        }
    }
    public void setPrefix(String pfx, String uri) throws XMLStreamException {
        curContext.addNs(pfx, uri);
    }


    public void writeEndElement() throws XMLStreamException {
        addEvent(factory.createEndElement(lastStart,
                                          Collections.<javax.xml.stream.events.Namespace>emptyList().iterator()));
        curContext = contexts.pop();
        lastStart = elNames.pop();
    }


    public void writeDefaultNamespace(String ns) throws XMLStreamException {
        writeNamespace("", ns);
    }

    public void writeEmptyElement(String name) throws XMLStreamException {
        writeStartElement(name);
        writeEndElement();
    }
    public void writeEmptyElement(String name, String ns) throws XMLStreamException {
        writeStartElement(name, ns);
        writeEndElement();
    }
    public void writeEmptyElement(String pfx, String name, String ns) throws XMLStreamException {
        writeStartElement(pfx, name, ns);
        writeEndElement();
    }

    public void writeStartElement(String name) throws XMLStreamException {
        elNames.push(lastStart);
        contexts.push(curContext);
        curContext = new NSContext(curContext);
        lastStart = new QName(name);
        addEvent(factory.createStartElement(lastStart,
                                            Collections.EMPTY_SET.iterator(),
                                            Collections.EMPTY_SET.iterator()));
    }
    public void writeStartElement(String name, String ns) throws XMLStreamException {
        elNames.push(lastStart);
        contexts.push(curContext);
        curContext = new NSContext(curContext);
        lastStart = new QName(ns, name);
        addEvent(factory.createStartElement(lastStart,
                                            Collections.EMPTY_SET.iterator(),
                                            Collections.EMPTY_SET.iterator()));
    }
    public void writeStartElement(String pfx, String name, String ns) throws XMLStreamException {
        elNames.push(lastStart);
        contexts.push(curContext);
        curContext = new NSContext(curContext);
        lastStart = new QName(ns, name, pfx);
        addEvent(factory.createStartElement(lastStart,
                                            Collections.EMPTY_SET.iterator(),
                                            Collections.EMPTY_SET.iterator()));
    }

    public static class NSContext implements NamespaceContext {
        NamespaceContext parent;
        Map<String, String> map = new HashMap<>();

        public NSContext(NamespaceContext p) {
            parent = p;
        }
        public void addNs(String pfx, String ns) {
            map.put(pfx, ns);
        }

        public String getNamespaceURI(String prefix) {
            String ret = map.get(prefix);
            if (ret == null && parent != null) {
                return parent.getNamespaceURI(prefix);
            }
            return ret;
        }

        public String getPrefix(String namespaceURI) {
            for (Map.Entry<String, String> e : map.entrySet()) {
                if (e.getValue().equals(namespaceURI)) {
                    return e.getKey();
                }
            }
            if (parent != null) {
                return parent.getPrefix(namespaceURI);
            }
            return null;
        }

        public Iterator<String> getPrefixes(String namespaceURI) {
            List<String> l = new ArrayList<>();
            for (Map.Entry<String, String> e : map.entrySet()) {
                if (e.getValue().equals(namespaceURI)) {
                    l.add(e.getKey());
                }
            }
            if (l.isEmpty()) {
                String pfx = getPrefix(namespaceURI);
                if (pfx == null) {
                    l = Collections.emptyList();
                    return l.iterator();
                }
                return Collections.singleton(pfx).iterator();
            }
            return l.iterator();
        }

    }
}
