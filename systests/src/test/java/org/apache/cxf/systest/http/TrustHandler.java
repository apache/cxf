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

package org.apache.cxf.systest.http;

import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HttpURLConnectionInfo;
import org.apache.cxf.transport.http.MessageTrustDecider;
import org.apache.cxf.transport.http.URLConnectionInfo;
import org.apache.cxf.transport.http.UntrustedURLConnectionIOException;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;

public class TrustHandler
    extends MessageTrustDecider {
    
    public TrustHandler() {
        // Set the logical name.
        super("The System Test Trust Decider");
    }
    
    public void establishTrust(
        String                  conduitName,
        URLConnectionInfo       connectionInfo,
        Message                 message
    ) throws UntrustedURLConnectionIOException {
        System.out.println("Trust decision for conduit: "
                + conduitName + " and " 
                + connectionInfo.getURL());
        if (connectionInfo instanceof HttpURLConnectionInfo) {
            HttpURLConnectionInfo c = (HttpURLConnectionInfo) connectionInfo;
            System.out.println("Http method: " 
                    + c.getHttpRequestMethod() + " on " + c.getURL());
        }
        if (connectionInfo instanceof HttpsURLConnectionInfo) {
            HttpsURLConnectionInfo c = (HttpsURLConnectionInfo) connectionInfo;
            System.out.println("TLS Connection to: " + c.getURL());
            System.out.println("Enabled Cipher: " + c.getEnabledCipherSuite());
            System.out.println("Local Principal: " + c.getLocalPrincipal());
            System.out.println("Peer Principal: " + c.getPeerPrincipal());
        }
        //throw new UntrustedURLConnectionIOException("No Way Jose"); 
    }
}
