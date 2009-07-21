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

import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;


import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;

import commonj.sdo.DataObject;
import commonj.sdo.helper.HelperContext;

public class DataWriterImpl implements DataWriter<XMLStreamWriter> {
    HelperContext context;
    Object xmlStreamHelper;
    
    public DataWriterImpl(HelperContext context) {
        this.context = context;
        try {
            xmlStreamHelper = context.getClass().getMethod("getXMLStreamHelper", 
                                                           new Class[0]).invoke(context);
        } catch (Throwable t) {
            xmlStreamHelper = null;
        }
    }
    
    public void write(Object obj, XMLStreamWriter output) {
        write(obj, null, output);
    }
    
    public void write(Object obj, MessagePartInfo part, XMLStreamWriter output) {
        try {
            xmlStreamHelper.getClass().getMethod("saveObject", 
                                                        new Class[] {DataObject.class,
                                                                     XMLStreamWriter.class})
                                                            .invoke(xmlStreamHelper, obj, output);
        } catch (Exception ex) {
            throw new Fault(ex);
        }

    }


    public void setAttachments(Collection<Attachment> attachments) {
    }

    public void setProperty(String key, Object value) {
    }

    public void setSchema(Schema schema) {
    }
}
