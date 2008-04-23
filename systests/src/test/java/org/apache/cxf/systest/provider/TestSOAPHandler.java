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
package org.apache.cxf.systest.provider;


import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;


public class TestSOAPHandler implements SOAPHandler<SOAPMessageContext> {
    public boolean handleMessage(SOAPMessageContext ctx) {
        try {
            SOAPMessage msg = ctx.getMessage();
            /*
             * System.out.println("-----------soap---------");
             * msg.writeTo(System.out);
             * System.out.println("-----------soap---------");
             */

            SOAPEnvelope env = msg.getSOAPPart().getEnvelope();
            SOAPBody body = env.getBody();
            Iterator it = body.getChildElements();
            while (it.hasNext()) {
                
                Object elem = it.next();
                if (elem instanceof SOAPElement) {

                    Iterator it2 = ((SOAPElement)elem).getChildElements();
                    while (it2.hasNext()) {
                        Object elem2 = it2.next();
                        if (elem2 instanceof SOAPElement) {
                            String value = ((SOAPElement)elem2).getValue();
                            if (value != null 
                                && (value.indexOf("Milestone-0") >= 0
                                || value.indexOf("TestGreetMeResponseServerLogicalHandler") >= 0)) {
                                value = value + "ServerSOAPHandler";
                                ((SOAPElement)elem2).setValue(value);
                            }
                        }
                    }
                }
            }
            msg.saveChanges();

            /*
             * System.out.println("-----------soapaf---------");
             * msg.writeTo(System.out);
             * System.out.println("-----------soapaf---------");
             */
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }
    public final Set<QName> getHeaders() {
        return null;
    }
    public boolean handleFault(SOAPMessageContext ctx) {
        return true;
    }
    public void close(MessageContext arg0) {
    }
}