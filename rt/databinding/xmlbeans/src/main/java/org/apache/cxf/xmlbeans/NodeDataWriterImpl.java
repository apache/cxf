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

package org.apache.cxf.xmlbeans;


import java.util.Collection;
import javax.xml.validation.Schema;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

public class NodeDataWriterImpl implements DataWriter<Node> {
    DataWriterImpl writer;
    
    public NodeDataWriterImpl() {
        writer = new DataWriterImpl();
    }
    
    public void write(Object obj, Node output) {
        write(obj, null, output);
    }
    
    public void write(Object obj, MessagePartInfo part, Node output) {
        W3CDOMStreamWriter domWriter = new W3CDOMStreamWriter((Element)output);
        writer.write(obj, part, domWriter);
    }

    public void setAttachments(Collection<Attachment> attachments) {
        writer.setAttachments(attachments);
    }

    public void setProperty(String key, Object value) {
        writer.setProperty(key, value);
    }

    public void setSchema(Schema s) {
        writer.setSchema(s);
    }
}
