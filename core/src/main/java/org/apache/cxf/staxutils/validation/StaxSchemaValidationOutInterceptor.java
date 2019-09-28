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

package org.apache.cxf.staxutils.validation;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.ServiceUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class StaxSchemaValidationOutInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LogUtils.getL7dLogger(StaxSchemaValidationOutInterceptor.class);

    public StaxSchemaValidationOutInterceptor() {
        super(Phase.PRE_MARSHAL);
    }


    public void handleMessage(Message message) {
        XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
        try {
            setSchemaInMessage(message, writer);
        } catch (XMLStreamException e) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("SCHEMA_ERROR", LOG),
                            e);
        }
    }

    private void setSchemaInMessage(Message message, XMLStreamWriter writer) throws XMLStreamException  {
        if (ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, message)) {
            try {
                WoodstoxValidationImpl mgr = new WoodstoxValidationImpl();
                if (mgr.canValidate()) {
                    mgr.setupValidation(writer, message.getExchange().getEndpoint(),
                                        message.getExchange().getService().getServiceInfos().get(0));
                }
            } catch (Throwable t) {
                //likely no MSV or similar
                LOG.log(Level.FINE, "Problem initializing MSV validation", t);
            }
        }
    }
}
