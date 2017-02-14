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
package org.apache.cxf.transport.http;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.transport.http.auth.HttpAuthSupplier;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

/**
 * Holds http conduit configs for the HttpConduitFeature
 */
public class HttpConduitConfig {
    private TLSClientParameters tlsClientParameters;
    private HTTPClientPolicy clientPolicy;
    private HttpAuthSupplier authSupplier;
    private ProxyAuthorizationPolicy proxyAuthorizationPolicy;
    private AuthorizationPolicy authorizationPolicy;

    public void setTlsClientParameters(TLSClientParameters tlsClientParameters) {
        this.tlsClientParameters = tlsClientParameters;
    }
    public HTTPClientPolicy getClientPolicy() {
        return clientPolicy;
    }
    public void setClientPolicy(HTTPClientPolicy clientPolicy) {
        this.clientPolicy = clientPolicy;
    }
    public void setAuthSupplier(HttpAuthSupplier authSupplier) {
        this.authSupplier = authSupplier;
    }
    public void setProxyAuthorizationPolicy(ProxyAuthorizationPolicy proxyAuthorizationPolicy) {
        this.proxyAuthorizationPolicy = proxyAuthorizationPolicy;
    }
    public void setAuthorizationPolicy(AuthorizationPolicy authorizationPolicy) {
        this.authorizationPolicy = authorizationPolicy;
    }

    public void apply(HTTPConduit conduit) {
        if (tlsClientParameters != null) {
            conduit.setTlsClientParameters(tlsClientParameters);
        }
        if (clientPolicy != null) {
            conduit.setClient(clientPolicy);
        }
        if (authSupplier != null) {
            conduit.setAuthSupplier(authSupplier);
        }
        if (proxyAuthorizationPolicy != null) {
            conduit.setProxyAuthorization(proxyAuthorizationPolicy);
        }
        if (authorizationPolicy != null) {
            conduit.setAuthorization(authorizationPolicy);
        }
    }
}
