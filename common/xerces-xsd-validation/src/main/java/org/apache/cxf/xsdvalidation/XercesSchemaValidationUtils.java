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

package org.apache.cxf.xsdvalidation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerException;

import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.Document;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaSerializer;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
import org.apache.xerces.dom.DOMXSImplementationSourceImpl;
import org.apache.xerces.xs.LSInputList;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;

/**
 * 
 */
class XercesSchemaValidationUtils {

    private static final class ListLSInput implements LSInputList {
        private final List<DOMLSInput> inputs;

        private ListLSInput(List<DOMLSInput> inputs) {
            this.inputs = inputs;
        }

        public int getLength() {
            return inputs.size();
        }

        public LSInput item(int index) {
            return inputs.get(index);
        }
    }

    private XSImplementation impl;

    XercesSchemaValidationUtils() {
        DOMXSImplementationSourceImpl source = new org.apache.xerces.dom.DOMXSImplementationSourceImpl();
        impl = (XSImplementation)source.getDOMImplementation("XS-Loader");
    }

    void tryToParseSchemas(XmlSchemaCollection collection, DOMErrorHandler handler)
        throws XmlSchemaSerializerException, TransformerException {

        final List<DOMLSInput> inputs = new ArrayList<DOMLSInput>();
        final Map<String, LSInput> resolverMap = new HashMap<String, LSInput>();

        for (XmlSchema schema : collection.getXmlSchemas()) {
            if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(schema.getTargetNamespace())) {
                continue;
            }
            Document document = new XmlSchemaSerializer().serializeSchema(schema, false)[0];
            DOMLSInput input = new DOMLSInput(document, schema.getTargetNamespace());
            resolverMap.put(schema.getTargetNamespace(), input);
            inputs.add(input);
        }

        XSLoader schemaLoader = impl.createXSLoader(null);
        schemaLoader.getConfig().setParameter("validate", Boolean.TRUE);
        schemaLoader.getConfig().setParameter("error-handler", handler);
        schemaLoader.getConfig().setParameter("resource-resolver", new LSResourceResolver() {

            public LSInput resolveResource(String type, String namespaceURI, String publicId,
                                           String systemId, String baseURI) {
                return resolverMap.get(namespaceURI);
            }
        });

        schemaLoader.loadInputList(new ListLSInput(inputs));
    }
}
