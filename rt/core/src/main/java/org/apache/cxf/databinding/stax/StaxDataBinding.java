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

package org.apache.cxf.databinding.stax;

import java.util.Collection;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;

import org.w3c.dom.Node;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.AbstractDataBinding;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * A simple databinding implementation which reads and writes Source objects.
 * This will not work with the standard databinding interceptors.
 */
public class StaxDataBinding extends AbstractDataBinding {

    private XMLStreamDataReader xsrReader;
    private XMLStreamDataWriter xswWriter;

    public StaxDataBinding() {
        super();
        this.xsrReader = new XMLStreamDataReader();
        this.xswWriter = new XMLStreamDataWriter();
    }

    public void initialize(Service service) {
        // do nothing
    }
    
    @SuppressWarnings("unchecked")
    public <T> DataReader<T> createReader(Class<T> cls) {
        if (cls == XMLStreamReader.class) {
            return (DataReader<T>) xsrReader;
        } else {
            throw new UnsupportedOperationException("The type " + cls.getName() + " is not supported.");
        }
    }

    public Class<?>[] getSupportedReaderFormats() {
        return new Class[] {XMLStreamReader.class};
    }

    @SuppressWarnings("unchecked")
    public <T> DataWriter<T> createWriter(Class<T> cls) {
        if (cls == XMLStreamWriter.class) {
            return (DataWriter<T>) xswWriter;
        } else {
            throw new UnsupportedOperationException("The type " + cls.getName() + " is not supported.");
        }
    }

    public Class<?>[] getSupportedWriterFormats() {
        return new Class[] {XMLStreamWriter.class, Node.class};
    }

    public static class XMLStreamDataReader implements DataReader<XMLStreamReader> {

        public Object read(MessagePartInfo part, XMLStreamReader input) {
            return read(null, input, part.getTypeClass());
        }

        public Object read(QName name, XMLStreamReader input, Class type) {
            return input;
        }

        public Object read(XMLStreamReader reader) {
            return reader;
        }
        
        public void setSchema(Schema s) {
        }

        public void setAttachments(Collection<Attachment> attachments) {
        }

        public void setProperty(String prop, Object value) {   
        }
    }

    public static class XMLStreamDataWriter implements DataWriter<XMLStreamWriter> {
        private static final Logger LOG = LogUtils.getL7dLogger(XMLStreamDataWriter.class);

        public void write(Object obj, MessagePartInfo part, XMLStreamWriter output) {
            write(obj, output);
        }

        public void write(Object obj, XMLStreamWriter writer) {
            try {
                if (obj instanceof XMLStreamReader) {
                    XMLStreamReader xmlStreamReader = (XMLStreamReader) obj;
                    StaxUtils.copy(xmlStreamReader, writer);
                    xmlStreamReader.close();
                } else if (obj instanceof XMLStreamWriterCallback) {
                    ((XMLStreamWriterCallback) obj).write(writer);
                } else {
                    throw new UnsupportedOperationException("Data types of " 
                                                            + obj.getClass() + " are not supported.");
                }
            } catch (XMLStreamException e) {
                throw new Fault(new Message("COULD_NOT_READ_XML_STREAM", LOG), e);
            }
        }

        public void setSchema(Schema s) {
        }

        public void setAttachments(Collection<Attachment> attachments) {
        }

        public void setProperty(String key, Object value) {
        }
        
    }
}
