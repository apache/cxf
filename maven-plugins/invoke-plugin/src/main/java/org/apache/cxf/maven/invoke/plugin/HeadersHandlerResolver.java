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
package org.apache.cxf.maven.invoke.plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.PortInfo;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * {@link HandlerResolver} that adds any SOAP headers given to the outgoing SOAP message.
 */
final class HeadersHandlerResolver implements HandlerResolver {

    /**
     * {@link SOAPHandler} that performs the addition of given SOAP headers. SOAP headers are added from an array of
     * {@link Node} objects given to the {@link HeadersHandlerResolver#HeadersHandlerResolver(Node[])} constructor.
     */
    final class HeaderHandler implements SOAPHandler<SOAPMessageContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void close(final MessageContext context) {
            // noop
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<QName> getHeaders() {
            return Collections.emptySet();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean handleFault(final SOAPMessageContext context) {
            return true; // proceed with the next handler
        }

        /**
         * Adds the given soap headers.
         */
        @Override
        public boolean handleMessage(final SOAPMessageContext context) {
            if ((headers == null) || (headers.length == 0)) {
                return true;
            }

            final SOAPHeader soapHeader = soapHeaderFrom(context);

            final Document ownerDocument = soapHeader.getOwnerDocument();
            for (final Node header : headers) {
                final Node headersNode = ownerDocument.importNode(header, true);

                soapHeader.appendChild(headersNode);
            }

            return true;
        }

        /**
         * Returns or creates {@link SOAPHeader} on the {@link SOAPMessage} in the {@link SOAPMessageContext}.
         *
         * @param context
         * @return existing or newly added SOAP header
         */
        private SOAPHeader soapHeaderFrom(final SOAPMessageContext context) {
            final SOAPMessage soapMessage = context.getMessage();
            final SOAPPart soapPart = soapMessage.getSOAPPart();
            final SOAPHeader soapHeader;
            try {
                final SOAPEnvelope soapEnvelope = soapPart.getEnvelope();

                soapHeader = Optional.ofNullable(soapEnvelope.getHeader()).orElseGet(() -> {
                    try {
                        return soapEnvelope.addHeader();
                    } catch (final SOAPException e) {
                        throw new IllegalStateException("Unable to add SOAP header", e);
                    }
                });
            } catch (final SOAPException e) {
                throw new IllegalStateException("Unable to create SOAP header", e);
            }
            return soapHeader;
        }

    }

    /** Headers to add. */
    private final Node[] headers;

    /**
     * Pass in the headers to be added on the SOAP message.
     *
     * @param headers
     *            headers to be added
     */
    HeadersHandlerResolver(final Node[] headers) {
        this.headers = headers;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public List<Handler> getHandlerChain(final PortInfo portInfo) {
        return Arrays.asList(new HeaderHandler());
    }

}
