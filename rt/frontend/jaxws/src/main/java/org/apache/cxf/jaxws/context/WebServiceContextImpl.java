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

package org.apache.cxf.jaxws.context;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.logging.Logger;

import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;

import org.w3c.dom.Element;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.security.SecurityContext;

public class WebServiceContextImpl implements WebServiceContext {
    private static final Logger LOG = LogUtils.getL7dLogger(WebServiceContextImpl.class);

    private static ThreadLocal<MessageContext> context = new ThreadLocal<MessageContext>();

    public WebServiceContextImpl() { 
    }

    public WebServiceContextImpl(MessageContext ctx) { 
        setMessageContext(ctx);
    } 

    // Implementation of javax.xml.ws.WebServiceContext

    public final MessageContext getMessageContext() {
        return context.get();
    }

    public final Principal getUserPrincipal() {
        SecurityContext ctx = (SecurityContext)getMessageContext().get(SecurityContext.class.getName());
        if (ctx == null) {
            return null;
        }
        return ctx.getUserPrincipal();
    }

    public final boolean isUserInRole(final String role) {
        SecurityContext ctx = (SecurityContext)getMessageContext().get(SecurityContext.class.getName());
        if (ctx == null) {
            return false;
        }
        return ctx.isUserInRole(role);
    }
    
    public EndpointReference getEndpointReference(Element... referenceParameters) {
        WrappedMessageContext ctx = (WrappedMessageContext)getMessageContext();
        org.apache.cxf.message.Message msg = ctx.getWrappedMessage();
        Endpoint ep = msg.getExchange().get(Endpoint.class);

        W3CEndpointReferenceBuilder builder = new W3CEndpointReferenceBuilder();
        builder.address(ep.getEndpointInfo().getAddress());
        builder.serviceName(ep.getService().getName());
        builder.endpointName(ep.getEndpointInfo().getName());
        URI wsdlDescription = ep.getEndpointInfo().getProperty("URI", URI.class);
        if (wsdlDescription == null) {
            String address = ep.getEndpointInfo().getAddress();
            try {
                wsdlDescription = new URI(address + "?wsdl");
            } catch (URISyntaxException e) {
                // do nothing
            }
            ep.getEndpointInfo().setProperty("URI", wsdlDescription);
        }
        builder.wsdlDocumentLocation(wsdlDescription.toString());
        
        /*
        if (ep.getEndpointInfo().getService().getDescription() != null) {
            builder.wsdlDocumentLocation(ep.getEndpointInfo().getService()
                                     .getDescription().getBaseURI());
        }
        */
        if (referenceParameters != null) {
            for (Element referenceParameter : referenceParameters) {
                builder.referenceParameter(referenceParameter);
            }
        }
        
        return builder.build();
    }

    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz,
                                                                Element... referenceParameters) {
        if (W3CEndpointReference.class.isAssignableFrom(clazz)) {
            return clazz.cast(getEndpointReference(referenceParameters));
        } else {
            throw new WebServiceException(new Message("ENDPOINTREFERENCE_TYPE_NOT_SUPPORTED",
                                                      LOG, clazz.getName()).toString());
        }
    }

    public static void setMessageContext(MessageContext ctx) {
        context.set(ctx);
    }

    public static void clear() {
        context.set(null);
    }

}
