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

package org.apache.cxf.ws.security.policy.interceptors;

import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.model.HttpsToken;

/**
 * 
 */
public class HttpsTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {

    public HttpsTokenInterceptorProvider(QName name) {
        super(Collections.singleton(name));
        this.getOutInterceptors().add(new HttpsTokenOutInterceptor(name));
        this.getOutFaultInterceptors().add(new HttpsTokenOutInterceptor(name));
        this.getInInterceptors().add(new HttpsTokenInInterceptor(name));
        this.getInFaultInterceptors().add(new HttpsTokenInInterceptor(name));
    }
    
    private static Map<String, List<String>> getSetProtocolHeaders(Message message) {
        Map<String, List<String>> headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));        
        if (null == headers) {
            headers = new HashMap<String, List<String>>();
            message.put(Message.PROTOCOL_HEADERS, headers);
        }
        return headers;
    }

    static class HttpsTokenOutInterceptor extends AbstractPhaseInterceptor<Message> {
        QName name;
        public HttpsTokenOutInterceptor(QName n) {
            super(Phase.PREPARE_SEND);
            name = n;
        }
        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais = aim.get(name);
                if (ais == null) {
                    return;
                }
                if (isRequestor(message)) {
                    assertHttps(ais, message);
                } else {
                    //server side should be checked on the way in
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }                    
                }
            }
        }
        private void assertHttps(Collection<AssertionInfo> ais, Message message) {
            for (AssertionInfo ai : ais) {
                HttpsToken token = (HttpsToken)ai.getAssertion();
                
                boolean asserted = true;
                HttpURLConnection connection = 
                    (HttpURLConnection) message.get("http.connection");
                
                Map<String, List<String>> headers = getSetProtocolHeaders(message);
                if (connection instanceof HttpsURLConnection) {
                    HttpsURLConnection https = (HttpsURLConnection)connection;
                    if (token.isRequireClientCertificate()
                        && https.getLocalCertificates().length == 0) {
                        asserted = false;
                    }
                    if (token.isHttpBasicAuthentication()) {
                        List<String> auth = headers.get("Authorization");
                        if (auth == null || auth.size() == 0 
                            || !auth.get(0).startsWith("Basic")) {
                            asserted = false;
                        }
                    }
                    if (token.isHttpDigestAuthentication()) {
                        List<String> auth = headers.get("Authorization");
                        if (auth == null || auth.size() == 0 
                            || !auth.get(0).startsWith("Digest")) {
                            asserted = false;
                        }                        
                    }
                } else {
                    asserted = false;
                }
                ai.setAsserted(asserted);
            }            
        }

    }
    
    static class HttpsTokenInInterceptor extends AbstractPhaseInterceptor<Message> {
        QName name;
        public HttpsTokenInInterceptor(QName n) {
            super(Phase.PRE_STREAM);
            name = n;
        }

        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais = aim.get(name);
                if (ais == null) {
                    return;
                }
                if (!isRequestor(message)) {
                    assertHttps(ais, message);
                } else {
                    //client side should be checked on the way out
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }                    
                }
            }
        }
        private void assertHttps(Collection<AssertionInfo> ais, Message message) {
            for (AssertionInfo ai : ais) {
                boolean asserted = true;
                HttpsToken token = (HttpsToken)ai.getAssertion();
                
                Map<String, List<String>> headers = getSetProtocolHeaders(message);                
                if (token.isHttpBasicAuthentication()) {
                    List<String> auth = headers.get("Authorization");
                    if (auth == null || auth.size() == 0 
                        || !auth.get(0).startsWith("Basic")) {
                        asserted = false;
                    }
                }
                if (token.isHttpDigestAuthentication()) {
                    List<String> auth = headers.get("Authorization");
                    if (auth == null || auth.size() == 0 
                        || !auth.get(0).startsWith("Digest")) {
                        asserted = false;
                    }                        
                }

                TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);                
                if (tlsInfo != null) {
                    if (token.isRequireClientCertificate()
                        && tlsInfo.getPeerCertificates().length == 0) {
                        asserted = false;
                    }
                } else {
                    asserted = false;
                }                
                
                ai.setAsserted(asserted);
            }
        }
    }
}
