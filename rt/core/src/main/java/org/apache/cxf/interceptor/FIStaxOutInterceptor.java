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

package org.apache.cxf.interceptor;


import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.sun.xml.fastinfoset.stax.StAXDocumentSerializer;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;


/**
 * Creates an XMLStreamReader from the InputStream on the Message.
 */
public class FIStaxOutInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final String FI_ENABLED = "org.apache.cxf.fastinfoset.enabled";
    private static final String OUTPUT_STREAM_HOLDER = FIStaxOutInterceptor.class.getName() + ".outputstream";
    private static final FIStaxOutEndingInterceptor ENDING = new FIStaxOutEndingInterceptor();
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(FIStaxOutInterceptor.class);

    boolean force;
    
    public FIStaxOutInterceptor() {
        super(Phase.PRE_STREAM);
        addAfter(AttachmentOutInterceptor.class.getName());
        addBefore(StaxOutInterceptor.class.getName());
    }
    public FIStaxOutInterceptor(boolean f) {
        this();
        force = f;
    }
    
    @Override
    public void handleFault(Message message) {
        super.handleFault(message);
        OutputStream os = (OutputStream)message.get(OUTPUT_STREAM_HOLDER);
        if (os != null) {
            message.setContent(OutputStream.class, os);
        }
    }
    
    public void handleMessage(Message message) {
        OutputStream out = message.getContent(OutputStream.class);
        XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
        if (out == null || writer != null) {
            return;
        } 
        
        boolean req = isRequestor(message);
        Object o = message.getContextualProperty(FI_ENABLED);
        if (!req) {
            if (message.getExchange().getInMessage() != null) {
                //check incoming accept header
                String s = (String)message.getExchange().getInMessage().get(Message.ACCEPT_CONTENT_TYPE);
                if (s != null && s.contains("fastinfoset")) {
                    o = Boolean.TRUE;
                }
            }
        } else {
            Map<String, List<String>> headers 
                = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
            List<String> accepts = headers.get("Accept");
            if (accepts == null) {
                accepts = new ArrayList<String>();
                headers.put("Accept", accepts);
            }
            String a = "application/fastinfoset";
            if (!accepts.isEmpty()) {
                a += ", " + accepts.get(0);
                accepts.set(0, a);
            } else {
                accepts.add(a);
            }
        }
            
        if (force 
            || MessageUtils.isTrue(o)) {
            StAXDocumentSerializer serializer = getOutput(message, out);
            message.setContent(XMLStreamWriter.class, serializer);
            
            message.removeContent(OutputStream.class);
            message.put(OUTPUT_STREAM_HOLDER, out);
            message.put(AbstractOutDatabindingInterceptor.DISABLE_OUTPUTSTREAM_OPTIMIZATION,
                  Boolean.TRUE);

            String s = (String)message.get(Message.CONTENT_TYPE);
            if (s.contains("application/soap+xml")) {
                s = s.replace("application/soap+xml", "application/soap+fastinfoset");
                message.put(Message.CONTENT_TYPE, s);
            } else {
                message.put(Message.CONTENT_TYPE, "application/fastinfoset");
            }
            
            try {
                serializer.writeStartDocument();
            } catch (XMLStreamException e) {
                throw new Fault(e);
            }
            message.getInterceptorChain().add(ENDING);
        }
    }
    
    private StAXDocumentSerializer getOutput(Message m, OutputStream out) {
        /*
        StAXDocumentSerializer serializer = (StAXDocumentSerializer)m.getExchange().get(Endpoint.class)
            .remove(StAXDocumentSerializer.class.getName());
        if (serializer != null) {
            serializer.setOutputStream(out);
        } else {
            serializer = new StAXDocumentSerializer(out);
        }
        return serializer;
        */
        return new StAXDocumentSerializer(out);
    }
    public static class FIStaxOutEndingInterceptor extends AbstractPhaseInterceptor<Message> {
        public FIStaxOutEndingInterceptor() {
            super(Phase.PRE_STREAM_ENDING);
            getAfter().add(AttachmentOutInterceptor.AttachmentOutEndingInterceptor.class.getName());
        }

        public void handleMessage(Message message) throws Fault {
            try {
                XMLStreamWriter xtw = message.getContent(XMLStreamWriter.class);
                if (xtw != null) {
                    xtw.writeEndDocument();
                    xtw.flush();
                    xtw.close();
                }
                /*
                if (xtw instanceof StAXDocumentSerializer) {
                    ((StAXDocumentSerializer)xtw).setOutputStream(null);
                    message.getExchange().get(Endpoint.class)
                        .put(StAXDocumentSerializer.class.getName(), xtw);
                }
                */

                OutputStream os = (OutputStream)message.get(OUTPUT_STREAM_HOLDER);
                if (os != null) {
                    message.setContent(OutputStream.class, os);
                }
                message.removeContent(XMLStreamWriter.class);
            } catch (XMLStreamException e) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("STAX_WRITE_EXC", BUNDLE), e);
            }
        }

    }    

}
