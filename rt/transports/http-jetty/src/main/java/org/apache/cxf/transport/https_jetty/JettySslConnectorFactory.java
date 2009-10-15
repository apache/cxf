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

package org.apache.cxf.transport.https_jetty;

import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.transport.http_jetty.JettyConnectorFactory;
import org.mortbay.jetty.AbstractConnector;

/**
 * This class wraps the JettyConnectorFactory and will create 
 * TLS enabled acceptors.
 */
public final class JettySslConnectorFactory implements JettyConnectorFactory {
    private static final long serialVersionUID = 1L;
    
    TLSServerParameters tlsServerParameters;
    
    public JettySslConnectorFactory(TLSServerParameters params) {
        tlsServerParameters = params;
    }
    /**
     * Create a Listener.
     * 
     * @param port the listen port
     */
    public AbstractConnector createConnector(int port) {
        return createConnector(null, port);
    }

    /**
     * Create a Listener.
     * 
     * @param host the host to bind to.  IP address or hostname is allowed. null to bind to all hosts.
     * @param port the listen port
     */
    public AbstractConnector createConnector(String host, int port) {
        assert tlsServerParameters != null;
        
        CXFJettySslSocketConnector secureConnector = 
            new CXFJettySslSocketConnector();
        if (host != null) {
            secureConnector.setHost(host);
        }
        secureConnector.setPort(port);
        decorateCXFJettySslSocketConnector(secureConnector);
        return secureConnector;
    }
    
    /**
     * This method sets the security properties for the CXF extension
     * of the JettySslConnector.
     */
    private void decorateCXFJettySslSocketConnector(
            CXFJettySslSocketConnector con
    ) {
        con.setKeyManagers(tlsServerParameters.getKeyManagers());
        con.setTrustManagers(tlsServerParameters.getTrustManagers());
        con.setSecureRandom(tlsServerParameters.getSecureRandom());
        con.setClientAuthentication(
                tlsServerParameters.getClientAuthentication());
        con.setProtocol(tlsServerParameters.getSecureSocketProtocol());
        con.setProvider(tlsServerParameters.getJsseProvider());
        con.setCipherSuites(tlsServerParameters.getCipherSuites());
        con.setCipherSuitesFilter(tlsServerParameters.getCipherSuitesFilter());
    }
    
}
