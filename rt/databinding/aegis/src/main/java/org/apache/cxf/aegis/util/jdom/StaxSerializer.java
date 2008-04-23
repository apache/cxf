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
package org.apache.cxf.aegis.util.jdom;

import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.Comment;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.EntityRef;
import org.jdom.Namespace;
import org.jdom.Text;

public class StaxSerializer {
    public void writeDocument(Document doc, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartDocument("1.0");

        for (Iterator itr = doc.getContent().iterator(); itr.hasNext();) {
            Content content = (Content)itr.next();

            if (content instanceof Element) {
                writeElement((Element)content, writer);
            }
        }

        writer.writeEndDocument();
    }

    public void writeElement(Element e, XMLStreamWriter writer) throws XMLStreamException {
        // need to check if the namespace is declared before we write the
        // start element because that will put the namespace in the context.
        String elPrefix = e.getNamespacePrefix();
        String elUri = e.getNamespaceURI();

        String boundPrefix = writer.getPrefix(elUri);
        boolean writeElementNS = false;
        if (boundPrefix == null || !elPrefix.equals(boundPrefix)) {
            writeElementNS = true;
        }

        writer.writeStartElement(elPrefix, e.getName(), elUri);

        List namespaces = e.getAdditionalNamespaces();
        for (Iterator itr = namespaces.iterator(); itr.hasNext();) {
            Namespace ns = (Namespace)itr.next();

            String prefix = ns.getPrefix();
            String uri = ns.getURI();

            writer.setPrefix(prefix, uri);
            writer.writeNamespace(prefix, uri);

            if (elUri.equals(uri) && elPrefix.equals(prefix)) {
                writeElementNS = false;
            }
        }

        for (Iterator itr = e.getAttributes().iterator(); itr.hasNext();) {
            Attribute attr = (Attribute)itr.next();
            String attPrefix = attr.getNamespacePrefix();
            String attUri = attr.getNamespaceURI();

            if (attUri == null || attUri.equals("")) {
                writer.writeAttribute(attr.getName(), attr.getValue());
            } else {
                writer.writeAttribute(attPrefix, attUri, attr.getName(), attr.getValue());

                if (!isDeclared(writer, attPrefix, attUri)) {
                    if (elUri.equals(attUri) && elPrefix.equals(attPrefix)) {
                        if (writeElementNS) {
                            writer.setPrefix(attPrefix, attUri);
                            writer.writeNamespace(attPrefix, attUri);
                            writeElementNS = false;
                        }
                    } else {
                        writer.setPrefix(attPrefix, attUri);
                        writer.writeNamespace(attPrefix, attUri);
                    }
                }
            }
        }

        if (writeElementNS) {
            if (elPrefix == null || elPrefix.length() == 0) {
                writer.writeDefaultNamespace(elUri);
            } else {
                writer.writeNamespace(elPrefix, elUri);
            }
        }

        for (Iterator itr = e.getContent().iterator(); itr.hasNext();) {
            Content n = (Content)itr.next();
            if (n instanceof CDATA) {
                writer.writeCData(n.getValue());
            } else if (n instanceof Text) {
                writer.writeCharacters(((Text)n).getText());
            } else if (n instanceof Element) {
                writeElement((Element)n, writer);
            } else if (n instanceof Comment) {
                writer.writeComment(n.getValue());
            } else if (n instanceof EntityRef) {
                // EntityRef ref = (EntityRef) n;
                // writer.writeEntityRef(ref.)
            }
        }

        writer.writeEndElement();
    }

    /**
     * @param writer
     * @param prefix
     * @param uri
     * @throws XMLStreamException
     */
    private boolean isDeclared(XMLStreamWriter writer, String prefix, String uri) throws XMLStreamException {
        for (Iterator pxs = writer.getNamespaceContext().getPrefixes(uri); pxs.hasNext();) {
            String px = (String)pxs.next();
            if (px.equals(prefix)) {
                return true;
            }
        }
        return false;
    }
}
