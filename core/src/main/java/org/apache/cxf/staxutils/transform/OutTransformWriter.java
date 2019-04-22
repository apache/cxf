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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.DelegatingXMLStreamWriter;

public class OutTransformWriter extends DelegatingXMLStreamWriter {
    private String defaultNamespace;
    private QNamesMap elementsMap;
    private QNamesMap attributesMap;
    private Map<QName, ElementProperty> appendMap = new HashMap<>(5);
    private Map<String, String> nsMap = new HashMap<>(5);
    private List<Set<String>> writtenUris = new LinkedList<>();

    private Set<QName> dropElements;
    private List<List<ParsingEvent>> pushedAheadEvents = new LinkedList<>();
    private List<QName> elementsStack = new LinkedList<>();
    private String replaceNamespace;
    private String replaceText;
    private int currentDepth;
    private int dropDepth;
    private boolean attributesToElements;
    private DelegatingNamespaceContext namespaceContext;

    public OutTransformWriter(XMLStreamWriter writer,
                              Map<String, String> outMap) {
        this(writer, outMap, null, null, null, false, null);
    }

    public OutTransformWriter(XMLStreamWriter writer,
                              Map<String, String> outMap,
                              Map<String, String> append,
                              List<String> dropEls,
                              boolean attributesToElements,
                              String defaultNamespace) {
        this(writer, outMap, append, dropEls, null, attributesToElements, defaultNamespace);
    }

    public OutTransformWriter(XMLStreamWriter writer,
                              Map<String, String> outEMap,
                              Map<String, String> append,
                              List<String> dropEls,
                              Map<String, String> outAMap,
                              boolean attributesToElements,
                              String defaultNamespace) {
        super(writer);
        elementsMap = new QNamesMap(outEMap == null ? 0 : outEMap.size());
        attributesMap = new QNamesMap(outAMap == null ? 0 : outAMap.size());
        TransformUtils.convertToQNamesMap(outEMap, elementsMap, nsMap);
        TransformUtils.convertToQNamesMap(outAMap, attributesMap, null);

        TransformUtils.convertToMapOfElementProperties(append, appendMap);
        dropElements = DOMUtils.convertStringsToQNames(dropEls);
        this.attributesToElements = attributesToElements;
        namespaceContext = new DelegatingNamespaceContext(
            writer.getNamespaceContext(), nsMap);
        this.defaultNamespace = defaultNamespace;
    }

    @Override
    public void writeNamespace(String prefix, String uri) throws XMLStreamException {
        if (StringUtils.isEmpty(prefix) || "xmlns".equals(prefix)) {
            writeDefaultNamespace(uri);
            return;
        }
        if (matchesDropped(true)) {
            return;
        }
        String value = nsMap.get(uri);
        if ((value != null && value.isEmpty())
            || uri.equals(replaceNamespace)) {
            return;
        }

        uri = value != null ? value : uri;

        if (writtenUris.get(0).contains(uri)
            && (prefix.isEmpty() || prefix.equals(namespaceContext.getPrefix(uri)))) {
            return;
        }

        if (defaultNamespace != null && defaultNamespace.equals(uri)) {
            super.writeDefaultNamespace(uri);
            namespaceContext.addPrefix("", uri);
        } else {
            if (prefix.isEmpty()) {
                prefix = namespaceContext.findUniquePrefix(uri);
            }
            super.writeNamespace(prefix, uri);
            namespaceContext.addPrefix(prefix, uri);
        }
        writtenUris.get(0).add(uri);
    }


    @Override
    public void writeDefaultNamespace(String uri) throws XMLStreamException {
        if (matchesDropped(true)) {
            return;
        }
        String value = nsMap.get(uri);
        if ((value != null && value.isEmpty())
            || (isDefaultNamespaceRedefined() && !isDefaultNamespaceRedefined(uri))
            || uri.equals(replaceNamespace)) {
            return;
        }

        uri = value != null ? value : uri;

        if (writtenUris.get(0).contains(uri) && namespaceContext.getPrefix(uri).isEmpty()) {
            return;
        }
        super.writeDefaultNamespace(uri);
        namespaceContext.addPrefix("", uri);
        writtenUris.get(0).add(uri);
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        if (matchesDropped(true)) {
            return;
        }
        String value = nsMap.get(uri);
        if ((value != null && value.isEmpty())
            || (isDefaultNamespaceRedefined() && !isDefaultNamespaceRedefined(uri))) {
            return;
        }

        super.setDefaultNamespace(value != null ? value : uri);
    }

    @Override
    public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
        currentDepth++;
        namespaceContext.down();
        if (matchesDropped(false)) {
            return;
        }
        Set<String> s;
        if (writtenUris.isEmpty()) {
            s = new HashSet<>();
        } else {
            s = new HashSet<>(writtenUris.get(0));
        }
        writtenUris.add(0, s);

