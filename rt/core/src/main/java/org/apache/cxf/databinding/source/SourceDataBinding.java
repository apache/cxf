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


import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;

import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceModelVisitor;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceInfo;

/**
 * A simple databinding implementation which reads and writes Source objects.
 */
public class SourceDataBinding extends org.apache.cxf.databinding.AbstractDataBinding {
    
    public static final String PREFERRED_FORMAT = "source-preferred-format";

    public SourceDataBinding() {
        super();
    }

    public void initialize(Service service) {
        for (ServiceInfo serviceInfo : service.getServiceInfos()) {
            SchemaCollection schemaCollection = serviceInfo.getXmlSchemaCollection();
            if (schemaCollection.getXmlSchemas().length > 1) {
                // Schemas are already populated.
                continue;
            }
            new ServiceModelVisitor(serviceInfo) {
                public void begin(MessagePartInfo part) {
                    if (part.getTypeQName() != null || part.getElementQName() != null) {
                        return;
                    }
                    part.setTypeQName(XmlSchemaConstants.ANY_TYPE_QNAME);
                }
            } .walk();
        }
    }
    

    @SuppressWarnings("unchecked")
    public <T> DataReader<T> createReader(Class<T> cls) {
        if (cls == XMLStreamReader.class) {
            return (DataReader<T>) new XMLStreamDataReader();
        } else if (cls == Node.class) {
            return (DataReader<T>) new NodeDataReader();
        } else {
            throw new UnsupportedOperationException("The type " + cls.getName() + " is not supported.");
        }
    }

    public Class<?>[] getSupportedReaderFormats() {
        return new Class[] {XMLStreamReader.class, Node.class};
    }

    @SuppressWarnings("unchecked")
    public <T> DataWriter<T> createWriter(Class<T> cls) {
        if (cls == XMLStreamWriter.class) {
            return (DataWriter<T>) new XMLStreamDataWriter();
        } else if (cls == Node.class) {
            return (DataWriter<T>) new NodeDataWriter();
        } else {
            throw new UnsupportedOperationException("The type " + cls.getName() + " is not supported.");
        }
    }

    public Class<?>[] getSupportedWriterFormats() {
        return new Class[] {XMLStreamWriter.class, Node.class};
    }

}
