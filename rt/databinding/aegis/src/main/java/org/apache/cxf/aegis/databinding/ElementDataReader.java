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
import javax.xml.validation.Schema;

import org.w3c.dom.Element;

import org.apache.cxf.aegis.AegisElementDataReader;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;

/**
 * 
 */
public class ElementDataReader implements DataReader<Element> {
    
    private AegisElementDataReader reader;
    private AegisDatabinding databinding;
    
    ElementDataReader(AegisDatabinding binding) {
        databinding = binding;
        reader = new AegisElementDataReader(binding.getAegisContext()); 
    }

    /**
     * {@inheritDoc}
     */
    public Object read(Element input) {
        try {
            return reader.read(input);
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    /** {@inheritDoc}*/
    public Object read(MessagePartInfo part, Element input) {
        try {
            Type type = databinding.getType(part);
            return reader.read(input, type);
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    /** {@inheritDoc}*/
    public Object read(QName name, Element input, Class typeClass) {
        try {
            // TODO: pay attention to the typeClass parameter.
            return reader.read(input);
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    /** 
     * {@inheritDoc}
     * */
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