        final QName theName = new QName(uri, local, prefix);
        final ElementProperty appendProp = appendMap.remove(theName);
        final boolean replaceContent = appendProp != null && theName.equals(appendProp.getName());

        final boolean dropped = dropElements.contains(theName);
        QName expected = elementsMap.get(theName);
        if (expected == null) {
            expected = theName;
        } else {
            if (prefix.isEmpty() && !expected.getNamespaceURI().isEmpty()
                && theName.getNamespaceURI().isEmpty()) {
                // if the element is promoted to a qualified element, use the prefix bound
                // to that namespace. If the namespace is unbound, generate a new prefix and
                // write its declaration later.
                prefix = namespaceContext.getPrefix(expected.getNamespaceURI());
                if (prefix == null) {
                    prefix = namespaceContext.findUniquePrefix(expected.getNamespaceURI());
                }
            } else if (!prefix.isEmpty() && expected.getNamespaceURI().isEmpty()) {
                // if the element is demoted to a unqualified element, use an empty prefix.
                prefix = "";
            }
            expected = new QName(expected.getNamespaceURI(), expected.getLocalPart(), prefix);
        }
        List<ParsingEvent> pe = null;
        if (appendProp != null && !replaceContent) {
            if (!appendProp.isChild()) {
                // ap-pre-*
                QName appendQName = appendProp.getName();
                String theprefix = namespaceContext.getPrefix(appendQName.getNamespaceURI());
                boolean nsadded = false;
                if (theprefix == null) {
                    nsadded = true;
                    theprefix = namespaceContext.getPrefix(appendQName.getNamespaceURI());
                    if (theprefix == null
                        && (appendQName.getNamespaceURI().equals(expected.getNamespaceURI())
                            && expected.getPrefix().length() > 0)) {
                        theprefix = expected.getPrefix();
                    } else if (theprefix == null) {
                        theprefix = namespaceContext.findUniquePrefix(appendQName.getNamespaceURI());
                    }
                    if (theprefix == null) {
                        theprefix = "";
                    }
                }
                write(new QName(appendQName.getNamespaceURI(), appendQName.getLocalPart(), theprefix), false);
                if (nsadded && theprefix.length() > 0) {
                    writeNamespace(theprefix, appendQName.getNamespaceURI());
                }
                if (appendProp.getText() == null) {
                    // ap-pre-wrap
                    currentDepth++;
                    pe = new ArrayList<>();
                    pe.add(TransformUtils.createEndElementEvent(expected));
                    pe.add(TransformUtils.createEndElementEvent(appendProp.getName()));
                    pushedAheadEvents.add(0, null);
                    elementsStack.add(0, appendQName);
                } else {
                    // ap-pre-incl
                    super.writeCharacters(appendProp.getText());
                    super.writeEndElement();
                }
            }
        } else if (null != appendProp && replaceContent) {
            //
            replaceText = appendProp.getText();
        } else if (dropped) {
            // unwrap the current element (shallow drop)
            elementsStack.add(0, theName);
            return;
        } else if (TransformUtils.isEmptyQName(expected)) {
            // skip the current element (deep drop));
            dropDepth = currentDepth;
            return;
        }
        write(expected, false);
        if (!expected.getNamespaceURI().isEmpty() && theName.getNamespaceURI().isEmpty()) {
            // the element is promoted to a qualified element, thus write its declaration
            writeNamespace(expected.getPrefix(), expected.getNamespaceURI());
        }
        pushedAheadEvents.add(0, pe);
        elementsStack.add(0, expected);
        replaceNamespace = expected.getNamespaceURI().equals(theName.getNamespaceURI())
            ? null : theName.getNamespaceURI();

