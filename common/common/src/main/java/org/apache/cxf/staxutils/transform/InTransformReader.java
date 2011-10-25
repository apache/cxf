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
package org.apache.cxf.staxutils.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.staxutils.DepthXMLStreamReader;

public class InTransformReader extends DepthXMLStreamReader {
    private static final Logger LOG = LogUtils.getLogger(InTransformReader.class);

    private static final String INTERN_NAMES = "org.codehaus.stax2.internNames";
    private static final String INTERN_NS = "org.codehaus.stax2.internNsUris";
    
    private QNamesMap inElementsMap;
    private QNamesMap inAttributesMap;
    private Map<QName, ElementProperty> inAppendMap = new HashMap<QName, ElementProperty>(5);
    private Set<QName> inDropSet = new HashSet<QName>(5);
    private Map<String, String> nsMap = new HashMap<String, String>(5);
    private Stack<ParsingEvent> pushedBackEvents = new Stack<ParsingEvent>();
    private Map<EndEventMarker, List<ParsingEvent>> pushedAheadEvents = 
        new HashMap<EndEventMarker, List<ParsingEvent>>();
    private String replaceText;
    private ParsingEvent currentEvent;
    private List<Integer> attributesIndexes = new ArrayList<Integer>(); 
    private boolean blockOriginalReader = true;
    private boolean attributesIndexed;
    private DelegatingNamespaceContext namespaceContext;

    public InTransformReader(XMLStreamReader reader, 
                             Map<String, String> inMap,
                             Map<String, String> appendMap,
                             boolean blockOriginalReader) {
        
        this(reader, inMap, appendMap, null, null, blockOriginalReader);
    }
    
    public InTransformReader(XMLStreamReader reader, 
                             Map<String, String> inEMap,
                             Map<String, String> appendMap,
                             List<String> dropESet,
                             Map<String, String> inAMap,
                             boolean blockOriginalReader) {
        super(reader);
        inElementsMap = new QNamesMap(inEMap == null ? 0 : inEMap.size());
        inAttributesMap = new QNamesMap(inAMap == null ? 0 : inAMap.size());
        this.blockOriginalReader = blockOriginalReader;
        TransformUtils.convertToQNamesMap(inEMap, inElementsMap, nsMap);
        TransformUtils.convertToQNamesMap(inAMap, inAttributesMap, null);
        
        TransformUtils.convertToMapOfElementProperties(appendMap, inAppendMap);
        TransformUtils.convertToSetOfQNames(dropESet, inDropSet);
        namespaceContext = new DelegatingNamespaceContext(
            reader.getNamespaceContext(), nsMap);
    }
    
    @Override
    // If JAXB schema validation is disabled then returning 
    // the native reader and thus bypassing this reader may work
    public XMLStreamReader getReader() {
        return blockOriginalReader ? this : super.getReader();
    }
    
