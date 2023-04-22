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
package org.apache.cxf.ws.security.sts;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLServiceFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class STSClientTest {

    @Test
    public void testConfigureViaEPR() throws Exception {

        final Set<Class<?>> addressingClasses = new HashSet<>();
        addressingClasses.add(org.apache.cxf.ws.addressing.wsdl.ObjectFactory.class);
        addressingClasses.add(org.apache.cxf.ws.addressing.ObjectFactory.class);

        JAXBContext ctx = JAXBContextCache.getCachedContextAndSchemas(addressingClasses, null, null, null,
                                                                      true).getContext();
        Unmarshaller um = ctx.createUnmarshaller();
        InputStream inStream = getClass().getResourceAsStream("epr.xml");
        JAXBElement<?> el = (JAXBElement<?>)um.unmarshal(inStream);
        EndpointReferenceType ref = (EndpointReferenceType)el.getValue();

        Bus bus = BusFactory.getThreadDefaultBus();
        STSClient client = new STSClient(bus);
        client.configureViaEPR(ref, false);

        assertEquals("http://localhost:8080/jaxws-samples-wsse-policy-trust-sts/SecurityTokenService?wsdl",
                     client.getWsdlLocation());
        assertEquals(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/", "SecurityTokenService"),
                     client.getServiceQName());
        assertEquals(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/", "UT_Port"),
                     client.getEndpointQName());
    }

    // A unit test to make sure that we can parse a WCF wsdl properly. See CXF-5817.
    @Test
    public void testWCFWsdl() throws Exception {
        Bus bus = BusFactory.getThreadDefaultBus();

        // Load WSDL
        InputStream inStream = getClass().getResourceAsStream("wcf.wsdl");
        Document doc = StaxUtils.read(inStream);


        NodeList metadataSections =
            doc.getElementsByTagNameNS("http://schemas.xmlsoap.org/ws/2004/09/mex", "MetadataSection");
        Element wsdlDefinition = null;
        List<Element> schemas = new ArrayList<>();
        for (int i = 0; i < metadataSections.getLength(); i++) {
            Node node = metadataSections.item(i);
            if (node instanceof Element) {
                Element element = (Element)node;
                String dialect = element.getAttributeNS(null, "Dialect");
                if ("http://schemas.xmlsoap.org/wsdl/".equals(dialect)) {
                    wsdlDefinition = DOMUtils.getFirstElement(element);
                } else if ("http://www.w3.org/2001/XMLSchema".equals(dialect)) {
                    schemas.add(DOMUtils.getFirstElement(element));
                }
            }
        }

        assertNotNull(wsdlDefinition);
        assertFalse(schemas.isEmpty());

        WSDLManager wsdlManager = bus.getExtension(WSDLManager.class);
        Definition definition = wsdlManager.getDefinition(wsdlDefinition);

        for (Element schemaElement : schemas) {
            QName schemaName =
                new QName(schemaElement.getNamespaceURI(), schemaElement.getLocalName());
            ExtensibilityElement
                exElement = wsdlManager.getExtensionRegistry().createExtension(Types.class, schemaName);
            ((Schema)exElement).setElement(schemaElement);
            definition.getTypes().addExtensibilityElement(exElement);
        }

        WSDLServiceFactory factory = new WSDLServiceFactory(bus, definition);
        SourceDataBinding dataBinding = new SourceDataBinding();
        factory.setDataBinding(dataBinding);
        Service service = factory.create();
        service.setDataBinding(dataBinding);

    }

}
