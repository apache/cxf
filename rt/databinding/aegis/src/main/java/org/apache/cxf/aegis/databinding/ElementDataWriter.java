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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.validation.Schema;

import org.w3c.dom.Element;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.aegis.xml.stax.ElementWriter;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

public class ElementDataWriter implements DataWriter<Element> {

    private static final Logger LOG = LogUtils.getL7dLogger(XMLStreamDataReader.class);

    private AegisDatabinding databinding;

    private Collection<Attachment> attachments;

    private Map<String, Object> properties;

    public ElementDataWriter(AegisDatabinding databinding) {
        this.databinding = databinding;
    }

    public void setAttachments(Collection<Attachment> attachments) {
        this.attachments = attachments;
    }

    public void setSchema(Schema s) {
    }

    public void write(Object obj, MessagePartInfo part, Element output) {
        AegisType type = databinding.getType(part);

        if (type == null) {
            throw new Fault(new Message("NO_MESSAGE_FOR_PART", LOG));
        }


        Context context = new Context(databinding.getAegisContext());

        context.setAttachments(attachments);
        type = TypeUtil.getWriteType(databinding.getAegisContext(), obj, type);
        try {
            W3CDOMStreamWriter domWriter = new W3CDOMStreamWriter(output);
            ElementWriter writer = new ElementWriter(domWriter);
            MessageWriter w2 = writer.getElementWriter(part.getConcreteName());
            if (type.isNillable() && type.isWriteOuter() && obj == null) {
                w2.writeXsiNil();
                w2.close();
                return;
            }

            type.writeObject(obj, w2, context);
            w2.close();
        } catch (DatabindingException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(Object obj, Element output) {
        write(obj, null, output);
    }


    public void setProperty(String prop, Object value) {
        if (properties == null) {
            properties = new HashMap<>();
        }

        properties.put(prop, value);
    }

    public Object getProperty(String key) {
        if (properties == null) {
            return null;
        }
        return properties.get(key);
    }
}
