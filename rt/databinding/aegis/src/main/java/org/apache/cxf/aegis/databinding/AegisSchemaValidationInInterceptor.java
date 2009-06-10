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

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.binding.soap.interceptor.StartBodyInterceptor;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.StaxValidationManager;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.ServiceInfo;

public class AegisSchemaValidationInInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(AegisSchemaValidationInInterceptor.class);
    
    private ServiceInfo service;
    private Bus bus;
    
    public AegisSchemaValidationInInterceptor(Bus bus, ServiceInfo service) {
        super(Phase.READ);
        this.bus = bus;
        this.service = service;
        addBefore(StartBodyInterceptor.class.getName());
        addAfter(ReadHeadersInterceptor.class.getName());
    }


    public void handleMessage(SoapMessage message) throws Fault {
        XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
        try {
            setSchemaInMessage(message, xmlReader);
        } catch (XMLStreamException e) {
            throw new SoapFault(new Message("XML_STREAM_EXC", LOG), 
                                e, message.getVersion().getSender());
        }
    }
    
    private void setSchemaInMessage(SoapMessage message, XMLStreamReader reader) throws XMLStreamException  {
        Object en = message.getContextualProperty(org.apache.cxf.message.Message.SCHEMA_VALIDATION_ENABLED);
        if (Boolean.TRUE.equals(en) || "true".equals(en)) {
            StaxValidationManager mgr = bus.getExtension(StaxValidationManager.class);
            if (mgr != null) {
                mgr.setupValidation(reader, service);
            }
        }
    }

}
