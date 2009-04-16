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

import javax.activation.DataSource;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

public class XMLStreamDataWriter implements DataWriter<XMLStreamWriter> {
    private static final Logger LOG = LogUtils.getL7dLogger(XMLStreamDataWriter.class);

    public void write(Object obj, MessagePartInfo part, XMLStreamWriter output) {
        write(obj, output);
    }

    public void write(Object obj, XMLStreamWriter writer) {
        try {
            XMLStreamReader reader = null;
            if (obj instanceof DataSource) {
                DataSource ds = (DataSource)obj;
                reader = StaxUtils.createXMLStreamReader(ds.getInputStream());
                StaxUtils.copy(reader, writer);
                reader.close();
            } else if (obj instanceof Node) {
                Node nd = (Node)obj;
                if (writer instanceof W3CDOMStreamWriter
                    && ((W3CDOMStreamWriter)writer).getCurrentNode() != null) {
                    W3CDOMStreamWriter dw = (W3CDOMStreamWriter)writer;
                    
                    if (nd.getOwnerDocument() == dw.getDocument()) {
                        dw.getCurrentNode().appendChild(nd);
                        return;
                    } else if (nd instanceof DocumentFragment) {
                        nd = dw.getDocument().importNode(nd, true);
                        dw.getCurrentNode().appendChild(nd);
                        return;
                    }
                }
                StaxUtils.writeNode(nd, writer, true);
            } else {
                Source s = (Source) obj;
                if (s instanceof DOMSource
                    && ((DOMSource) s).getNode() == null) {
                    return;
                }
                StaxUtils.copy(s, writer);
            }
        } catch (XMLStreamException e) {
            throw new Fault(new Message("COULD_NOT_READ_XML_STREAM", LOG), e);
        } catch (IOException e) {
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
