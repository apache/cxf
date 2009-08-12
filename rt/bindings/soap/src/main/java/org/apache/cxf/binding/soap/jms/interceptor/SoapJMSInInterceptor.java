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

package org.apache.cxf.binding.soap.jms.interceptor;

import java.util.List;
import java.util.Map;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

/**
 * 
 */
public class SoapJMSInInterceptor extends AbstractSoapInterceptor {

    public SoapJMSInInterceptor() {
        super(Phase.RECEIVE);
        addAfter(AttachmentInInterceptor.class.getName());
    }

    public void handleMessage(SoapMessage message) throws Fault {
        Map<String, List<String>> headers = CastUtils.cast((Map)message
            .get(Message.PROTOCOL_HEADERS));
        if (headers != null) {
            checkContentType(message, headers);
            checkRequestURI(message, headers);
            checkSoapAction(message, headers);
            checkBindingVersion(message, headers);
            checkJMSMessageFormat(message, headers);
        }
    }

    /**
     * @param message
     * @param headers
     */
    private void checkJMSMessageFormat(SoapMessage message, Map<String, List<String>> headers) {
        List<String> mt = headers.get(SoapJMSConstants.JMS_MESSAGE_TYPE);
        if (mt != null && mt.size() > 0) {
            String messageType = mt.get(0);
            if (!"text".equals(messageType) && !"byte".equals(messageType)) {
                JMSFault jmsFault = JMSFaultFactory.createUnsupportedJMSMessageFormatFault(messageType);
                Fault f = createFault(message, jmsFault);
                if (f != null) {
                    throw f;
                }
            }
        }
    }

    /**
     * @param message
     * @param headers
     */
    private void checkSoapAction(SoapMessage message, Map<String, List<String>> headers) {
        List<String> sa = headers.get(SoapJMSConstants.SOAPACTION_FIELD);
        if (sa != null && sa.size() > 0) {
            //String soapAction = sa.get(0);
            // ToDO
        }
    }

    /**
     * @param message
     * @param headers
     */
    private void checkRequestURI(SoapMessage message, Map<String, List<String>> headers) {
        List<String> ru = headers.get(SoapJMSConstants.REQUESTURI_FIELD);
        JMSFault jmsFault = null;
        if (ru != null && ru.size() > 0) {
            String requestURI = ru.get(0);
            List<String> mr = headers.get(SoapJMSConstants.MALFORMED_REQUESTURI);
            if (mr != null && mr.size() > 0 && mr.get(0).equals("true")) {
                jmsFault = JMSFaultFactory.createMalformedRequestURIFault(requestURI);
            }

            List<String> trn = headers.get(SoapJMSConstants.TARGET_SERVICE_IN_REQUESTURI);
            if (trn != null && trn.size() > 0 && trn.get(0).equals("true")) {
                jmsFault = JMSFaultFactory.createTargetServiceNotAllowedInRequestURIFault();
            }
        } else {
            jmsFault = JMSFaultFactory.createMissingRequestURIFault();
        }
        if (jmsFault != null) {
            Fault f = createFault(message, jmsFault);
            if (f != null) {
                throw f;
            }
        }
    }

    /**
     * @param message
     * @param headers
     */
    private void checkContentType(SoapMessage message, Map<String, List<String>> headers) {
        List<String> ct = headers.get(SoapJMSConstants.CONTENTTYPE_FIELD);
        JMSFault jmsFault = null;
        if (ct != null && ct.size() > 0) {
            String contentType = ct.get(0);
            if (!contentType.startsWith("text/xml")
                && !contentType.startsWith("application/soap+xml")
                && !contentType.startsWith("multipart/related")) {
                jmsFault = JMSFaultFactory.createContentTypeMismatchFault(contentType);
            }
        } else {
            jmsFault = JMSFaultFactory.createMissingContentTypeFault();
        }
        if (jmsFault != null) {
            Fault f = createFault(message, jmsFault);
            if (f != null) {
                throw f;
            }
        }
    }

    /**
     * @param message
     * @param headers
     */
    private void checkBindingVersion(SoapMessage message, Map<String, List<String>> headers) {
        List<String> bv = headers.get(SoapJMSConstants.BINDINGVERSION_FIELD);
        if (bv != null && bv.size() > 0) {
            String bindingVersion = bv.get(0);
            if (!"1.0".equals(bindingVersion)) {
                JMSFault jmsFault = JMSFaultFactory
                    .createUnrecognizedBindingVerionFault(bindingVersion);
                Fault f = createFault(message, jmsFault);
                if (f != null) {
                    throw f;
                }
            }
        }
    }

    private Fault createFault(SoapMessage message, JMSFault jmsFault) {
        Fault f = null;
        Endpoint e = message.getExchange().get(Endpoint.class);
        Binding b = null;
        if (null != e) {
            b = e.getBinding();
        }
        if (null != b) {
            SoapFaultFactory sff = new SoapFaultFactory(b);
            f = sff.createFault(jmsFault);
        }
        return f;
    }
}
