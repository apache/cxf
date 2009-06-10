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

package org.apache.cxf.databinding.stax;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.interceptor.DocLiteralInInterceptor;
import org.apache.cxf.interceptor.URIMappingInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;

public class StaxDataBindingInterceptor extends AbstractInDatabindingInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(DocLiteralInInterceptor.class);

    public StaxDataBindingInterceptor() {
        super(Phase.UNMARSHAL);
        addAfter(URIMappingInterceptor.class.getName());
    }

    public void handleMessage(Message message) {
        if (isGET(message) && message.getContent(List.class) != null) {
            LOG.fine("StaxDataBindingInterceptor skipped in HTTP GET method");
            return;
        }
        
        DepthXMLStreamReader xmlReader = getXMLStreamReader(message);
        DataReader<XMLStreamReader> dr = getDataReader(message);
        MessageContentsList parameters = new MessageContentsList();

        Exchange exchange = message.getExchange();
        BindingOperationInfo bop = exchange.get(BindingOperationInfo.class);

        //if body is empty and we have BindingOperationInfo, we do not need to match 
        //operation anymore, just return
        if (!StaxUtils.toNextElement(xmlReader) && bop != null) {
            // body may be empty for partial response to decoupled request
            return;
        }
        
        if (bop == null) {
            Endpoint ep = exchange.get(Endpoint.class);
            bop = ep.getBinding().getBindingInfo().getOperations().iterator().next();
        }
        
        message.getExchange().put(BindingOperationInfo.class, bop);
        
        if (isRequestor(message)) {
            parameters.put(bop.getOutput().getMessageParts().get(0), dr.read(xmlReader));            
        } else {
            parameters.put(bop.getInput().getMessageParts().get(0), dr.read(xmlReader));            
        }

        
        if (parameters.size() > 0) {
            message.setContent(List.class, parameters);
        }
    }
}
