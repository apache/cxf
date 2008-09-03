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

package org.apache.cxf.transport.jbi;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;

public class JBIDestinationOutputStream extends CachedOutputStream {

    private static final String SEND_SYNC = "javax.jbi.messaging.sendSync";

    private static final Logger LOG = LogUtils.getL7dLogger(JBIDestinationOutputStream.class);
    private Message inMessage;
    private Message outMessage;
    private DeliveryChannel channel;
    
    public JBIDestinationOutputStream(Message m, 
                               Message outM,
                               DeliveryChannel dc) {
        super();
        inMessage = m;
        outMessage = outM;
        channel = dc;
    }
    
    @Override
    protected void doFlush() throws IOException {
        // so far do nothing
    }

    @Override
    protected void doClose() throws IOException {
        commitOutputMessage();
    }

    @Override
    protected void onWrite() throws IOException {
        // so far do nothing
    }

    private void commitOutputMessage() throws IOException {
        try { 
            if (inMessage.getExchange().isOneWay()) {
                return;
            } else {
                
                InputStream bais = getInputStream();
                LOG.finest(new org.apache.cxf.common.i18n.Message(
                    "BUILDING.DOCUMENT", LOG).toString());
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                docBuilderFactory.setNamespaceAware(true);
                DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
                Document doc = builder.parse(bais);
            
                MessageExchange xchng = inMessage.get(MessageExchange.class);
                LOG.fine(new org.apache.cxf.common.i18n.Message(
                    "CREATE.NORMALIZED.MESSAGE", LOG).toString());
                if (inMessage.getExchange().getOutFaultMessage() != null) {
                    org.apache.cxf.interceptor.Fault f = (org.apache.cxf.interceptor.Fault) 
                            inMessage.getContent(Exception.class);
                    if (f.hasDetails()) {
                        Fault fault = xchng.createFault();
                        fault.setContent(new DOMSource(doc));
                        xchng.setFault(fault);
                    } else {
                        xchng.setError(f);
                    }
                } else {
                    NormalizedMessage msg = xchng.createMessage();
                    //copy attachments
                    if (outMessage != null && outMessage.getAttachments() != null) {
                        for (Attachment att : outMessage.getAttachments()) {
                            msg.addAttachment(att.getId(), att
                                    .getDataHandler());
                        }
                    }
                    //copy properties
                    
                    for (Map.Entry<String, Object> ent : inMessage.entrySet()) {
                        //check if value is Serializable, and if value is Map or collection,
                        //just exclude it since the entry of it may not be Serializable as well
                        if (ent.getValue() instanceof Serializable 
                                && !(ent.getValue() instanceof Map)
                                && !(ent.getValue() instanceof Collection)) {
                            msg.setProperty(ent.getKey(), ent.getValue());
                        }
                    }


                    //copy contents
                    msg.setContent(new DOMSource(doc));
                    xchng.setMessage(msg, "out");
                    
                }
                LOG.fine(new org.apache.cxf.common.i18n.Message(
                    "POST.DISPATCH", LOG).toString());
                if (xchng.getStatus() == ExchangeStatus.ACTIVE
                        && Boolean.TRUE.equals(xchng.getProperty(SEND_SYNC))) {
                    channel.sendSync(xchng);
                } else {
                    channel.send(xchng);
                }
            }
        } catch (Exception ex) { 
            LOG.log(Level.SEVERE, new org.apache.cxf.common.i18n.Message(
                "ERROR.SEND.MESSAGE", LOG).toString(), ex);
        }
    }

}
