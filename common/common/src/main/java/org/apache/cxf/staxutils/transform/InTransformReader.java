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

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.staxutils.DepthXMLStreamReader;

public class InTransformReader extends DepthXMLStreamReader {
    
    private static final String INTERN_NAMES = "org.codehaus.stax2.internNames";
    private static final String INTERN_NS = "org.codehaus.stax2.internNsUris";
    
    private Stack<QName> elementStack = new Stack<QName>();
    private QNamesMap inElementsMap;
    private QNamesMap inAttributesMap;
    private Map<QName, ElementProperty> inAppendMap = new HashMap<QName, ElementProperty>(5);
    private Set<QName> inDropSet = new HashSet<QName>(5);
    private Map<String, String> nsMap = new HashMap<String, String>(5);
    private QName currentQName;
    private QName pushBackQName;
    private QName pushAheadQName;
    private String currentText;
    private String pushAheadText;
    private List<Integer> attributesIndexes = new ArrayList<Integer>(); 
    private int previousDepth = -1;
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
        if (isAtText()) {
            resetCurrentText();
            return currentText != null 
                   ? XMLStreamConstants.CHARACTERS : XMLStreamConstants.END_ELEMENT;
        } else if (isAtPushedQName()) {
            resetCurrentQName();
            pushElement();
            return XMLStreamConstants.START_ELEMENT;
        } else if (isAtMarkedDepth()) { 
            previousDepth = -1;
            popElement();
            return XMLStreamConstants.END_ELEMENT;
        } else {
            final int event = super.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                attributesIndexed = false;
                final QName theName = super.getName();
                final ElementProperty appendProp = inAppendMap.remove(theName);
                final boolean dropped = inDropSet.contains(theName);
                if (appendProp != null) {
                    if (appendProp.isChild()) {
                        // append-post-*
                        pushAheadQName = appendProp.getName();
                    } else {
                        // append-pre-*
                        currentQName = appendProp.getName();
                    }
                    if (appendProp.getText() != null) {
                        // append-*-include
                        pushAheadText = appendProp.getText();
                    } else {
                        // append-*-wrap
                        previousDepth = getDepth();
                        pushElement();
                    }
                } else if (dropped) {
                    // unwrap the current element (shallow drop)
                    previousDepth = getDepth();
                    return super.next();
                }
                
                QName expected = inElementsMap.get(theName);
                if (expected == null) {
                    expected = theName;
                } else if (isEmpty(expected)) {
                    // drop the current element (deep drop)
                    final int depth = getDepth();
                    while (depth != getDepth() || super.next() != XMLStreamConstants.END_ELEMENT) {
                        // get to the matching end element event
                    }
                    popElement();
                    return XMLStreamConstants.END_ELEMENT;
                }
                
                if (appendProp != null && appendProp.isChild()) {
                    // append-post-*
                    currentQName = expected;
                } else if (appendProp != null && !appendProp.isChild()) {
                    // append-pre-*
                    pushBackQName = expected;
                } else {
                    // no append
                    currentQName = expected;
                    pushElement();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                QName theName = super.getName();
                boolean dropped = inDropSet.contains(theName);
                if (dropped) {
                    super.next();
                }
                popElement();
            } else {
                // reset the element context and content
                currentQName = null;
                currentText = null;
            }
            return event;
        }
    }

    private boolean isAtText() {
        return pushAheadQName == null && (pushAheadText != null || currentText != null);
    }

    private boolean isAtPushedQName() {
        return pushBackQName != null || pushAheadQName != null;
    }
    
    private boolean isAtMarkedDepth() {
        return previousDepth != -1 && previousDepth == getDepth() + 1;
    }
    
    private boolean isEmpty(QName qname) {
        return XMLConstants.NULL_NS_URI.equals(qname.getNamespaceURI()) && "".equals(qname.getLocalPart());
    }
    
    private void popElement() {
        currentQName = elementStack.empty() ? null : elementStack.pop();
    }
    
    private void pushElement() {
        elementStack.push(currentQName);
    }

    public Object getProperty(String name) throws IllegalArgumentException {
        if (INTERN_NAMES.equals(name) || INTERN_NS.equals(name)) {
            return Boolean.FALSE;
        }
        return super.getProperty(name);
    }

    public String getLocalName() {
        if (currentQName != null) {
            return currentQName.getLocalPart();    
        } else {
            return super.getLocalName();
        }
    }

    private void resetCurrentQName() {
        currentQName = pushBackQName != null ? pushBackQName : pushAheadQName;
        pushBackQName = null;
        pushAheadQName = null;
    }
    
    private void resetCurrentText() {
        currentText = pushAheadText;
        pushAheadText = null;
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
        if (currentQName != null) {
            return currentQName.getNamespaceURI();
        } else {
            return super.getNamespaceURI();
        }
    }

    private QName readCurrentElement() {
        if (currentQName != null) {
            return currentQName;
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
        if (pushBackQName != null) {
            return 0;
        }
        checkAttributeIndexRange(-1);
        return attributesIndexes.size();
    }

    public String getAttributeLocalName(int arg0) {
        if (pushBackQName != null) {
            throwIndexException(arg0, 0);
        }
        checkAttributeIndexRange(arg0);
        
        return getAttributeName(arg0).getLocalPart();
    }

    public QName getAttributeName(int arg0) {
        if (pushBackQName != null) {
            throwIndexException(arg0, 0);
        }
        checkAttributeIndexRange(arg0);
        QName aname = super.getAttributeName(attributesIndexes.get(arg0));
        QName expected = inAttributesMap.get(aname);
        
        return expected == null ? aname : expected;
    }

    public String getAttributeNamespace(int arg0) {
        if (pushBackQName != null) {
            throwIndexException(arg0, 0);
        }
        checkAttributeIndexRange(arg0);

        return getAttributeName(arg0).getNamespaceURI();
    }

    public String getAttributePrefix(int arg0) {
        if (pushBackQName != null) {
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
        if (pushBackQName != null) {
            throwIndexException(arg0, 0);
        }
        checkAttributeIndexRange(arg0);
        return super.getAttributeType(attributesIndexes.get(arg0));
    }

    public String getAttributeValue(int arg0) {
        if (pushBackQName != null) {
            throwIndexException(arg0, 0);
        }
        checkAttributeIndexRange(arg0);
        return super.getAttributeValue(attributesIndexes.get(arg0));
    }

    public String getAttributeValue(String namespace, String localName) {
        if (pushBackQName != null) {
            return null;
        }
        checkAttributeIndexRange(-1);
        //TODO need reverse lookup
        return super.getAttributeValue(namespace, localName);    
    }

    public String getText() {
        if (currentText != null) {
            return currentText;
        }
        return super.getText();
    }

    public char[] getTextCharacters() {
        if (currentText != null) {
            return currentText.toCharArray();
        }
        return super.getTextCharacters();
    }

    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) 
        throws XMLStreamException {
        if (currentText != null) {
            int len = currentText.length() - sourceStart;
            if (len > length) {
                len = length;
            }
            currentText.getChars(sourceStart, sourceStart + len, target, targetStart);
            return len;
        }

        return super.getTextCharacters(sourceStart, target, targetStart, length);
    }

    public int getTextLength() {
        if (currentText != null) {
            return currentText.length();
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
                if (expected == null || !isEmpty(expected)) {
                    attributesIndexes.add(i);
                }
            }
            attributesIndexed = true;
        }
        if (index >= attributesIndexes.size()) {
            throwIndexException(index, attributesIndexes.size());
        }
    }
    
    private void throwIndexException(int index, int size) {
        throw new IllegalArgumentException("Invalid index " + index 
                                           + "; current element has only " + size + " attributes");
    }
}
