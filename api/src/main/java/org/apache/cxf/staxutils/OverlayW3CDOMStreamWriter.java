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


import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.util.StringUtils;


/**
 * Special StreamWriter that will "overlay" any write events onto the DOM.
 * If the startElement ends up writing an element that already exists at that
 * location, it will just walk into it instead of creating a new element
 */
public class OverlayW3CDOMStreamWriter extends W3CDOMStreamWriter {

    List<Boolean> isOverlaidStack = new LinkedList<Boolean>();
    boolean isOverlaid = true;
    
    public OverlayW3CDOMStreamWriter(Document document) {
        super(document);
    }

    public OverlayW3CDOMStreamWriter(Element e) {
        super(e);
    }
    
    @Override
    public void writeEndElement() throws XMLStreamException {
        isOverlaid = isOverlaidStack.remove(0);
        super.writeEndElement();
    }
    public void writeStartElement(String local) throws XMLStreamException {
        isOverlaidStack.add(0, isOverlaid);
        if (isOverlaid) {
            Element nd = getCurrentNode();
            Node nd2 = null;
            if (nd == null) {
                nd2 = getDocument().getDocumentElement();
            } else {
                nd2 = nd.getFirstChild();
            }
            while (nd2 != null) {
                if (nd2.getNodeType() == Node.ELEMENT_NODE 
                    && local.equals(nd2.getLocalName())
                    && StringUtils.isEmpty(nd2.getNamespaceURI())) {
                    setChild((Element)nd2, false);
                    return;
                }
                nd2 = nd2.getNextSibling();
            }
        }
        isOverlaid = false;
        super.writeStartElement(local);
    }

    public void writeStartElement(String namespace, String local) throws XMLStreamException {
        isOverlaidStack.add(0, isOverlaid);
        if (isOverlaid) {
            Element nd = getCurrentNode();
            Node nd2 = null;
            if (nd == null) {
                nd2 = getDocument().getDocumentElement();
            } else {
                nd2 = nd.getFirstChild();
            }
            while (nd2 != null) {
                if (nd2.getNodeType() == Node.ELEMENT_NODE 
                    && local.equals(nd2.getLocalName())
                    && namespace.equals(nd2.getNamespaceURI())) {
                    setChild((Element)nd2, false);
                    return;
                }
                nd2 = nd2.getNextSibling();
            }
        }
        isOverlaid = false;
        super.writeStartElement(namespace, local);
    }

    public void writeStartElement(String prefix, String local, String namespace) throws XMLStreamException {
        if (prefix == null || prefix.equals("")) {
            writeStartElement(namespace, local);
        } else {
            isOverlaidStack.add(0, isOverlaid);
            if (isOverlaid) {
    
                Element nd = getCurrentNode();
                Node nd2 = null;
                if (nd == null) {
                    nd2 = getDocument().getDocumentElement();
                } else {
                    nd2 = nd.getFirstChild();
                }
                
                while (nd2 != null) {
                    if (nd2.getNodeType() == Node.ELEMENT_NODE 
                        && local.equals(nd2.getLocalName())
                        && namespace.equals(nd2.getNamespaceURI())) {
                        setChild((Element)nd2, false);
                        return;
                    }
                    nd2 = nd2.getNextSibling();
                }
            }
            isOverlaid = false;
            super.writeStartElement(prefix, local, namespace);
        }
    }
    
}
