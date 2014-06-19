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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.shared.TransferTools;
import org.apache.cxf.ws.transfer.shared.faults.UnknownResource;

/**
 *
 * @author erich
 */
public class MemoryResourceManager implements ResourceManager {
    
    public static final String REF_NAMESPACE = "http://cxf.apache.org/rt/ws/transfer/MemoryResourceManager";
    
    public static final String REF_LOCAL_NAME = "UUID";

    protected Map<String, String> storage;
    
    public MemoryResourceManager() {
        storage = new HashMap<String, String>();
    }

    @Override
    public Representation get(ReferenceParametersType ref) {
        String uuid = getUUID(ref);
        if (!storage.containsKey(uuid)) {
            throw new UnknownResource();
        }
        String resource = storage.get(uuid);
        if (resource.isEmpty()) {
            return new Representation();
        } else {
            Document doc = TransferTools.parse(new InputSource(new StringReader(storage.get(uuid))));
            Representation representation = new Representation();
            representation.setAny(doc.getDocumentElement());
            return representation;
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
        String uuid = getUUID(ref);
        if (!storage.containsKey(uuid)) {
            throw new UnknownResource();
        }
        StringWriter writer = new StringWriter();
        TransferTools.transform(new DOMSource((Node) newRepresentation.getAny()), new StreamResult(writer));
        storage.put(uuid, writer.toString());
    }

    @Override
    public ReferenceParametersType create(Representation initRepresentation) {
        // Store xmlResource
        String uuid = UUID.randomUUID().toString();
        StringWriter writer = new StringWriter();
        TransferTools.transform(new DOMSource((Node) initRepresentation.getAny()), new StreamResult(writer));
        storage.put(uuid, writer.toString());
        // Create referenceParameter
        ReferenceParametersType refParam = new ReferenceParametersType();

        Element uuidEl = TransferTools.createElementNS(REF_NAMESPACE, REF_LOCAL_NAME);
        uuidEl.setTextContent(uuid);
        refParam.getAny().add(uuidEl);
        return refParam;
    }
    
    private String getUUID(ReferenceParametersType ref) {
        for (Object object : ref.getAny()) {
            Element element = (Element) object;
            if (
                REF_NAMESPACE.equals(element.getNamespaceURI())
                && REF_LOCAL_NAME.equals(element.getLocalName())
            ) {
                return element.getTextContent();
            }
        }
        throw new UnknownResource();
    }
}
