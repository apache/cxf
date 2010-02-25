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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import javax.xml.validation.Schema;

import org.apache.cxf.databinding.DataWriter;
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
import org.apache.cxf.wsdl.EndpointReferenceUtils;

public abstract class AbstractOutDatabindingInterceptor extends AbstractPhaseInterceptor<Message> {

    public static final String DISABLE_OUTPUTSTREAM_OPTIMIZATION = "disable.outputstream.optimization";
    public static final String OUT_BUFFERING = "org.apache.cxf.output.buffering";
    
    public AbstractOutDatabindingInterceptor(String phase) {
        super(phase);
    }
    public AbstractOutDatabindingInterceptor(String id, String phase) {
        super(id, phase);
    }
    
    protected boolean isRequestor(Message message) {
        return Boolean.TRUE.equals(message.containsKey(Message.REQUESTOR_ROLE));
    }
    

       
    protected void writeParts(Message message, Exchange exchange, 
                              BindingOperationInfo operation, MessageContentsList objs, 
                              List<MessagePartInfo> parts) {
        OutputStream out = message.getContent(OutputStream.class);
        XMLStreamWriter origXmlWriter = message.getContent(XMLStreamWriter.class);
        Service service = exchange.getService();
        XMLStreamWriter xmlWriter = origXmlWriter;
        CachingXmlEventWriter cache = null;
        
        Object en = message.getContextualProperty(OUT_BUFFERING);
        boolean allowBuffer = true;
        boolean buffer = false;
        if (en != null) {
            buffer = Boolean.TRUE.equals(en) || "true".equals(en);
            allowBuffer = !(Boolean.FALSE.equals(en) || "false".equals(en));
        }
        // need to cache the events in case validation fails or buffering is enabled
        if (buffer || (allowBuffer && shouldValidate(message) && !isRequestor(message))) {
            cache = new CachingXmlEventWriter();
            try {
                cache.setNamespaceContext(origXmlWriter.getNamespaceContext());
            } catch (XMLStreamException e) {
                //ignorable, will just get extra namespace decls
            }
            xmlWriter = cache;
            out = null;
        }
        
        if (out != null 
            && writeToOutputStream(message, operation.getBinding(), service)
            && !MessageUtils.isTrue(message.getContextualProperty(DISABLE_OUTPUTSTREAM_OPTIMIZATION))) {
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
                    Object o = objs.get(part);
                    dataWriter.write(o, part, xmlWriter);
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
    
    
    protected boolean shouldValidate(Message m) {
        Object en = m.getContextualProperty(Message.SCHEMA_VALIDATION_ENABLED);
        return Boolean.TRUE.equals(en) || "true".equals(en);
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
        return info.getClass().getName().equals("org.apache.cxf.binding.soap.model.SoapBindingInfo") 
            && s.getDataBinding().getClass().getName().equals("org.apache.cxf.jaxb.JAXBDataBinding")
            && !MessageUtils.isDOMPresent(m)
            && (enc == null || "UTF-8".equals(enc));
    }
    
    protected <T> DataWriter<T> getDataWriter(Message message, Service service, Class<T> output) {
        DataWriter<T> writer = service.getDataBinding().createWriter(output);
        
        Collection<Attachment> atts = message.getAttachments();
        if (MessageUtils.isTrue(message.getContextualProperty(
              org.apache.cxf.message.Message.MTOM_ENABLED))
              && atts == null) {
            atts = new ArrayList<Attachment>();
            message.setAttachments(atts);
        }
        
        writer.setAttachments(atts);
        writer.setProperty(DataWriter.ENDPOINT, message.getExchange().getEndpoint());
        writer.setProperty(Message.class.getName(), message);
        
        setSchemaOutMessage(service, message, writer);
        return writer;
    }

    private void setSchemaOutMessage(Service service, Message message, DataWriter<?> writer) {
        if (shouldValidate(message)) {
            Schema schema = EndpointReferenceUtils.getSchema(service.getServiceInfos().get(0));
            writer.setSchema(schema);
        }
    }

    protected XMLStreamWriter getXMLStreamWriter(Message message) {
        return message.getContent(XMLStreamWriter.class);
    }
}
