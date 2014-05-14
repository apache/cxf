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

package org.apache.cxf.ws.transfer.manager;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.apache.cxf.ws.transfer.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.shared.TransferConstants;
import org.apache.cxf.ws.transfer.shared.faults.UnknownResource;

/**
 *
 * @author erich
 */
public class MemoryResourceManager implements ResourceManager {
    
    protected static final String LOCAL_NAME = "UUID";
    
    protected Map<String, String> storage;
    
    public MemoryResourceManager() {
        storage = new HashMap<String, String>();
    }

    @Override
    public Representation get(ReferenceParametersType ref) {
        try {
            String uuid = getUUID(ref);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            if (!storage.containsKey(uuid)) {
                throw new UnknownResource();
            }
            Document doc = db.parse(storage.get(uuid));
            Representation representation = new Representation();
            representation.setAny(doc.getDocumentElement());
            return representation;
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        } catch (SAXException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void delete(ReferenceParametersType ref) {
        String uuid = getUUID(ref);
        if (!storage.containsKey(uuid)) {
            throw new UnknownResource();
        }
        storage.remove(uuid);
    }

    @Override
    public void put(ReferenceParametersType ref, Representation newRepresentation) {
        try {
            String uuid = getUUID(ref);
            if (!storage.containsKey(uuid)) {
                throw new UnknownResource();
            }
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource((Node) newRepresentation.getAny()), new StreamResult(writer));
            storage.put(uuid, writer.toString());
        } catch (TransformerConfigurationException ex) {
            throw new RuntimeException(ex);
        } catch (TransformerException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public ReferenceParametersType create(Representation initRepresentation) {
        try {
            // Store xmlResource
            String uuid = UUID.randomUUID().toString();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource((Node) initRepresentation.getAny()), new StreamResult(writer));
            storage.put(uuid, writer.toString());
            // Create referenceParameter
            ReferenceParametersType refParam = new ReferenceParametersType();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();
            Element uuidEl = doc.createElementNS(TransferConstants.TRANSFER_2011_03_NAMESPACE, LOCAL_NAME);
            uuidEl.setTextContent(uuid);
            refParam.getAny().add(uuidEl);
            return refParam;
        } catch (TransformerConfigurationException ex) {
            throw new RuntimeException(ex);
        } catch (TransformerException ex) {
            throw new RuntimeException(ex);
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private String getUUID(ReferenceParametersType ref) {
        for (Object object : ref.getAny()) {
            Element element = (Element) object;
            if (
                TransferConstants.TRANSFER_2011_03_NAMESPACE.equals(element.getNamespaceURI())
                && LOCAL_NAME.equals(element.getLocalName())
            ) {
                return element.getTextContent();
            }
        }
        throw new UnknownResource();
    }
}