    public int next() throws XMLStreamException {
        final boolean doDebug = LOG.isLoggable(Level.FINE);

        if (!pushedBackEvents.empty()) {
            // consume events from the pushed back stack
            currentEvent = pushedBackEvents.pop();
            if (doDebug) {
                LOG.fine("pushed event available: " + currentEvent);
            }
            return currentEvent.event;
        } else {
            if (doDebug) {
                LOG.fine("no pushed event");
            }
        }
        
        int event = super.next();
        if (event == XMLStreamConstants.START_ELEMENT) {
            attributesIndexed = false;
            final QName theName = super.getName();
            final ElementProperty appendProp = inAppendMap.remove(theName);
            final boolean replaceContent = appendProp != null && theName.equals(appendProp.getName());
            if (doDebug) {
                LOG.fine("read StartElement " + theName + " at " + getDepth());
            }
            
            final boolean dropped = inDropSet.contains(theName);
            QName expected = inElementsMap.get(theName);
            if (expected == null) {
                expected = theName;
            }

            if (null != appendProp && !replaceContent) {
                // handle one of the four append modes
                handleAppendMode(theName, expected, appendProp);
            } else if (replaceContent) {
                replaceText = appendProp.getText();
                if (doDebug) {
                    LOG.fine("replacing content with " + replaceText);    
                }
                
                currentEvent = createStartElementEvent(expected);
            } else if (dropped) {
                if (doDebug) {
                    LOG.fine("shallow-dropping start " + expected);
                }
                // unwrap the current element (shallow drop)
                event = next();
            } else if (isEmptyQName(expected)) {
                // skip the current element (deep drop)
                if (doDebug) {
                    LOG.fine("deep-dropping " + theName);
                }
                handleDeepDrop();
                event = next();
            } else {
                handleDefaultMode(theName, expected);
            }
        } else if (event == XMLStreamConstants.END_ELEMENT) {
            final QName theName = super.getName();
            if (doDebug) {
                LOG.fine("read EndElement " + theName + " at " + getDepth());
            }
            
            final boolean dropped = inDropSet.contains(theName);
            if (!dropped) {
                EndEventMarker em = new EndEventMarker(theName, getDepth() + 1);
                List<ParsingEvent> pe = pushedAheadEvents.remove(em);
                if (null != pe) {
                    if (doDebug) {
                        LOG.fine("pushed event found");    
                    }
                    pushedBackEvents.addAll(pe);
                    currentEvent = pushedBackEvents.pop();
                    event = currentEvent.event;
                } else {
                    if (doDebug) {
                        LOG.fine("no pushed event found");    
                    }
                }
            } else {
                if (doDebug) {
                    LOG.fine("shallow-dropping end " + theName);    
                }
                event = next();
            }
        } else {
            if (doDebug) {
                LOG.fine("read other event " + event);    
            }
            currentEvent = null;
        }
        return event;
    }

    
    private void handleAppendMode(QName name, QName expected, ElementProperty appendProp) {
        final boolean doDebug = LOG.isLoggable(Level.FINE);

        if (appendProp.isChild()) {
            if (null == appendProp.getText()) {
                // ap-post-wrap
                pushedBackEvents.push(createStartElementEvent(appendProp.getName()));
                currentEvent = createStartElementEvent(expected);
                EndEventMarker em = new EndEventMarker(name, getDepth());
                if (doDebug) {
                    LOG.fine("ap-post-wrap " + appendProp.getName() + ", " + em);    
                }

                List<ParsingEvent> pe = new ArrayList<ParsingEvent>(2);
                pe.add(createEndElementEvent(expected));
                pe.add(createEndElementEvent(appendProp.getName()));
                pushedAheadEvents.put(em, pe);
            } else {
                // ap-post-incl
                currentEvent = createStartElementEvent(expected);
                EndEventMarker em = new EndEventMarker(name, getDepth());
                if (doDebug) {
                    LOG.fine("ap-pre-incl " + appendProp.getName() + "=" + appendProp.getText() + ", " + em);
                }

                List<ParsingEvent> pe = new ArrayList<ParsingEvent>();
                pe.add(createEndElementEvent(expected));
                pe.add(createEndElementEvent(appendProp.getName()));
                pe.add(createCharactersEvent(appendProp.getText()));
                pe.add(createStartElementEvent(appendProp.getName()));
                pushedAheadEvents.put(em, pe);
            }
        } else { 
            if (null == appendProp.getText()) {
                // ap-pre-wrap
                pushedBackEvents.push(createStartElementEvent(expected));
                currentEvent = createStartElementEvent(appendProp.getName());
                EndEventMarker em = new EndEventMarker(name, getDepth());
                if (doDebug) {
                    LOG.fine("ap-pre-wrap " + appendProp.getName() + ", " + em);
                }

                List<ParsingEvent> pe = new ArrayList<ParsingEvent>();
                pe.add(createEndElementEvent(appendProp.getName()));
                pe.add(createEndElementEvent(expected));
                pushedAheadEvents.put(em, pe);
            } else {
                // ap-pre-incl
                pushedBackEvents.push(createStartElementEvent(expected));
                pushedBackEvents.push(createEndElementEvent(appendProp.getName()));
                pushedBackEvents.push(createCharactersEvent(appendProp.getText()));
                currentEvent = createStartElementEvent(appendProp.getName());
                if (doDebug) {
                    LOG.fine("ap-pre-incl " + appendProp.getName() + "=" + appendProp.getText());
                }
            }
        }
    }
    
