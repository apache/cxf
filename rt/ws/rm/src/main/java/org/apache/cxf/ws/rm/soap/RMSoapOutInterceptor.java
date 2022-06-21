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

package org.apache.cxf.ws.rm.soap;

import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.soap.MAPCodec;
import org.apache.cxf.ws.rm.ProtocolVariation;
import org.apache.cxf.ws.rm.RM10Constants;
import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.RMContextUtils;
import org.apache.cxf.ws.rm.RMProperties;
import org.apache.cxf.ws.rm.SequenceFault;

/**
 * Protocol Handler responsible for {en|de}coding the RM
 * Properties for {outgo|incom}ing messages.
 */
public class RMSoapOutInterceptor extends AbstractSoapInterceptor {

    protected static JAXBContext jaxbContext;

    private static final Set<QName> HEADERS;
    static {
        Set<QName> set = new HashSet<>();
        set.addAll(RM10Constants.HEADERS);
        set.addAll(RM11Constants.HEADERS);
        HEADERS = set;
    }

    private static final Logger LOG = LogUtils.getL7dLogger(RMSoapOutInterceptor.class);

    /**
     * Constructor.
     */
    public RMSoapOutInterceptor() {
        super(Phase.PRE_PROTOCOL);

        addAfter(MAPCodec.class.getName());
    }

    // AbstractSoapInterceptor interface

    /**
     * @return the set of SOAP headers understood by this handler
     */
    public Set<QName> getUnderstoodHeaders() {
        return HEADERS;
    }

    // Interceptor interface

    /* (non-Javadoc)
     * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
     */
    public void handleMessage(SoapMessage message) throws Fault {
        encode(message);
    }

    /**
     * Encode the current RM properties in protocol-specific headers.
     *
     * @param message the SOAP message
     */
    void encode(SoapMessage message) {
        RMProperties rmps = RMContextUtils.retrieveRMProperties(message, true);
        if (null != rmps) {
            encode(message, rmps);
        } else if (MessageUtils.isFault(message)) {
            Exception ex = message.getContent(Exception.class);
            if (ex instanceof SoapFault && ex.getCause() instanceof SequenceFault) {
                encodeFault(message, (SequenceFault)ex.getCause());
            }
        }

    }

    /**
     * Encode the current RM properties in protocol-specific headers.
     *
     * @param message the SOAP message.
     * @param rmps the current RM properties.
     */
    public static void encode(SoapMessage message, RMProperties rmps) {
        if (null == rmps) {
            return;
        }
        LOG.log(Level.FINE, "encoding RMPs in SOAP headers");
        try {

            AddressingProperties maps = RMContextUtils.retrieveMAPs(message, false, true);
            ProtocolVariation protocol = ProtocolVariation.findVariant(rmps.getNamespaceURI(), maps.getNamespaceURI());
            List<Header> headers = message.getHeaders();
            int startSize = headers.size();
            protocol.getCodec().buildHeaders(rmps, headers);
            if (startSize != headers.size() && MessageUtils.isPartialResponse(message)) {
                // make sure the response is returned as HTTP 200 and not 202
                message.put(Message.RESPONSE_CODE, HttpURLConnection.HTTP_OK);
            }
        } catch (JAXBException je) {
            LOG.log(Level.WARNING, "SOAP_HEADER_ENCODE_FAILURE_MSG", je);
        }
    }

    /**
     * Encode the SequenceFault in protocol-specific header.
     *
     * @param message the SOAP message.
     * @param sf the SequenceFault.
     */
    public static void encodeFault(SoapMessage message, SequenceFault sf) {
        LOG.log(Level.FINE, "Encoding SequenceFault in SOAP header");
        try {
            Message inmsg = message.getExchange().getInMessage();
            RMProperties rmps = RMContextUtils.retrieveRMProperties(inmsg, false);
            AddressingProperties maps = RMContextUtils.retrieveMAPs(inmsg, false, false);
            ProtocolVariation protocol = ProtocolVariation.findVariant(rmps.getNamespaceURI(),
                maps.getNamespaceURI());
            Header header = protocol.getCodec().buildHeaderFault(sf);
            List<Header> headers = message.getHeaders();
            headers.add(header);
        } catch (JAXBException je) {
            LOG.log(Level.WARNING, "SOAP_HEADER_ENCODE_FAILURE_MSG", je);
        }
    }
}