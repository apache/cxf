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

package org.apache.cxf.jibx;

import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallable;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.JiBXException;
import org.jibx.runtime.impl.StAXWriter;

public class JibxDataWriter implements DataWriter<XMLStreamWriter> {

    public void write(Object obj, XMLStreamWriter output) {
    }

    public void write(Object obj, MessagePartInfo part, XMLStreamWriter output) {
        Class<?> jtype = part.getTypeClass();
        QName stype = part.getTypeQName();
        if (JibxSimpleTypes.isSimpleType(jtype)) {
            try {
                String pfx = output.getPrefix(part.getConcreteName().getNamespaceURI());
                if (StringUtils.isEmpty(pfx)) {
                    output.writeStartElement("tns", part.getConcreteName().getLocalPart(), part
                        .getConcreteName().getNamespaceURI());
                    output.writeNamespace("tns", part.getConcreteName().getNamespaceURI());
                } else {
                    output.writeStartElement(pfx, part.getConcreteName().getLocalPart(), part
                        .getConcreteName().getNamespaceURI());
                }
                output.writeCharacters(JibxSimpleTypes.toText(stype, obj));
                output.writeEndElement();
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                IBindingFactory factory = BindingDirectory.getFactory(obj.getClass());
                IMarshallingContext ctx = getMarshallingContext(obj);
                StAXWriter writer = new StAXWriter(factory.getNamespaces(), output);
                ctx.setXmlWriter(writer);
                ((IMarshallable)obj).marshal(ctx);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setAttachments(Collection<Attachment> attachments) {
    }

    public void setProperty(String key, Object value) {
    }

    public void setSchema(Schema s) {
    }

    private IMarshallingContext getMarshallingContext(Object object) throws JiBXException {
        IBindingFactory factory = BindingDirectory.getFactory(object.getClass());
        return factory.createMarshallingContext();
    }
}
