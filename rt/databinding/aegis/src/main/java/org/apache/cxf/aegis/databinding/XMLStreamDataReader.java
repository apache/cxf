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
package org.apache.cxf.aegis.databinding;

import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.validation.Schema;

import org.apache.cxf.Bus;
import org.apache.cxf.aegis.AegisXMLStreamDataReader;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;

public class XMLStreamDataReader implements DataReader<XMLStreamReader> {

    private AegisDatabinding databinding;
    private AegisXMLStreamDataReader reader;
    
    public XMLStreamDataReader(AegisDatabinding databinding, Bus bus) {
        this.databinding = databinding;
        reader = new AegisXMLStreamDataReader(databinding.getAegisContext());
    }

    public Object read(MessagePartInfo part, XMLStreamReader input) {
        Type type = databinding.getType(part);
        try {
            return reader.read(input, type); 
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    public Object read(QName name, XMLStreamReader input, Class typeClass) {
        MessagePartInfo info = databinding.getPartFromClass(typeClass);
        return info == null ? read(input) : read(info, input);
    }

    public Object read(XMLStreamReader input) {
        try {
            return reader.read(input, null);
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    public void setAttachments(Collection<Attachment> attachments) {
        reader.getContext().setAttachments(attachments);
    }

    public void setProperty(String prop, Object value) {
        reader.setProperty(prop, value);
    }

    public void setSchema(Schema s) {
        reader.setSchema(s);
    }
}
