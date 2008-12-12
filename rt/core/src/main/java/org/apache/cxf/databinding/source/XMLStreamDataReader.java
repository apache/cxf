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
package org.apache.cxf.databinding.source;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxUtils;



public class XMLStreamDataReader implements DataReader<XMLStreamReader> {
    private static final Logger LOG = LogUtils.getL7dLogger(XMLStreamDataReader.class);

    public Object read(MessagePartInfo part, XMLStreamReader input) {
        return read(null, input, part.getTypeClass());
    }

    public Object read(QName name, XMLStreamReader input, Class type) {
        if (type != null) {
            if (SAXSource.class.isAssignableFrom(type)) {
                try {
                    CachedOutputStream out = new CachedOutputStream();
                    try {
                        XMLStreamWriter xsw = StaxUtils.createXMLStreamWriter(out);
                        StaxUtils.copy(input, xsw);
                        xsw.close();
                        return new SAXSource(new InputSource(out.getInputStream()));
                    } finally {
                        out.close();
                    }
                } catch (IOException e) {
                    throw new Fault(new Message("COULD_NOT_READ_XML_STREAM", LOG), e);
                } catch (XMLStreamException e) {
                    throw new Fault(new Message("COULD_NOT_READ_XML_STREAM", LOG), e);
                }
            } else if (StreamSource.class.isAssignableFrom(type)) {
                try {
                    CachedOutputStream out = new CachedOutputStream();
                    try {
                        XMLStreamWriter xsw = StaxUtils.createXMLStreamWriter(out);
                        StaxUtils.copy(input, xsw);
                        xsw.close();
                        return new StreamSource(out.getInputStream());
                    } finally {
                        out.close();
                    }
                } catch (IOException e) {
                    throw new Fault(new Message("COULD_NOT_READ_XML_STREAM", LOG), e);
                } catch (XMLStreamException e) {
                    throw new Fault(new Message("COULD_NOT_READ_XML_STREAM", LOG), e);
                }
            } 
        }
        return read(input);
    }

    public Object read(XMLStreamReader reader) {
        // Use a DOMSource for now, we should really use a StaxSource/SAXSource though for 
        // performance reasons
        try {
            Document document = StaxUtils.read(reader);
            return new DOMSource(document);
        } catch (XMLStreamException e) {
            throw new Fault(new Message("COULD_NOT_READ_XML_STREAM", LOG), e);
        }
    }
    
    public void setSchema(Schema s) {
    }

    public void setAttachments(Collection<Attachment> attachments) {
    }

    public void setProperty(String prop, Object value) {   
    }
}
