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

package org.apache.cxf.jaxws.handler.soap;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPHeaderElement;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Message;

public class SOAPMessageContextImpl extends WrappedMessageContext implements SOAPMessageContext {
    private static final SAAJInInterceptor SAAJ_IN = new SAAJInInterceptor();

    private Set<String> roles = new HashSet<>();

    public SOAPMessageContextImpl(Message m) {
        super(m, Scope.HANDLER);
        roles.add(getWrappedSoapMessage().getVersion().getNextRole());
    }

    public void setMessage(SOAPMessage message) {
        if (getWrappedMessage().getContent(Object.class) instanceof SOAPMessage) {
            getWrappedMessage().setContent(Object.class, message);
        } else {
            getWrappedMessage().setContent(SOAPMessage.class, message);
        }
    }

    public SOAPMessage getMessage() {
        SOAPMessage message;
        if (getWrappedMessage().getContent(Object.class) instanceof SOAPMessage) {
            message = (SOAPMessage)getWrappedMessage().getContent(Object.class);
        } else {
            message = getWrappedMessage().getContent(SOAPMessage.class);
        }

        //Only happens to non-Dispatch/Provider case.
        if (null == message) {
            Boolean outboundProperty = (Boolean)get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
            if (outboundProperty == null || !outboundProperty) {
                //No SOAPMessage exists yet, so lets create one
                SAAJ_IN.handleMessage(getWrappedSoapMessage());
                message = getWrappedSoapMessage().getContent(SOAPMessage.class);
            }
        }
        return message;
    }

    public Object[] getHeaders(QName name, JAXBContext context, boolean allRoles) {
        SOAPMessage msg = getMessage();
        SOAPHeader header;
        try {
            header = msg.getSOAPPart().getEnvelope().getHeader();
            if (header == null || !header.hasChildNodes()) {
                return new Object[0];
            }
            List<Object> ret = new ArrayList<>();
            Iterator<SOAPHeaderElement> it = CastUtils.cast(header.examineAllHeaderElements());
            while (it.hasNext()) {
                SOAPHeaderElement she = it.next();
                if ((allRoles
                    || roles.contains(she.getActor()))
                    && name.equals(she.getElementQName())) {
                    ret.add(JAXBUtils.unmarshall(context, she));
                }
            }
            return ret.toArray(new Object[0]);
        } catch (SOAPException | JAXBException e) {
            throw new WebServiceException(e);
        }
    }

    public Set<String> getRoles() {
        return roles;
    }

    private SoapMessage getWrappedSoapMessage() {
        return (SoapMessage)getWrappedMessage();
    }

    public Object get(Object key) {
        Object o = super.get(key);
        if (MessageContext.HTTP_RESPONSE_HEADERS.equals(key)
            || MessageContext.HTTP_REQUEST_HEADERS.equals(key)) {
            Map<?, ?> mp = (Map<?, ?>)o;
            if (mp != null) {
                if (mp.isEmpty()) {
                    return null;
                }
                if (!isRequestor() && isOutbound() && MessageContext.HTTP_RESPONSE_HEADERS.equals(key)) {
                    return null;
                }
                if (isRequestor() && !isOutbound() && MessageContext.HTTP_REQUEST_HEADERS.equals(key)) {
                    return null;
                }
            }
        }
        return o;
    }

}
