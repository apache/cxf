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

import java.util.Collection;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

public class NodeDataWriter implements DataWriter<Node> {
    private static final Logger LOG = LogUtils.getL7dLogger(NodeDataWriter.class);

    public void write(Object obj, MessagePartInfo part, Node output) {
        write(obj, output);
    }

    public void write(Object obj, Node n) {
        try {
            Source s = (Source) obj;
            if (s instanceof DOMSource
                    && ((DOMSource) s).getNode() == null) {
                return;
            }
            
            XMLStreamWriter writer = new W3CDOMStreamWriter((Element)n);
            StaxUtils.copy(s, writer);
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
