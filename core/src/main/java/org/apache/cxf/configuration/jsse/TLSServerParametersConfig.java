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
package org.apache.cxf.configuration.jsse;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.security.TLSServerParametersType;

/**
 * This class is used by Spring Config to convert the TLSServerParameters
 * JAXB generated type into programmatic TLS Server Parameters for the
 * configuration of the http-destination.
 */
@NoJSR250Annotations
public class TLSServerParametersConfig 
    extends TLSServerParameters {

    public TLSServerParametersConfig(TLSServerParametersType params) 
        throws GeneralSecurityException,
               IOException {
        
        TLSServerParametersTypeInternal iparams = null;
        if (params instanceof TLSServerParametersTypeInternal) {
            iparams = (TLSServerParametersTypeInternal)params;
        }
        
        if (params.isSetSecureSocketProtocol()) {
            this.setSecureSocketProtocol(params.getSecureSocketProtocol());
        }
        if (params.isSetCipherSuitesFilter()) {
            this.setCipherSuitesFilter(params.getCipherSuitesFilter());
        }
        if (params.isSetCipherSuites()) {
            this.setCipherSuites(params.getCipherSuites().getCipherSuite());
        }
        if (params.isSetJsseProvider()) {
            this.setJsseProvider(params.getJsseProvider());
        }
        if (params.isSetSecureRandomParameters()) {
            this.setSecureRandom(
                TLSParameterJaxBUtils.getSecureRandom(
                        params.getSecureRandomParameters()));
        }
        if (params.isSetClientAuthentication()) {
            this.setClientAuthentication(params.getClientAuthentication());
        }
        if (params.isSetKeyManagers()) {
            this.setKeyManagers(
                TLSParameterJaxBUtils.getKeyManagers(params.getKeyManagers()));
        }
        if (params.isSetTrustManagers()) {
            this.setTrustManagers(
                TLSParameterJaxBUtils.getTrustManagers(
                        params.getTrustManagers()));
        }
        if (params.isSetCertConstraints()) {
            this.setCertConstraints(params.getCertConstraints());
        }
        if (params.isSetCertAlias()) {
            this.setCertAlias(params.getCertAlias());
        }
        if (iparams != null && iparams.isSetKeyManagersRef()) {
            this.setKeyManagers(iparams.getKeyManagersRef());
        }
        if (iparams != null && iparams.isSetTrustManagersRef()) {
            this.setTrustManagers(iparams.getTrustManagersRef());
        }
    }

    public static class TLSServerParametersTypeInternal extends TLSServerParametersType {
        private KeyManager[] keyManagersRef;
        private TrustManager[] trustManagersRef;

        public KeyManager[] getKeyManagersRef() {
            return keyManagersRef;
        }

        public void setKeyManagersRef(KeyManager[] keyManagersRef) {
            this.keyManagersRef = keyManagersRef;
        }
        
        public boolean isSetKeyManagersRef() {
            return this.keyManagersRef != null;
        }

        public TrustManager[] getTrustManagersRef() {
            return trustManagersRef;
        }

        public void setTrustManagersRef(TrustManager[] trustManagersRef) {
            this.trustManagersRef = trustManagersRef;
        }
        
        public boolean isSetTrustManagersRef() {
            return this.trustManagersRef != null;
        }

    }
    
}
