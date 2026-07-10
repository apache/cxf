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
package org.apache.cxf.transport.http_jetty;

import javax.net.ssl.SSLEngine;

import org.eclipse.jetty.alpn.server.ALPNServerConnection;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.ssl.ALPNProcessor;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.io.ssl.SslHandshakeListener;

/**
 * Jetty {@link ALPNProcessor.Server} for BouncyCastle JSSE ({@code bctls-jdk18on}).
 *
 * <p>Jetty's built-in {@code JDK9ServerALPNProcessor} only matches engines whose
 * module is {@code java.base}, so it rejects BC JSSE's {@code ProvSSLEngine} and
 * throws {@code IllegalStateException: No suitable ALPNProcessor}.  This processor
 * bridges the gap: it claims BC JSSE engines and wires up the same
 * {@link SSLEngine#setHandshakeApplicationProtocolSelector} callback that the
 * built-in processor uses, delegating to {@link ALPNServerConnection#select}.
 *
 * <p>A {@link SslHandshakeListener} is also registered so that if the client does not
 * advertise any ALPN protocols, {@link ALPNServerConnection#unsupported()} is called
 * on handshake success to fall back to the server's default protocol (HTTP/1.1).
 *
 * <p>Registered via {@code META-INF/services/org.eclipse.jetty.io.ssl.ALPNProcessor$Server}.
 */
public class BCJsseServerALPNProcessor implements ALPNProcessor.Server {

    @Override
    public boolean appliesTo(SSLEngine sslEngine) {
        return sslEngine.getClass().getName().startsWith("org.bouncycastle.");
    }

    @Override
    public void configure(SSLEngine sslEngine, Connection connection) {
        ALPNServerConnection alpnConn = (ALPNServerConnection) connection;

        // Called when the client includes an ALPN extension in its ClientHello.
        sslEngine.setHandshakeApplicationProtocolSelector((engine, protocols) -> {
            try {
                alpnConn.select(protocols);
                return alpnConn.getProtocol();
            } catch (Throwable t) {
                return null;
            }
        });

        // When the client sends no ALPN extension the selector above is never invoked,
        // leaving getProtocol() null.  The SslHandshakeListener fires on handshake
        // completion and calls unsupported() to fall back to the default protocol.
        SslConnection.SslEndPoint sslEndPoint = (SslConnection.SslEndPoint) alpnConn.getEndPoint();
        sslEndPoint.getSslConnection().addHandshakeListener(new SslHandshakeListener() {
            @Override
            public void handshakeSucceeded(Event event) {
                if (alpnConn.getProtocol() == null) {
                    alpnConn.unsupported();
                }
            }
        });
    }
}
