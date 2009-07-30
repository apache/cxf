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

import javax.xml.namespace.QName;

/**
 * 
 */
public final class SoapJMSConstants {

    public static final String SOAP_JMS_PREFIX = "SOAPJMS_";
    
    // JMS Message properties' names.
    public static final String REQUESTURI_PARAMETER_NAME = "requestURI";
    public static final String BINDINGVERSION_PARAMETER_NAME = "bindingVersion";
    public static final String SOAPACTION_PARAMETER_NAME = "soapAction";
    public static final String TARGETSERVICE_PARAMETER_NAME = "targetService";
    public static final String CONTENTTYPE_PARAMETER_NAME = "contentType";
    public static final String ISFAULT_PARAMETER_NAME = "isFault";
    
    // JMS Field name
    public static final String REQUESTURI_FIELD = SOAP_JMS_PREFIX + REQUESTURI_PARAMETER_NAME;
    public static final String BINDINGVERSION_FIELD = SOAP_JMS_PREFIX
                                                      + BINDINGVERSION_PARAMETER_NAME;
    public static final String SOAPACTION_FIELD = SOAP_JMS_PREFIX + SOAPACTION_PARAMETER_NAME;
    public static final String TARGETSERVICE_FIELD = SOAP_JMS_PREFIX + TARGETSERVICE_PARAMETER_NAME;
    public static final String CONTENTTYPE_FIELD = SOAP_JMS_PREFIX + CONTENTTYPE_PARAMETER_NAME;
    public static final String ISFAULT_FIELD = SOAP_JMS_PREFIX + ISFAULT_PARAMETER_NAME;

    public static final String SOAP_JMS_SPECIFICIATION_TRANSPORTID = "http://www.w3.org/2008/07/"
                                                                     + "soap/bindings/JMS/";
    public static final String SOAP_JMS_NAMESPACE = SOAP_JMS_SPECIFICIATION_TRANSPORTID;
    
    // fault codes
    private static final String JMS_CONTENTTYPEMISMATCH_FAULT_CODE = "contentTypeMismatch";
    private static final String JMS_MALFORMEDREQUESTURI_FAULT_CODE = "malformedRequestURI";
    private static final String JMS_MISMATCHEDSOAPACTION_FAULT_CODE = "mismatchedSoapAction";
    private static final String JMS_MISSINGCONTENTTYPE_FAULT_CODE = "missingContentType";
    private static final String JMS_MISSINGREQUESTURI_FAULT_CODE = "missingRequestURI";
    private static final String JMS_TARGETSERVICENOTALLOWEDINREQUESTURI_FAULT_CODE = 
        "targetServiceNotAllowedInRequestURI";
    private static final String JMS_UNRECOGNIZEDBINDINGVERSION_FAULT_CODE = "unrecognizedBindingVersion";
    private static final String JMS_UNSUPPORTEDJMSMESSAGEFORMAT_FAULT_CODE = "unsupportedJMSMessageFormat";

    private SoapJMSConstants() {
    }
    
    public static QName getContentTypeMismatchQName() {
        return new QName(SOAP_JMS_NAMESPACE, JMS_CONTENTTYPEMISMATCH_FAULT_CODE);
    }

    public static QName getMalformedRequestURIQName() {
        return new QName(SOAP_JMS_NAMESPACE, JMS_MALFORMEDREQUESTURI_FAULT_CODE);
    }

    public static QName getMismatchedSoapActionQName() {
        return new QName(SOAP_JMS_NAMESPACE, JMS_MISMATCHEDSOAPACTION_FAULT_CODE);
    }

    public static QName getMissingContentTypeQName() {
        return new QName(SOAP_JMS_NAMESPACE, JMS_MISSINGCONTENTTYPE_FAULT_CODE);
    }

    public static QName getMissingRequestURIQName() {
        return new QName(SOAP_JMS_NAMESPACE, JMS_MISSINGREQUESTURI_FAULT_CODE);
    }

    public static QName getTargetServiceNotAllowedInRequestURIQName() {
        return new QName(SOAP_JMS_NAMESPACE, JMS_TARGETSERVICENOTALLOWEDINREQUESTURI_FAULT_CODE);
    }

    public static QName getUnrecognizedBindingVersionQName() {
        return new QName(SOAP_JMS_NAMESPACE, JMS_UNRECOGNIZEDBINDINGVERSION_FAULT_CODE);
    }

    public static QName getUnsupportedJMSMessageFormatQName() {
        return new QName(SOAP_JMS_NAMESPACE, JMS_UNSUPPORTEDJMSMESSAGEFORMAT_FAULT_CODE);
    }
}
