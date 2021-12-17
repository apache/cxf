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
package org.apache.cxf.systest.ws.security.handler;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

public class JAXWSHandler implements SOAPHandler<SOAPMessageContext> {

    private PrintStream out;

    public JAXWSHandler() {
        setLogStream(System.out);
    }

    protected final void setLogStream(PrintStream ps) {
        out = ps;
    }

    @SuppressWarnings("rawtypes")
    public void init(Map c) {
    }

    public Set<QName> getHeaders() {
        return null;
    }

    public boolean handleMessage(SOAPMessageContext smc) {
        // logToSystemOut(smc);
        return true;
    }

    public boolean handleFault(SOAPMessageContext smc) {
        // logToSystemOut(smc);
        return true;
    }

    public void close(MessageContext messageContext) {

    }

    public void destroy() {

    }

    protected void logToSystemOut(SOAPMessageContext smc) {
        Boolean outboundProperty = (Boolean)smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (outboundProperty.booleanValue()) {
            out.println("\nOutbound message:");
        } else {
            out.println("\nInbound message:");
        }

        SOAPMessage message = smc.getMessage();
        try {
            message.writeTo(out);
            out.println();
        } catch (Exception e) {
            out.println("Exception in handler: " + e);
        }

        out.println("WSDL_SERVICE = " + smc.get(MessageContext.WSDL_SERVICE));
        out.println("WSDL_INTERFACE = " + smc.get(MessageContext.WSDL_INTERFACE));
        out.println("WSDL_PORT = " + smc.get(MessageContext.WSDL_PORT));
        out.println("WSDL_OPERATION = " + smc.get(MessageContext.WSDL_OPERATION));
    }
}
