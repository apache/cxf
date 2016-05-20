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
package org.apache.cxf.ws.security.wss4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.xml.security.encryption.AbstractSerializer;
import org.apache.xml.security.encryption.XMLEncryptionException;

/**
 * Converts <code>String</code>s into <code>Node</code>s and visa versa using CXF's StaxUtils
 */
public class StaxSerializer extends AbstractSerializer {

    /**
     * @param source
     * @param ctx
     * @return the Node resulting from the parse of the source
     * @throws XMLEncryptionException
     */
    public Node deserialize(byte[] source, Node ctx) throws XMLEncryptionException {
        byte[] fragment = createContext(source, ctx);
        return deserialize(ctx, new InputSource(new ByteArrayInputStream(fragment)));
    }

    /**
     * @param source
     * @param ctx
     * @return the Node resulting from the parse of the source
     * @throws XMLEncryptionException
     */
    public Node deserialize(String source, Node ctx) throws XMLEncryptionException {
        String fragment = createContext(source, ctx);
        return deserialize(ctx, new InputSource(new StringReader(fragment)));
    }
    
    @Override
    public byte[] serializeToByteArray(Element element) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(baos);
            StaxUtils.copy(element, writer);
            writer.close();
            return baos.toByteArray();
        }
    }
    
    @Override
    public byte[] serializeToByteArray(NodeList content) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(baos);
            for (int i = 0; i < content.getLength(); i++) {
                StaxUtils.copy(new DOMSource(content.item(i)), writer);
            }
            writer.close();
            return baos.toByteArray();
        }
    }

    /**
     * @param ctx
     * @param inputSource
     * @return the Node resulting from the parse of the source
     * @throws XMLEncryptionException
     */
    private Node deserialize(Node ctx, InputSource inputSource) throws XMLEncryptionException {
        
        Document contextDocument = null;
        if (Node.DOCUMENT_NODE == ctx.getNodeType()) {
            contextDocument = (Document)ctx;
        } else {
            contextDocument = ctx.getOwnerDocument();
        }
        
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(inputSource);
        
        // Import to a dummy fragment
        DocumentFragment dummyFragment = contextDocument.createDocumentFragment();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(new DOMResult(dummyFragment));
        
        try {
            StaxUtils.copy(reader, writer);
        } catch (XMLStreamException ex) {
            throw new XMLEncryptionException(ex);
        }
        
        // Remove the "dummy" wrapper
        DocumentFragment result = contextDocument.createDocumentFragment();
        Node child = dummyFragment.getFirstChild().getFirstChild();
        while (child != null) {
            Node nextChild = child.getNextSibling();
            result.appendChild(child);
            child = nextChild;
        }
        
        return result;
    }

}