    private void handleDefaultMode(QName name, QName expected) {
        currentEvent = createStartElementEvent(expected);
        if (!name.equals(expected)) {
            EndEventMarker em = new EndEventMarker(name, getDepth());
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("default " + name + "->" + expected + ", " + em);
            }
            List<ParsingEvent> pe = new ArrayList<ParsingEvent>(1);
            pe.add(createEndElementEvent(expected));
            pushedAheadEvents.put(em, pe);
        }
    }
    
    private void handleDeepDrop() throws XMLStreamException {
        final int depth = getDepth();
        while (depth != getDepth() || super.next() != XMLStreamConstants.END_ELEMENT) {
            // get to the matching end element event
        }
    }


    public Object getProperty(String name) throws IllegalArgumentException {
        if (INTERN_NAMES.equals(name) || INTERN_NS.equals(name)) {
            return Boolean.FALSE;
        }
        return super.getProperty(name);
    }

    public String getLocalName() {
        if (currentEvent != null) {
            return currentEvent.name.getLocalPart();    
        } else {
            return super.getLocalName();
        }
    }


    public NamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    public String getPrefix() {
        QName name = readCurrentElement();
        String prefix = name.getPrefix();
        if (prefix.length() == 0) {
            prefix = namespaceContext.findUniquePrefix(name.getNamespaceURI());
        }
        return prefix;
    }
     
    public String getNamespaceURI(int index) {
        String ns = super.getNamespaceURI(index);
        String actualNs = nsMap.get(ns);
        if (actualNs != null) {
            return actualNs;
        } else {
            return ns;
        }
    }
    
    public String getNamespacePrefix(int index) {
        String ns = super.getNamespaceURI(index);
        String actualNs = nsMap.get(ns);
        if (actualNs != null) {
            return namespaceContext.findUniquePrefix(actualNs);
        } else {
            return namespaceContext.getPrefix(ns);
        }
    }
    
    public String getNamespaceURI() {
        if (currentEvent != null) {
            return currentEvent.name.getNamespaceURI();
        } else {
            return super.getNamespaceURI();
        }
    }

    private QName readCurrentElement() {
        if (currentEvent != null) {
            return currentEvent.name;
        }
        String ns = super.getNamespaceURI();
        String name = super.getLocalName();
        String prefix = super.getPrefix();
        return new QName(ns, name, prefix == null ? "" : prefix);
    }
    
    public QName getName() { 
        return new QName(getNamespaceURI(), getLocalName());
    }

    public int getAttributeCount() {
        if (!pushedBackEvents.empty()) {
            return 0;
        }
        checkAttributeIndexRange(-1);
        return attributesIndexes.size();
    }

    public String getAttributeLocalName(int arg0) {
        if (!pushedBackEvents.empty()) {
            throwIndexException(arg0, 0);
        }
        checkAttributeIndexRange(arg0);
        
        return getAttributeName(arg0).getLocalPart();
    }

    public QName getAttributeName(int arg0) {
        if (!pushedBackEvents.empty()) {
            throwIndexException(arg0, 0);
        }
        checkAttributeIndexRange(arg0);
        QName aname = super.getAttributeName(attributesIndexes.get(arg0));
        QName expected = inAttributesMap.get(aname);
        
        return expected == null ? aname : expected;
    }

    public String getAttributeNamespace(int arg0) {
        if (!pushedBackEvents.empty()) {
            throwIndexException(arg0, 0);
        }
        checkAttributeIndexRange(arg0);

        return getAttributeName(arg0).getNamespaceURI();
    }

    public String getAttributePrefix(int arg0) {
        if (!pushedBackEvents.empty()) {
            throwIndexException(arg0, 0);
        }
        checkAttributeIndexRange(arg0);

        QName aname = getAttributeName(arg0);
        if (XMLConstants.NULL_NS_URI.equals(aname.getNamespaceURI())) {
            return "";
        } else {
            String actualNs = nsMap.get(aname.getNamespaceURI());
            if (actualNs != null) {
                return namespaceContext.findUniquePrefix(actualNs);
            } else {
                return namespaceContext.getPrefix(aname.getNamespaceURI());
            }
        }
    }

    public String getAttributeType(int arg0) {
        if (!pushedBackEvents.empty()) {
            throwIndexException(arg0, 0);
        }
        checkAttributeIndexRange(arg0);
        return super.getAttributeType(attributesIndexes.get(arg0));
    }

    public String getAttributeValue(int arg0) {
        if (!pushedBackEvents.empty()) {
            throwIndexException(arg0, 0);
        }
        checkAttributeIndexRange(arg0);
        return super.getAttributeValue(attributesIndexes.get(arg0));
    }

    public String getAttributeValue(String namespace, String localName) {
        if (!pushedBackEvents.empty()) {
            return null;
        }
        checkAttributeIndexRange(-1);
        //TODO need reverse lookup
        return super.getAttributeValue(namespace, localName);    
    }

    public String getText() {
        if (currentEvent != null) {
            return currentEvent.value;
        }
        String superText = super.getText();
        if (replaceText != null) {
            superText = replaceText;
            replaceText = null;
        }
        return superText;
    }

    public char[] getTextCharacters() {
        if (currentEvent != null && currentEvent != null) {
            return currentEvent.value.toCharArray();
        }
        char[] superChars = super.getTextCharacters();
        if (replaceText != null) {
            superChars = replaceText.toCharArray();
            replaceText = null;
        }
        return superChars;
    }

    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) 
        throws XMLStreamException {
        if (currentEvent != null && currentEvent != null) {
            int len = currentEvent.value.length() - sourceStart;
            if (len > length) {
                len = length;
            }
            currentEvent.value.getChars(sourceStart, sourceStart + len, target, targetStart);
            return len;
        }

        return super.getTextCharacters(sourceStart, target, targetStart, length);
    }

    public int getTextLength() {
        if (currentEvent != null && currentEvent.value != null) {
            return currentEvent.value.length();
        }
        return super.getTextLength();
    }

    /**
     * Checks the index range for the current attributes set.
     * If the attributes are not indexed for the current element context, they
     * will be indexed. 
     * @param index
     */
    private void checkAttributeIndexRange(int index) {
        if (!attributesIndexed) {
            attributesIndexes.clear();
            final int c = super.getAttributeCount();
            for (int i = 0; i < c; i++) {
                QName aname = super.getAttributeName(i);
                QName expected = inAttributesMap.get(aname);
                if (expected == null || !isEmptyQName(expected)) {
                    attributesIndexes.add(i);
                }
            }
            attributesIndexed = true;
        }
        if (index >= attributesIndexes.size()) {
            throwIndexException(index, attributesIndexes.size());
        }
    }

    private static boolean isEmptyQName(QName qname) {
        return XMLConstants.NULL_NS_URI.equals(qname.getNamespaceURI()) && "".equals(qname.getLocalPart());
    }

    private void throwIndexException(int index, int size) {
        throw new IllegalArgumentException("Invalid index " + index 
                                           + "; current element has only " + size + " attributes");
    }

    private static ParsingEvent createStartElementEvent(QName name) {
        return new ParsingEvent(XMLStreamConstants.START_ELEMENT, name, null);
    }

    private static ParsingEvent createEndElementEvent(QName name) {
        return new ParsingEvent(XMLStreamConstants.END_ELEMENT, name, null);
    }

    private static ParsingEvent createCharactersEvent(String value) {
        return new ParsingEvent(XMLStreamConstants.CHARACTERS, null, value);
    }
    
    private static class ParsingEvent {
        private int event;
        private QName name;
        private String value;
        
        public ParsingEvent(int event, QName name, String value) {
            this.event = event;
            this.name = name;
            this.value = value;
        }
        
        public String toString() {
            return new StringBuffer().append("Event(").
                append(event).append(", ").append(name).append(", ").append(value).append(")").
                toString();
        }
    }
    
    private static class EndEventMarker {
        private QName name;
        private int level;
        
        public EndEventMarker(QName name, int level) {
            this.name = name;
            this.level = level;
        }

        /** {@inheritDoc}*/
        @Override
        public int hashCode() {
            return name.hashCode() ^ level;
        }

        /** {@inheritDoc}*/
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof EndEventMarker) {
                EndEventMarker e = (EndEventMarker)obj;
                return name.equals(e.name) && level == e.level;
            }
            return false;
        }
        
        public String toString() {
            return new StringBuffer().append("Marker(").
                append(name).append(", ").append(level).append(")").
                toString();
        }
    }
}
