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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.annotation.Resource;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.ws.WebServiceContext;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.shared.faults.UnknownResource;

/**
 * In memory implementation for ResourceManager interface.
 */
public class MemoryResourceManager implements ResourceManager {

    public static final String REF_NAMESPACE = "http://cxf.apache.org/rt/ws/transfer/MemoryResourceManager";

    public static final String REF_LOCAL_NAME = "uuid";

    private static final Logger LOG = LogUtils.getL7dLogger(MemoryResourceManager.class);

    protected final Map<String, String> storage = new HashMap<>();

    @Resource
    private WebServiceContext context;

    @Override
    public Representation get(ReferenceParametersType ref) {
        String uuid = getUUID(ref);
        if (!storage.containsKey(uuid)) {
            throw new UnknownResource();
        }
        String resource = storage.get(uuid);
        if (resource.isEmpty()) {
            return new Representation();
        }
        final Document doc;
        try {
            doc = StaxUtils.read(new StringReader(storage.get(uuid)));
        } catch (XMLStreamException e) {
            LOG.severe(e.getLocalizedMessage());
            throw new SoapFault("Internal Error", getSoapVersion().getReceiver());
        }
        Representation representation = new Representation();
        representation.setAny(doc.getDocumentElement());
        return representation;
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
        Element representationEl = (Element) newRepresentation.getAny();
        if (representationEl == null) {
            storage.put(uuid, "");
        } else {
            storage.put(uuid, StaxUtils.toString(representationEl));
        }
    }

    @Override
    public ReferenceParametersType create(Representation initRepresentation) {
        // Store xmlResource
        String uuid = UUID.randomUUID().toString();
        Element representationEl = (Element) initRepresentation.getAny();
        if (representationEl == null) {
            storage.put(uuid, "");
        } else {
            storage.put(uuid, StaxUtils.toString(representationEl));
        }

        Element uuidEl = DOMUtils.getEmptyDocument().createElementNS(REF_NAMESPACE, REF_LOCAL_NAME);
        uuidEl.setTextContent(uuid);

        // Create referenceParameter
        ReferenceParametersType refParam = new ReferenceParametersType();
        refParam.getAny().add(uuidEl);
        return refParam;
    }

    private String getUUID(ReferenceParametersType ref) {
        for (Object object : ref.getAny()) {
            if (object instanceof JAXBElement) {
                JAXBElement<?> element = (JAXBElement<?>) object;
                QName qName = element.getName();
                if (
                        REF_NAMESPACE.equals(qName.getNamespaceURI())
                                && REF_LOCAL_NAME.equals(qName.getLocalPart())) {
                    return (String) element.getValue();
                }
            } else if (object instanceof Element) {
                Element element = (Element) object;
                if (
                        REF_NAMESPACE.equals(element.getNamespaceURI())
                                && REF_LOCAL_NAME.equals(element.getLocalName())) {
                    return element.getTextContent();
                }
            }
        }
        throw new UnknownResource();
    }

    private SoapVersion getSoapVersion() {
        WrappedMessageContext wmc = (WrappedMessageContext) context.getMessageContext();
        SoapMessage message = (SoapMessage) wmc.getWrappedMessage();
        return message.getVersion();
    }
}
