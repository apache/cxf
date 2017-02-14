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

package org.apache.cxf.frontend;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

public class WSDLGetOutInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final WSDLGetOutInterceptor INSTANCE = new WSDLGetOutInterceptor();

    public WSDLGetOutInterceptor() {
        super(Phase.PRE_STREAM);
        getAfter().add(StaxOutInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault {
        Document doc = (Document)message.get(WSDLGetInterceptor.DOCUMENT_HOLDER);
        if (doc == null) {
            return;
        }
        message.remove(WSDLGetInterceptor.DOCUMENT_HOLDER);

        XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
        if (writer == null) {
            return;
        }
        message.put(Message.CONTENT_TYPE, "text/xml");
        try {
            StaxUtils.writeDocument(doc, writer,
                                    !MessageUtils.getContextualBoolean(message,
                                                                       StaxOutInterceptor.FORCE_START_DOCUMENT,
                                                                       false),
                                    true);
        } catch (XMLStreamException e) {
            throw new Fault(e);
        }
    }
}
