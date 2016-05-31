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

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.ws.commons.schema.XmlSchema;
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

    private static final boolean HAS_WOODSTOX;
    static {
        boolean hasw = false;
        try {
            new ResolvingGrammarReaderController(null, null); // will throw if msv isn't available
            new W3CMultiSchemaFactory(); // will throw if wrong woodstox.
            hasw = true;
        } catch (Throwable t) {
            //ignore
        }
        HAS_WOODSTOX = hasw;
    }
    
    public Stax2ValidationUtils() {
        if (!HAS_WOODSTOX) {
            throw new RuntimeException("Could not load woodstox");
        }
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
            effectiveReader = ((DepthXMLStreamReader)reader).getReader();
        }
        final XMLStreamReader2 reader2 = (XMLStreamReader2)effectiveReader;
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
        
        XMLStreamWriter2 writer2 = (XMLStreamWriter2)writer;
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
     * @param schemas
     * @return
     * @throws XMLStreamException
     */
    private XMLValidationSchema getValidator(Endpoint endpoint, ServiceInfo serviceInfo) throws XMLStreamException {
        synchronized (endpoint) {
            XMLValidationSchema ret = (XMLValidationSchema)endpoint.get(KEY);
            if (ret == null) {
                if (endpoint.containsKey(KEY)) {
                    return null;
                }
                Map<String, EmbeddedSchema> sources = new TreeMap<String, EmbeddedSchema>();
        
                for (SchemaInfo schemaInfo : serviceInfo.getSchemas()) {
                    XmlSchema sch = schemaInfo.getSchema();
                    String uri = sch.getTargetNamespace();
                    if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(uri)) {
                        continue;
                    }
        
                    Element serialized = schemaInfo.getElement();
                    String schemaSystemId = sch.getSourceURI();
                    if (null == schemaSystemId) {
                        schemaSystemId = sch.getTargetNamespace();
                    }
        
                    EmbeddedSchema embeddedSchema = new EmbeddedSchema(schemaSystemId, serialized);
                    sources.put(sch.getTargetNamespace(), embeddedSchema);
                }
        
                W3CMultiSchemaFactory factory = new W3CMultiSchemaFactory();
                // I don't think that we need the baseURI.
                try {
                    ret = factory.loadSchemas(null, sources);
                    endpoint.put(KEY, ret);
                } catch (XMLStreamException ex) {
                    LOG.log(Level.INFO, "Problem loading schemas. Falling back to slower method.", ret);
                    endpoint.put(KEY, null);
                }
            }
            return ret;
        }
    }

}
