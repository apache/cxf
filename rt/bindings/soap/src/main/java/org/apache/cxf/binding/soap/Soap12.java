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

package org.apache.cxf.binding.soap;

import javax.xml.namespace.QName;

/**
 * Represents the SOAP 1.2 version
 *
 */
public final class Soap12 implements SoapVersion {
    public static final String SOAP_NAMESPACE = "http://www.w3.org/2003/05/soap-envelope";

    private static final Soap12 INSTANCE = new Soap12();

    private static final double VERSION = 1.2;

    private static final String NONE_ROLE = SOAP_NAMESPACE + "/role/none";

    private static final String ULTIMATE_RECEIVER_ROLE = SOAP_NAMESPACE + "/role/ultimateReceiver";

    private static final String NEXT_ROLE = SOAP_NAMESPACE + "/role/next";

    private static final String SOAP_ENCODING_STYLE = "http://www.w3.org/2003/05/soap-encoding";

    private final QName envelope = new QName(SOAP_NAMESPACE, "Envelope");

    private final QName header = new QName(SOAP_NAMESPACE, "Header");

    private final QName body = new QName(SOAP_NAMESPACE, "Body");

    private final QName fault = new QName(SOAP_NAMESPACE, "Fault");

    private Soap12() {
       // Singleton
    }

    public static Soap12 getInstance() {
        return INSTANCE;
    }
    public String getBindingId() {
        return SoapBindingConstants.SOAP12_BINDING_ID;
    }

    public double getVersion() {
        return VERSION;
    }

    public String getNamespace() {
        return SOAP_NAMESPACE;
    }

    public QName getEnvelope() {
        return envelope;
    }

    public QName getHeader() {
        return header;
    }

    public QName getBody() {
        return body;
    }

    public QName getFault() {
        return fault;
    }

    public String getSoapEncodingStyle() {
        return SOAP_ENCODING_STYLE;
    }

    // Role URIs
    // -------------------------------------------------------------------------
    public String getNoneRole() {
        return NONE_ROLE;
    }

    public String getUltimateReceiverRole() {
        return ULTIMATE_RECEIVER_ROLE;
    }

    public String getNextRole() {
        return NEXT_ROLE;
    }

    public String getAttrNameRole() {
        return "role";
    }

    public String getAttrNameMustUnderstand() {
        return "mustUnderstand";
    }

    public String getAttrValueMustUnderstand(boolean value) {
        return value ? "true" : "false";
    }

    public QName getReceiver() {
        return new QName(SOAP_NAMESPACE, "Receiver");
    }

    public QName getSender() {
        return new QName(SOAP_NAMESPACE, "Sender");
    }

    public QName getMustUnderstand() {
        return new QName(SOAP_NAMESPACE, "MustUnderstand");
    }

    public QName getVersionMismatch() {
        return new QName(SOAP_NAMESPACE, "VersionMismatch");
    }

    public QName getDateEncodingUnknown() {
        return new QName(SOAP_NAMESPACE, "DataEncodingUnknown");
    }
    public String getContentType() {
        return "application/soap+xml";
    }

}
