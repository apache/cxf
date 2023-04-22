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

package org.apache.cxf.staxutils.validation;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.InputSource;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaExternal;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.validation.ValidationProblemHandler;
import org.codehaus.stax2.validation.XMLValidationException;
import org.codehaus.stax2.validation.XMLValidationProblem;
import org.codehaus.stax2.validation.XMLValidationSchema;

/**
 * This class touches stax2 API, so it is kept separate to allow graceful fallback.
 */
class Stax2ValidationUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(Stax2ValidationUtils.class);
    private static final String KEY = XMLValidationSchema.class.getName();

    private static final boolean HAS_WOODSTOX_5;
    private static final boolean HAS_WOODSTOX_6_2;

    private final Class<?> multiSchemaFactory;

    static {
        boolean hasWoodstox5 = false;
        boolean hasWoodstox62 = false;
        try {
            // Check to see if we have a version of Woodstox < 6 with MSV
            new W3CMultiSchemaFactory();
            hasWoodstox5 = true;
        } catch (Throwable t) {
            // Otherwise delegate to Woodstox directly if W3CMultiSchemaFactory exists there
            try {
                Class<?> multiSchemaFactory =
                        ClassLoaderUtils.loadClass("com.ctc.wstx.msv.W3CMultiSchemaFactory",
                                Stax2ValidationUtils.class);
                if (multiSchemaFactory != null) {
                    hasWoodstox62 = true;
                }
            } catch (Throwable t2) {
                // ignore
            }
        }
        HAS_WOODSTOX_5 = hasWoodstox5;
        HAS_WOODSTOX_6_2 = hasWoodstox62;
    }

    Stax2ValidationUtils() throws ClassNotFoundException {
        if (!(HAS_WOODSTOX_5 || HAS_WOODSTOX_6_2)) {
            throw new RuntimeException("Could not load woodstox");
        }

        String className = "com.ctc.wstx.msv.W3CMultiSchemaFactory";
        if (HAS_WOODSTOX_5) {
            className = "org.apache.cxf.staxutils.validation.W3CMultiSchemaFactory";
        }
        multiSchemaFactory = ClassLoaderUtils.loadClass(className, this.getClass());
    }

    /**
     * {@inheritDoc}
     *
     * @throws XMLStreamException
     */
    public boolean setupValidation(XMLStreamReader reader, Endpoint endpoint, ServiceInfo serviceInfo)
            throws XMLStreamException {

        // Gosh, this is bad, but I don't know a better solution, unless we're willing
        // to require the stax2 API no matter what.
        XMLStreamReader effectiveReader = reader;
        if (effectiveReader instanceof DepthXMLStreamReader) {
            effectiveReader = ((DepthXMLStreamReader) reader).getReader();
        }
        final XMLStreamReader2 reader2 = (XMLStreamReader2) effectiveReader;
        XMLValidationSchema vs = getValidator(endpoint, serviceInfo);
        if (vs == null) {
            return false;
        }
        reader2.setValidationProblemHandler(new ValidationProblemHandler() {

            public void reportProblem(XMLValidationProblem problem) throws XMLValidationException {
                throw new Fault(new Message("READ_VALIDATION_ERROR", LOG, problem.getMessage()),
                        Fault.FAULT_CODE_CLIENT);
            }
        });
        reader2.validateAgainst(vs);
        return true;
    }

    public boolean setupValidation(XMLStreamWriter writer, Endpoint endpoint, ServiceInfo serviceInfo)
            throws XMLStreamException {

        XMLStreamWriter2 writer2 = (XMLStreamWriter2) writer;
        XMLValidationSchema vs = getValidator(endpoint, serviceInfo);
        if (vs == null) {
            return false;
        }
        writer2.setValidationProblemHandler(new ValidationProblemHandler() {

            public void reportProblem(XMLValidationProblem problem) throws XMLValidationException {
                throw new Fault(problem.getMessage(), LOG);
            }
        });
        writer2.validateAgainst(vs);
        return true;
    }

    /**
     * Create woodstox validator for a schema set.
     *
     * @throws XMLStreamException
     */
    private XMLValidationSchema getValidator(Endpoint endpoint, ServiceInfo serviceInfo)
            throws XMLStreamException {
        synchronized (endpoint) {
            XMLValidationSchema ret = (XMLValidationSchema) endpoint.get(KEY);
            if (ret == null) {
                if (endpoint.containsKey(KEY)) {
                    return null;
                }
                Map<String, Source> sources = new TreeMap<>();

                for (SchemaInfo schemaInfo : serviceInfo.getSchemas()) {
                    XmlSchema sch = schemaInfo.getSchema();
                    String uri = sch.getTargetNamespace();
                    if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(uri)) {
                        continue;
                    }

                    if (sch.getTargetNamespace() == null && !sch.getExternals().isEmpty()) {
                        for (XmlSchemaExternal xmlSchemaExternal : sch.getExternals()) {
                            addSchema(sources, xmlSchemaExternal.getSchema(),
                                    getElement(xmlSchemaExternal.getSchema().getSourceURI()));
                        }
                        continue;
                    } else if (sch.getTargetNamespace() == null) {
                        throw new IllegalStateException("An Schema without imports must have a targetNamespace");
                    }

                    addSchema(sources, sch, schemaInfo.getElement());
                }

                try {
                    // I don't think that we need the baseURI.
                    Method method = multiSchemaFactory.getMethod("createSchema", String.class, Map.class);
                    ret = (XMLValidationSchema) method.invoke(multiSchemaFactory.getDeclaredConstructor().newInstance(),
                                                              null, sources);
                    endpoint.put(KEY, ret);
                } catch (Throwable t) {
                    LOG.log(Level.INFO, "Problem loading schemas. Falling back to slower method.", ret);
                    endpoint.put(KEY, null);
                }
            }
            return ret;
        }
    }

    private void addSchema(Map<String, Source> sources, XmlSchema schema, Element element)
            throws XMLStreamException {
        String schemaSystemId = schema.getSourceURI();
        if (null == schemaSystemId) {
            schemaSystemId = schema.getTargetNamespace();
        }
        sources.put(schema.getTargetNamespace(), new DOMSource(element, schemaSystemId));
    }

    private Element getElement(String path) throws XMLStreamException {
        InputSource in = new InputSource(path);
        Document doc = StaxUtils.read(in);
        return doc.getDocumentElement();
    }

}
