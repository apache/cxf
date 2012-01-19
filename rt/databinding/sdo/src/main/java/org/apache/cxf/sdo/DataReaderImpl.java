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

package org.apache.cxf.sdo;

import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.validation.Schema;


import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxUtils;

import commonj.sdo.helper.HelperContext;

public class DataReaderImpl implements DataReader<XMLStreamReader> {
    HelperContext context;
    Object xmlStreamHelper;
    
    public DataReaderImpl(HelperContext context) {
        this.context = context;
        try {
            xmlStreamHelper = context.getClass().getMethod("getXMLStreamHelper", 
                                                           new Class[0]).invoke(context);
        } catch (Throwable t) {
            xmlStreamHelper = null;
        }
    }

    public Object read(XMLStreamReader input) {
        return read(null, input);
    }

    public Object read(MessagePartInfo part, XMLStreamReader reader) {
        if (xmlStreamHelper != null) {
            try {
                if (reader.getEventType() == XMLStreamReader.START_DOCUMENT) {
                    StaxUtils.toNextTag(reader);
                }
                Object o = xmlStreamHelper.getClass().getMethod("loadObject", 
                                                     new Class[] {XMLStreamReader.class})
                                                         .invoke(xmlStreamHelper, reader);
                
                return o;
            } catch (Exception e) {
                new Fault(e);
            }
        }
        return null;
    }

    public Object read(QName name, XMLStreamReader input, Class type) {        
        return read(null, input);
    }

    
    public void setAttachments(Collection<Attachment> attachments) {
    }

    public void setProperty(String prop, Object value) {
    }

    public void setSchema(Schema s) {
    }
}
