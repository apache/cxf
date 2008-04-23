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
package org.apache.cxf.systest.handlers;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.ws.LogicalMessage;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.handlers.types.AddNumbers;
import org.apache.handlers.types.ObjectFactory;


/**
 * handles addition of small numbers.
 */
public class ModifyNumberHandler implements LogicalHandler<LogicalMessageContext> {
    
    public final boolean handleMessage(LogicalMessageContext messageContext) {
        //System.out.println("LogicalMessageHandler handleMessage called");

        try {
            // get the LogicalMessage from our context
            LogicalMessage msg = messageContext.getMessage();

            // check the payload, if its an AddNumbers request, we'll intervene
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            Object payload = msg.getPayload(jaxbContext);
            Object value = payload;
            if (payload instanceof JAXBElement) {
                value = ((JAXBElement)payload).getValue();
            }

            if (value instanceof AddNumbers) {
                AddNumbers req = (AddNumbers)value;

                int a = req.getArg0();
                req.setArg0(a * 10);
                msg.setPayload(payload, jaxbContext);
            }
            return true;
        } catch (JAXBException ex) {
            throw new ProtocolException(ex);
        }

    }

    public final boolean handleFault(LogicalMessageContext messageContext) {
        return true;
    }

    public void close(MessageContext ctx) {
    }

}
