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
package org.apache.cxf.xkms.x509.validator;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.xkms.exception.XKMSRequestException;
import org.apache.cxf.xkms.model.xkms.ValidateRequestType;
import org.apache.cxf.xkms.model.xmldsig.X509DataType;
import org.apache.cxf.xkms.x509.utils.X509Utils;

public final class ValidateRequestParser {

    private static final Logger LOG = LogUtils.getL7dLogger(ValidateRequestParser.class);

    private ValidateRequestParser() {
    }

    /**
     * Extract the X509 certificates from ValidateRequestType and return them as list.
     */
    public static List<X509Certificate> parse(ValidateRequestType request) {
        List<X509Certificate> certs = new ArrayList<>();

        if (request.getQueryKeyBinding() != null && request.getQueryKeyBinding().getKeyInfo() != null) {
            List<Object> keyInfoContent = request.getQueryKeyBinding().getKeyInfo().getContent();
            for (Object keyInfoObject : keyInfoContent) {
                if (keyInfoObject instanceof JAXBElement<?>) {
                    JAXBElement<?> dataInstance = (JAXBElement<?>) keyInfoObject;
                    if (X509Utils.X509_DATA.equals(dataInstance.getName())) {
                        try {
                            X509Utils.parseX509Data((X509DataType) dataInstance.getValue(), certs);
                            LOG.fine("Extracted " + certs.size() + " certificates from ValidateRequest");
                        } catch (CertificateException e) {
                            throw new XKMSRequestException("Corrupted X509 certificate in request: " + e.getMessage(),
                                    e);
                        }
                    }
                }
            }
        }
        return certs;
    }

}
