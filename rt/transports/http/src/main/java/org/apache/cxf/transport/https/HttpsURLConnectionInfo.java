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
 
package org.apache.cxf.transport.https;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.security.Principal;
import java.security.cert.Certificate;

import javax.net.ssl.HttpsURLConnection;

import org.apache.cxf.transport.http.HttpURLConnectionInfo;

/**
 * This class holds information about the HttpsURLConnection. This
 * class should be used when the getURL().getProtocol() is "https".
 */
public class HttpsURLConnectionInfo extends HttpURLConnectionInfo {

    /**
     * This field contains the cipherSuite enabled in the 
     * HTTPS URLconnection.
     */
    protected String enabledCipherSuite;
    
    /**
     * This field contains the certificates that were used to
     * authenticate the connection to the peer.
     */
    protected Certificate[] localCertificates;
    
    /**
     * This field contains the Principal that authenticated to the
     * peer.
     */
    protected Principal localPrincipal;
    
    /**
     * This field contains the certificates the server presented
     * to authenticate.
     */
    protected Certificate[] serverCertificates;
    
    /**
     * This field contains the Principal that represents the 
     * authenticated peer.
     */
    protected Principal peerPrincipal;

    
    /**
     * This constructor is used to create the info object
     * representing the this HttpsURLConnection. Connection parameter is 
     * of supertype HttpURLConnection, which allows internal cast to 
     * potentially divergent subtype (Https) implementations.
     */
    HttpsURLConnectionInfo(HttpURLConnection connection)
        throws IOException {
        super(connection);
        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection conn = (HttpsURLConnection) connection;
            enabledCipherSuite = conn.getCipherSuite();
            localCertificates  = conn.getLocalCertificates();
            localPrincipal     = conn.getLocalPrincipal();
            serverCertificates = conn.getServerCertificates();
            peerPrincipal      = conn.getPeerPrincipal();
        } else {
            Exception ex = null;
            try {
                Method method = null;
                method = connection.getClass().getMethod("getCipherSuite", (Class[]) null);
                enabledCipherSuite = (String) method.invoke(connection, (Object[]) null);
                method = connection.getClass().getMethod("getLocalCertificates", (Class[]) null);
                localCertificates = (Certificate[]) method.invoke(connection, (Object[]) null);
                method = connection.getClass().getMethod("getServerCertificates", (Class[]) null);
                serverCertificates = (Certificate[]) method.invoke(connection, (Object[]) null);
                
                //TODO Obtain localPrincipal and peerPrincipal using the com.sun.net.ssl api
            } catch (Exception e) {
                ex = e;
            } finally {
                if (ex != null) {
                    if (ex instanceof IOException) {
                        throw (IOException) ex;
                    }
                    IOException ioe = new IOException("Error constructing HttpsURLConnectionInfo "
                                                      + "for connection class "
                                                      + connection.getClass().getName());
                    ioe.initCause(ex);
                    throw ioe;
                    
                }
            }
        }
    }

    /**
     * This method returns the cipher suite employed in this
     * HttpsURLConnection.
     */
    public String getEnabledCipherSuite() {
        return enabledCipherSuite;
    }
    
    /**
     * This method returns the certificates that were used to
     * authenticate to the peer.
     */
    public Certificate[] getLocalCertificates() {
        return localCertificates;
    }
    
    /**
     * This method returns the Princpal that authenticated to
     * the peer.
     */
    public Principal getLocalPrincipal() {
        return localPrincipal;
    }
    
    /**
     * This method returns the certificates presented by the
     * peer for authentication.
     */
    public Certificate[] getServerCertificates() {
        return serverCertificates;
    }
    
    /**
     * This method returns the Principal that represents the
     * authenticated peer.
     */
    public Principal getPeerPrincipal() {
        return peerPrincipal;
    }
}
