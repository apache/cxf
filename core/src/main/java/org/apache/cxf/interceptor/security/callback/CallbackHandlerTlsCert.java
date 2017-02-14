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
package org.apache.cxf.interceptor.security.callback;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.interceptor.security.NamePasswordCallbackHandler;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.transport.TLSSessionInfo;

public class CallbackHandlerTlsCert implements CallbackHandlerProvider {
    private CertificateToNameMapper certMapper;
    private NameToPasswordMapper nameToPasswordMapper;
    private String fixedPassword;

    public CallbackHandlerTlsCert() {
        // By default use subjectDN as userName
        this.certMapper = new CertificateToNameMapper() {
            public String getUserName(Certificate cert) {
                return ((X509Certificate)cert).getSubjectDN().getName();
            }
        };
        // By default use fixed password
        this.nameToPasswordMapper = new NameToPasswordMapper() {
            public String getPassword(String userName) {
                return fixedPassword;
            }
        };
    }

    @Override
    public CallbackHandler create(Message message) {
        TLSSessionInfo tlsSession = message.get(TLSSessionInfo.class);
        if (tlsSession == null) {
            return null;
        }
        Certificate cert = getCertificate(message);
        String name = certMapper.getUserName(cert);
        String password = nameToPasswordMapper.getPassword(name);
        return new NamePasswordCallbackHandler(name, password);
    }

    /**
     * Extracts certificate from message, expecting to find TLSSessionInfo inside.
     *
     * @param message
     */
    private Certificate getCertificate(Message message) {
        TLSSessionInfo tlsSessionInfo = message.get(TLSSessionInfo.class);
        if (tlsSessionInfo == null) {
            throw new SecurityException("Not TLS connection");
        }

        Certificate[] certificates = tlsSessionInfo.getPeerCertificates();
        if (certificates == null || certificates.length == 0) {
            throw new SecurityException("No certificate found");
        }

        // Due to RFC5246, senders certificates always comes 1st
        return certificates[0];
    }

    public void setCertMapper(CertificateToNameMapper certMapper) {
        this.certMapper = certMapper;
    }

    public void setFixedPassword(String fixedPassword) {
        this.fixedPassword = fixedPassword;
    }

    public void setNameToPasswordMapper(NameToPasswordMapper nameToPasswordMapper) {
        this.nameToPasswordMapper = nameToPasswordMapper;
    }

}