        if (appendProp != null && !replaceContent && appendProp.isChild()) {
            // ap-post-*
            QName appendQName = appendProp.getName();
            String theprefix = namespaceContext.getPrefix(appendQName.getNamespaceURI());

            if (appendProp.getText() == null) {
                // ap-post-wrap
                write(new QName(appendQName.getNamespaceURI(), appendQName.getLocalPart(),
                                theprefix == null ? "" : theprefix), false);
                if (namespaceContext.getPrefix(appendQName.getNamespaceURI()) == null) {
                    this.writeNamespace(theprefix, uri);
                }
                currentDepth++;
                pe = new ArrayList<>();
                pe.add(TransformUtils.createEndElementEvent(appendProp.getName()));
                pe.add(TransformUtils.createEndElementEvent(expected));
                pushedAheadEvents.add(0, pe);
                elementsStack.add(0, appendQName);
            } else {
                // ap-post-incl
                pushedAheadEvents.remove(0);
                pe = new ArrayList<>();
                pe.add(TransformUtils.createStartElementEvent(appendProp.getName()));
                pe.add(TransformUtils.createCharactersEvent(appendProp.getText()));
                pe.add(TransformUtils.createEndElementEvent(appendProp.getName()));
                pe.add(TransformUtils.createEndElementEvent(expected));
                pushedAheadEvents.add(0, pe);
            }
        }
    }


    @Override
    public void writeStartElement(String uri, String local) throws XMLStreamException {
        /*
        pushedAheadEvents.push(null);
        elementsStack.push(new QName(uri, local));
        super.writeStartElement(uri, local);
        */
        writeStartElement("", local, uri);
    }

    @Override
    public void writeStartElement(String local) throws XMLStreamException {
        writeStartElement("", local, "");
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        final boolean indrop = matchesDropped(false);
        namespaceContext.up();
        --currentDepth;
        if (indrop) {
            if (dropDepth > currentDepth) {
                dropDepth = 0;
            }
            return;
        }
        if (!writtenUris.isEmpty()) {
            writtenUris.remove(0);
        }
        QName theName = elementsStack.remove(0);
        final boolean dropped = dropElements.contains(theName);
        if (!dropped) {
            List<ParsingEvent> pes = pushedAheadEvents.remove(0);
            if (null != pes) {
                for (ParsingEvent pe : pes) {
                    switch (pe.getEvent()) {
                    case XMLStreamConstants.START_ELEMENT:
                        write(pe.getName(), true);
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        super.writeEndElement();
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        super.writeCharacters(pe.getValue());
                        break;
                    default:
                    }
                }
            } else {
                super.writeEndElement();
            }
        }
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        if (matchesDropped(false)) {
            return;
        }
        if (replaceText != null) {
            text = replaceText;
            replaceText = null;
        }
        super.writeCharacters(text);
    }
    
    @Override
    public void writeCharacters(char[] text, int arg1, int arg2) throws XMLStreamException {
        if (matchesDropped(false)) {
            return;
        }
        if (replaceText != null) {
            text = replaceText.toCharArray();
            replaceText = null;
        }
        super.writeCharacters(text, arg1, arg2);
    }

    private void write(QName qname, boolean replacePrefix) throws XMLStreamException {
        boolean writeNs = false;
        String prefix = "";
        if (!qname.getNamespaceURI().isEmpty()) {
            if ((replacePrefix || isDefaultNamespaceRedefined())
                && qname.getPrefix().isEmpty()) {
                // if the default namespace is configured to be replaced, a non-empty prefix must be assigned
                prefix = namespaceContext.getPrefix(qname.getNamespaceURI());
                if (prefix == null) {
                    prefix = namespaceContext.findUniquePrefix(qname.getNamespaceURI());
                    writeNs = true;
                }
            } else {
                prefix = qname.getPrefix();
            }

        }
        if (isDefaultNamespaceRedefined(qname.getNamespaceURI())) {
            prefix = "";
        }
        super.writeStartElement(prefix, qname.getLocalPart(), qname.getNamespaceURI());
        if (writeNs
            || !qname.getNamespaceURI().equals(namespaceContext.getNamespaceURI(prefix))) {
            this.writeNamespace(prefix, qname.getNamespaceURI());
        }
    }

    private boolean isDefaultNamespaceRedefined() {
        return defaultNamespace != null;
    }

    private boolean isDefaultNamespaceRedefined(String uri) {
        return isDefaultNamespaceRedefined() && defaultNamespace.equals(uri);
    }

    private boolean matchesDropped(boolean shallow) {
        return (dropDepth > 0 && dropDepth <= currentDepth)
            || (shallow && (!elementsStack.isEmpty() && dropElements.contains(elementsStack.get(0))));
    }


    @Override
    public NamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    @Override
    public void writeAttribute(String prefix, String uri, String local, String value)
        throws XMLStreamException {
        if (matchesDropped(false)) {
            return;
        }

        QName expected = attributesMap.get(new QName(uri, local, prefix));
        if (expected != null) {
            if (TransformUtils.isEmptyQName(expected)) {
                return;
            }
            uri = expected.getNamespaceURI();
            local = expected.getLocalPart();
        }
        if (!attributesToElements) {
            super.writeAttribute(prefix, uri, local, value);
        } else {
            writeAttributeAsElement(uri, local, value);
        }
    }

    @Override
    public void writeAttribute(String local, String value) throws XMLStreamException {
        if (matchesDropped(false)) {
            return;
        }
        String uri = XMLConstants.NULL_NS_URI;
        QName expected = attributesMap.get(new QName("", local));
        if (expected != null) {
            if (TransformUtils.isEmptyQName(expected)) {
                return;
            }
            uri = expected.getNamespaceURI();
            local = expected.getLocalPart();
        }
        if (!attributesToElements) {
            if (!uri.isEmpty()) {
                super.writeAttribute(uri, local, value);
            } else {
                super.writeAttribute(local, value);
            }
        } else {
            writeAttributeAsElement(uri, local, value);
        }
    }

    private void writeAttributeAsElement(String uri, String local, String value)
        throws XMLStreamException {
        this.writeStartElement(uri, local);
        this.writeCharacters(value);
        this.writeEndElement();
    }
}
