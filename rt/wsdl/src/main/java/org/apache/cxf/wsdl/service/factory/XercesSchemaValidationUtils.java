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

package org.apache.cxf.wsdl.service.factory;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerException;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaSerializer;

/**
 *
 */
class XercesSchemaValidationUtils {


    static class DOMLSInput implements LSInput {
        private String systemId;
        private String data;

        DOMLSInput(Document doc, String systemId) throws TransformerException {
            this.systemId = systemId;
            data = StaxUtils.toString(doc);
        }

        /** {@inheritDoc}*/
        public String getBaseURI() {
            return null;
        }

        /** {@inheritDoc}*/
        public InputStream getByteStream() {
            return null;
        }

        /** {@inheritDoc}*/
        public boolean getCertifiedText() {
            return false;
        }

        /** {@inheritDoc}*/
        public Reader getCharacterStream() {
            return null;
        }

        /** {@inheritDoc}*/
        public String getEncoding() {
            return "utf-8";
        }

        /** {@inheritDoc}*/
        public String getPublicId() {
            return null;
        }

        /** {@inheritDoc}*/
        public String getStringData() {
            return data;
        }

        /** {@inheritDoc}*/
        public String getSystemId() {
            return systemId;
        }

        /** {@inheritDoc}*/
        public void setBaseURI(String baseURI) {
        }

        /** {@inheritDoc}*/
        public void setByteStream(InputStream byteStream) {
        }

        /** {@inheritDoc}*/
        public void setCertifiedText(boolean certifiedText) {
        }

        /** {@inheritDoc}*/
        public void setCharacterStream(Reader characterStream) {
        }

        /** {@inheritDoc}*/
        public void setEncoding(String encoding) {
        }

        /** {@inheritDoc}*/
        public void setPublicId(String publicId) {
        }

        /** {@inheritDoc}*/
        public void setStringData(String stringData) {
        }

        /** {@inheritDoc}*/
        public void setSystemId(String systemId) {
        }
    }


    private DOMImplementation impl;

    XercesSchemaValidationUtils() throws ClassNotFoundException, InstantiationException,
        IllegalAccessException, ClassCastException {

        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        impl = registry.getDOMImplementation("XS-Loader");
    }


    Method findMethod(Object o, String name) {
        for (Method m : o.getClass().getMethods()) {
            if (m.getName() != null && m.getName().equals(name)) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }
    void tryToParseSchemas(XmlSchemaCollection collection, DOMErrorHandler handler)
        throws Exception {

        final List<DOMLSInput> inputs = new ArrayList<>();
        final Map<String, LSInput> resolverMap = new HashMap<>();

        for (XmlSchema schema : collection.getXmlSchemas()) {
            if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(schema.getTargetNamespace())) {
                continue;
            }
            Document document = new XmlSchemaSerializer().serializeSchema(schema, false)[0];
            DOMLSInput input = new DOMLSInput(document, schema.getTargetNamespace());
            resolverMap.put(schema.getTargetNamespace(), input);
            inputs.add(input);
        }

        try {

            Object schemaLoader = findMethod(impl, "createXSLoader").invoke(impl, new Object[1]);
            DOMConfiguration config = (DOMConfiguration)findMethod(schemaLoader, "getConfig").invoke(schemaLoader);

            config.setParameter("validate", Boolean.TRUE);
            config.setParameter("error-handler", handler);
            config.setParameter("resource-resolver", new LSResourceResolver() {
                public LSInput resolveResource(String type, String namespaceURI, String publicId,
                                               String systemId, String baseURI) {
                    return resolverMap.get(namespaceURI);
                }
            });


            Method m = findMethod(schemaLoader, "loadInputList");
            String name = m.getParameterTypes()[0].getName() + "Impl";
            name = name.replace("xs.LS", "impl.xs.util.LS");
            Class<?> c = Class.forName(name);
            Object inputList = c.getConstructor(LSInput[].class, Integer.TYPE)
                .newInstance(inputs.toArray(new LSInput[0]), inputs.size());
            m.invoke(schemaLoader, inputList);
        } catch (InvocationTargetException e) {
            throw (Exception)e.getTargetException();
        }
    }
}
