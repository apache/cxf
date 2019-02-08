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
 * Singleton object that represents the SOAP 1.1 VERSION.
 *
 */
public final class Soap11 implements SoapVersion {

    // some constants that don't fit into the SoapVersion paradigm.
    public static final String SOAP_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String SOAP_ENCODING_URI = "http://schemas.xmlsoap.org/soap/encoding/";
    public static final QName ENCODED_STRING = new QName(SOAP_ENCODING_URI, "string");
    public static final QName ENCODED_LONG = new QName(SOAP_ENCODING_URI, "long");
    public static final QName ENCODED_FLOAT = new QName(SOAP_ENCODING_URI, "float");
    public static final QName ENCODED_CHAR = new QName(SOAP_ENCODING_URI, "char");
    public static final QName ENCODED_DOUBLE = new QName(SOAP_ENCODING_URI, "double");
    public static final QName ENCODED_INT = new QName(SOAP_ENCODING_URI, "int");
    public static final QName ENCODED_SHORT = new QName(SOAP_ENCODING_URI, "short");
    public static final QName ENCODED_BOOLEAN = new QName(SOAP_ENCODING_URI, "boolean");
    public static final QName ENCODED_DATETIME = new QName(SOAP_ENCODING_URI, "dateTime");
    public static final QName ENCODED_BASE64 = new QName(SOAP_ENCODING_URI, "base64Binary");
    public static final QName ENCODED_DECIMAL = new QName(SOAP_ENCODING_URI, "decimal");
    public static final QName ENCODED_INTEGER = new QName(SOAP_ENCODING_URI, "integer");

    private static final Soap11 INSTANCE = new Soap11();

    private static final double VERSION = 1.1;

    private static final String NONE_ROLE = SOAP_NAMESPACE + "/role/none";

    private static final String ULTIMATE_RECEIVER_ROLE = SOAP_NAMESPACE + "/role/ultimateReceiver";

    private static final String NEXT_ROLE = "http://schemas.xmlsoap.org/soap/actor/next";

    private static final String SOAP_ENCODING_STYLE = SOAP_ENCODING_URI;

    private final QName envelope = new QName(SOAP_NAMESPACE, "Envelope");

    private final QName header = new QName(SOAP_NAMESPACE, "Header");

    private final QName body = new QName(SOAP_NAMESPACE, "Body");

    private final QName fault = new QName(SOAP_NAMESPACE, "Fault");

    private Soap11() {
        // Singleton
    }


    public static Soap11 getInstance() {
        return INSTANCE;
    }

    public String getBindingId() {
        return SoapBindingConstants.SOAP11_BINDING_ID;
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
        return "actor";
    }

    public String getAttrNameMustUnderstand() {
        return "mustUnderstand";
    }

    public String getAttrValueMustUnderstand(boolean value) {
        return value ? "1" : "0";
    }

    public QName getReceiver() {
        return new QName(SOAP_NAMESPACE, "Server");
    }

    public QName getSender() {
        return new QName(SOAP_NAMESPACE, "Client");
    }

    public QName getMustUnderstand() {
        return new QName(SOAP_NAMESPACE, "MustUnderstand");
    }

    public QName getVersionMismatch() {
        return new QName(SOAP_NAMESPACE, "VersionMismatch");
    }

    public QName getDateEncodingUnknown() {
        // There is no such fault code in soap11
        return null;
    }

    public String getContentType() {
        return "text/xml";
    }
}
