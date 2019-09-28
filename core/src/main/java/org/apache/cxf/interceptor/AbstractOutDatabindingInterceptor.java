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

package org.apache.cxf.interceptor;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import javax.xml.validation.Schema;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.helpers.ServiceUtils;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.CachingXmlEventWriter;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;

public abstract class AbstractOutDatabindingInterceptor extends AbstractPhaseInterceptor<Message> {

    public static final String DISABLE_OUTPUTSTREAM_OPTIMIZATION = "disable.outputstream.optimization";
    public static final String OUT_BUFFERING = "org.apache.cxf.output.buffering";

    public AbstractOutDatabindingInterceptor(String phase) {
        super(phase);
    }
    public AbstractOutDatabindingInterceptor(String id, String phase) {
        super(id, phase);
    }

    protected boolean shouldBuffer(Message message) {
        Object en = message.getContextualProperty(OUT_BUFFERING);
        boolean allowBuffer = true;
        boolean buffer = false;
        if (en != null) {
            buffer = Boolean.TRUE.equals(en) || "true".equals(en);
            allowBuffer = !(Boolean.FALSE.equals(en) || "false".equals(en));
        }
        // need to cache the events in case validation fails or buffering is enabled
        return buffer || (allowBuffer && shouldValidate(message) && !isRequestor(message));
    }

    protected void writeParts(Message message, Exchange exchange,
                              BindingOperationInfo operation, MessageContentsList objs,
                              List<MessagePartInfo> parts) {
        OutputStream out = message.getContent(OutputStream.class);
        XMLStreamWriter origXmlWriter = message.getContent(XMLStreamWriter.class);
        Service service = exchange.getService();
        XMLStreamWriter xmlWriter = origXmlWriter;
        CachingXmlEventWriter cache = null;

        // configure endpoint and operation level schema validation
        setOperationSchemaValidation(message);

        // need to cache the events in case validation fails or buffering is enabled
        if (shouldBuffer(message)) {
            if (!(xmlWriter instanceof CachingXmlEventWriter)) {
                cache = new CachingXmlEventWriter();
                try {
                    cache.setNamespaceContext(origXmlWriter.getNamespaceContext());
                } catch (XMLStreamException e) {
                    //ignorable, will just get extra namespace decls
                }
                xmlWriter = cache;
            }
            out = null;
        }

        if (out != null
            && writeToOutputStream(message, operation.getBinding(), service)
            && !MessageUtils.getContextualBoolean(message, DISABLE_OUTPUTSTREAM_OPTIMIZATION, false)) {
            if (xmlWriter != null) {
                try {
                    xmlWriter.writeCharacters("");
                    xmlWriter.flush();
                } catch (XMLStreamException e) {
                    throw new Fault(e);
                }
            }

            DataWriter<OutputStream> osWriter = getDataWriter(message, service, OutputStream.class);

            for (MessagePartInfo part : parts) {
                if (objs.hasValue(part)) {
                    Object o = objs.get(part);
                    osWriter.write(o, part, out);
                }
            }
        } else {
            DataWriter<XMLStreamWriter> dataWriter = getDataWriter(message, service, XMLStreamWriter.class);

            for (MessagePartInfo part : parts) {
                if (objs.hasValue(part)) {
                    NamespaceContext c = null;
                    if (!part.isElement()
                        && xmlWriter instanceof CachingXmlEventWriter) {
                        try {
                            c = xmlWriter.getNamespaceContext();
                            xmlWriter.setNamespaceContext(new CachingXmlEventWriter.NSContext(null));
                        } catch (XMLStreamException e) {
                            //ignore
                        }
                    }
                    Object o = objs.get(part);
                    dataWriter.write(o, part, xmlWriter);
                    if (c != null) {
                        try {
                            xmlWriter.setNamespaceContext(c);
                        } catch (XMLStreamException e) {
                            //ignore
                        }
                    }
                }
            }
        }
        if (cache != null) {
            try {
                for (XMLEvent event : cache.getEvents()) {
                    StaxUtils.writeEvent(event, origXmlWriter);
                }
            } catch (XMLStreamException e) {
                throw new Fault(e);
            }
        }
    }

    protected void setOperationSchemaValidation(Message message) {
        SchemaValidationType validationType = ServiceUtils.getSchemaValidationType(message);
        message.put(Message.SCHEMA_VALIDATION_ENABLED, validationType);
    }

    protected boolean shouldValidate(Message m) {
        return ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, m);
    }

    protected boolean writeToOutputStream(Message m, BindingInfo info, Service s) {
        /**
         * Yes, all this code is EXTREMELY ugly. But it gives about a 60-70% performance
         * boost with the JAXB RI, so its worth it.
         */

        if (s == null) {
            return false;
        }

        String enc = (String)m.get(Message.ENCODING);
        return "org.apache.cxf.binding.soap.model.SoapBindingInfo".equals(info.getClass().getName())
            && "org.apache.cxf.jaxb.JAXBDataBinding".equals(s.getDataBinding().getClass().getName())
            && !MessageUtils.isDOMPresent(m)
            && (enc == null || StandardCharsets.UTF_8.name().equals(enc));
    }

    protected <T> DataWriter<T> getDataWriter(Message message, Service service, Class<T> output) {
        DataWriter<T> writer = service.getDataBinding().createWriter(output);

        Collection<Attachment> atts = message.getAttachments();
        if (MessageUtils.getContextualBoolean(message, Message.MTOM_ENABLED, false)
              && atts == null) {
            atts = new ArrayList<>();
            message.setAttachments(atts);
        }

        writer.setAttachments(atts);
        writer.setProperty(DataWriter.ENDPOINT, message.getExchange().getEndpoint());
        writer.setProperty(Message.class.getName(), message);

        setDataWriterValidation(service, message, writer);
        return writer;
    }

    /**
     * Based on the Schema Validation configuration, will initialise the DataWriter with or without the schema set.
     */
    private void setDataWriterValidation(Service service, Message message, DataWriter<?> writer) {
        if (shouldValidate(message)) {
            Schema schema = EndpointReferenceUtils.getSchema(service.getServiceInfos().get(0),
                                                             message.getExchange().getBus());
            writer.setSchema(schema);
        }
    }

    protected XMLStreamWriter getXMLStreamWriter(Message message) {
        return message.getContent(XMLStreamWriter.class);
    }
}
