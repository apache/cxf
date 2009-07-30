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

import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.logging.LogUtils;

/**
 * 
 */
public final class JMSFaultFactory {

    static final Logger LOG = LogUtils.getL7dLogger(JMSFaultFactory.class);

    private JMSFaultFactory() {
    }

    public static JMSFault createContentTypeMismatchFault(String contentType) {
        String m = new org.apache.cxf.common.i18n.Message("CONTENTTYPE_MISMATCH", LOG,
                                                          new Object[] {
                                                              contentType
                                                          }).toString();
        return createFault(SoapJMSConstants.getContentTypeMismatchQName(), m);
    }

    public static JMSFault createMalformedRequestURIFault(String requestURI) {
        String m = new org.apache.cxf.common.i18n.Message("MALFORMED_REQUESTURI", LOG,
                                                          new Object[] {
                                                              requestURI
                                                          }).toString();
        return createFault(SoapJMSConstants.getMalformedRequestURIQName(), m);
    }

    public static JMSFault createMismatchedSoapActionFault(String soapAction) {
        String m = new org.apache.cxf.common.i18n.Message("MISMATCHED_SOAPACTION", LOG,
                                                          new Object[] {
                                                              soapAction
                                                          }).toString();
        return createFault(SoapJMSConstants.getMismatchedSoapActionQName(), m);
    }

    public static JMSFault createMissingContentTypeFault() {
        String m = new org.apache.cxf.common.i18n.Message("MISSING_CONTENTTYPE", LOG).toString();
        return createFault(SoapJMSConstants.getMissingContentTypeQName(), m);
    }

    public static JMSFault createMissingRequestURIFault() {
        String m = new org.apache.cxf.common.i18n.Message("MISSING_REQUESTURI", LOG).toString();
        return createFault(SoapJMSConstants.getMissingRequestURIQName(), m);
    }

    public static JMSFault createTargetServiceNotAllowedInRequestURIFault() {
        String m = new org.apache.cxf.common.i18n.Message(
                                                          "TARGET_SERVICE_NOT_ALLOWED_IN_REQUESTURI",
                                                          LOG).toString();
        return createFault(SoapJMSConstants.getTargetServiceNotAllowedInRequestURIQName(), m);
    }

    public static JMSFault createUnrecognizedBindingVerionFault(String bindingVersion) {
        String m = new org.apache.cxf.common.i18n.Message("UNRECOGNIZED_BINDINGVERSION", LOG,
                                                          new Object[] {
                                                              bindingVersion
                                                          }).toString();
        return createFault(SoapJMSConstants.getUnrecognizedBindingVersionQName(), m);
    }

    public static JMSFault createUnsupportedJMSMessageFormatFault(String messageFormat) {
        String m = new org.apache.cxf.common.i18n.Message("UNSUPPORTED_JMSMESSAGEFORMAT", LOG,
                                                          new Object[] {
                                                              messageFormat
                                                          }).toString();
        return createFault(SoapJMSConstants.getUnsupportedJMSMessageFormatQName(), m);
    }

    private static JMSFault createFault(QName faultCode, String message) {
        JMSFaultType jmsFaultType = new JMSFaultType();
        jmsFaultType.setFaultCode(faultCode);
        JMSFault jmsFault = new JMSFault(message);
        jmsFault.setJmsFaultType(jmsFaultType);
        jmsFault.setSender(true);
        return jmsFault;
    }
}
