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
    
    private static Soap12 instance = new Soap12();

    private final double version = 1.2;

    private final String namespace = SOAP_NAMESPACE;

    private final String prefix = "soap";

    private final String noneRole = namespace + "/role/none";

    private final String ultimateReceiverRole = namespace + "/role/ultimateReceiver";

    private final String nextRole = namespace + "/role/next";

    private final String soapEncodingStyle = "http://www.w3.org/2003/05/soap-encoding";

    private final QName envelope = new QName(namespace, "Envelope", prefix);

    private final QName header = new QName(namespace, "Header", prefix);

    private final QName body = new QName(namespace, "Body", prefix);

    private final QName fault = new QName(namespace, "Fault", prefix);

    private Soap12() {
       // Singleton 
    }
    
    public static Soap12 getInstance() {
        return instance;
    }
    public String getBindingId() {
        return SoapBindingConstants.SOAP12_BINDING_ID;
    }

    public double getVersion() {
        return version;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPrefix() {
        return prefix;
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
        return soapEncodingStyle;
    }

    // Role URIs
    // -------------------------------------------------------------------------
    public String getNoneRole() {
        return noneRole;
    }

    public String getUltimateReceiverRole() {
        return ultimateReceiverRole;
    }

    public String getNextRole() {
        return nextRole;
    }
    
    public String getAttrNameRole() {
        return "role";
    }

    public String getAttrNameMustUnderstand() {
        return "mustUnderstand";
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
