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

package org.apache.cxf.binding.soap.interceptor;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.phase.Phase;

public class CheckFaultInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(CheckFaultInterceptor.class);
    
    public CheckFaultInterceptor() {
        this(Phase.POST_PROTOCOL);
    }
    public CheckFaultInterceptor(String phase) {
        super(phase);
    }

    public void handleMessage(SoapMessage message) {
        XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
        if (xmlReader == null) {
            return;
        }
        try {
            // advance to first tag.
            int x = xmlReader.getEventType();
            while (x != XMLStreamReader.START_ELEMENT
                && x != XMLStreamReader.END_ELEMENT
                && xmlReader.hasNext()) {
                x = xmlReader.next();
            }
            if (!xmlReader.hasNext()) {
                //end of document, just return
                return;
            }
        } catch (XMLStreamException e) {
            throw new SoapFault(new Message("XML_STREAM_EXC", LOG), e, 
                                message.getVersion().getSender());
        }
        if (message.getVersion().getFault().equals(xmlReader.getName()) && isRequestor(message)) {
            Endpoint ep = message.getExchange().get(Endpoint.class);
            message.getInterceptorChain().abort();
            if (ep.getInFaultObserver() != null) {
                ep.getInFaultObserver().onMessage(message);
            }
        }
    }
}
