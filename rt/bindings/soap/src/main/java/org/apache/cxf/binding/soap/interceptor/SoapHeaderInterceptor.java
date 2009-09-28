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

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.model.SoapHeaderInfo;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.interceptor.BareInInterceptor;
import org.apache.cxf.interceptor.DocLiteralInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.W3CDOMStreamReader;

/**
 * Perform databinding of the SOAP headers.
 */
public class SoapHeaderInterceptor extends AbstractInDatabindingInterceptor {

    public SoapHeaderInterceptor() {
        super(Phase.UNMARSHAL);
        addAfter(BareInInterceptor.class.getName());
        addAfter(RPCInInterceptor.class.getName());
        addAfter(DocLiteralInInterceptor.class.getName());
    }

    public void handleMessage(Message m) throws Fault {
        SoapMessage message = (SoapMessage) m;
        Exchange exchange = message.getExchange();

        MessageContentsList parameters = MessageContentsList.getContentsList(message);

        if (null == parameters) {
            parameters = new MessageContentsList();
        }

        BindingOperationInfo bop = exchange.get(BindingOperationInfo.class);
        if (null == bop) {
            return;
        }

        if (bop.isUnwrapped()) {
            bop = bop.getWrappedOperation();
        }
        
        boolean client = isRequestor(message);
        BindingMessageInfo bmi = client ? bop.getOutput() : bop.getInput();
        if (bmi == null) {
            // one way operation.
            return;
        }
        
        List<SoapHeaderInfo> headers = bmi.getExtensors(SoapHeaderInfo.class);
        if (headers == null || headers.size() == 0) {
            return;
        }
        
        boolean supportsNode = this.supportsDataReader(message, Node.class);
        for (SoapHeaderInfo header : headers) {
            MessagePartInfo mpi = header.getPart();
            if (mpi.getTypeClass() != null) {

                Header param = findHeader(message, mpi);
                
                Object object = null;
                if (param != null) {
                    message.getHeaders().remove(param);
                    
                    if (param.getDataBinding() == null) {
                        Node source = (Node)param.getObject();
                        if (supportsNode) {
                            object = getNodeDataReader(message).read(mpi, source);
                        } else {
                            W3CDOMStreamReader reader = new W3CDOMStreamReader((Element)source);
                            try {
                                reader.nextTag(); //advance into the first tag
                            } catch (XMLStreamException e) {
                                //ignore
                            } 
                            object = getDataReader(message, XMLStreamReader.class).read(mpi, reader);
                        }
                    } else {
                        object = param.getObject();
                    }
                    
                }
                parameters.put(mpi, object);
            }
        }
        if (parameters.size() > 0) {
            message.setContent(List.class, parameters);
        }
    }

    private Header findHeader(SoapMessage message, MessagePartInfo mpi) {
        return message.getHeader(mpi.getConcreteName());
    }
}
