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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import com.sun.xml.fastinfoset.stax.factory.StAXOutputFactory;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;


/**
 * Creates an XMLStreamReader from the InputStream on the Message.
 */
public class FIStaxOutInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final String FI_ENABLED = "org.apache.cxf.fastinfoset.enabled";
    
    boolean force;
    XMLOutputFactory factory = new StAXOutputFactory();
    
    public FIStaxOutInterceptor() {
        super(Phase.PRE_STREAM);
        addAfter(AttachmentOutInterceptor.class.getName());
        addBefore(StaxOutInterceptor.class.getName());
    }
    public FIStaxOutInterceptor(boolean f) {
        this();
        force = f;
    }
    
    public void handleMessage(Message message) {
        XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
        if (writer != null) {
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
            || Boolean.TRUE.equals(o)) {
            
            message.put(XMLOutputFactory.class.getName(),
                        factory);
            String s = (String)message.get(Message.CONTENT_TYPE);
            if (s.contains("application/soap+xml")) {
                s = s.replace("application/soap+xml", "application/soap+fastinfoset");
                message.put(Message.CONTENT_TYPE, s);
            } else {
                message.put(Message.CONTENT_TYPE, "application/fastinfoset");
            }
        }
    }

}
