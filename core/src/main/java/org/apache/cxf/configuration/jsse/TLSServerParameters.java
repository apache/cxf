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

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.configuration.security.ClientAuthentication;

/**
 * This class extends {@link TLSParameterBase} with service-specific
 * SSL/TLS parameters.
 *
 */
public class TLSServerParameters extends TLSParameterBase {
    ClientAuthentication clientAuthentication;
    List<String> excludeProtocols = new ArrayList<>();
    List<String> includeProtocols = new ArrayList<>();
    boolean sniHostCheck;

    /**
     * This parameter configures the server side to request and/or
     * require client authentication.
     */
    public final void setClientAuthentication(ClientAuthentication clientAuth) {
        clientAuthentication = clientAuth;
    }

    /**
     * This parameter retrieves the client authentication settings.
     */
    public ClientAuthentication getClientAuthentication() {
        return clientAuthentication;
    }

    /**
     * This parameter sets the protocol list to exclude.
     */
    public final void setExcludeProtocols(List<String> protocols) {
        excludeProtocols = protocols;
    }

    /**
     * Returns the protocols to exclude that are associated with this endpoint.
     */
    public List<String> getExcludeProtocols() {
        if (excludeProtocols == null) {
            excludeProtocols = new ArrayList<>();
        }
        return excludeProtocols;
    }

    /**
     * This parameter sets the protocol list to include.
     */
    public final void setIncludeProtocols(List<String> protocols) {
        includeProtocols = protocols;
    }

    /**
     * Returns the protocols to include that are associated with this endpoint.
     */
    public List<String> getIncludeProtocols() {
        if (includeProtocols == null) {
            includeProtocols = new ArrayList<>();
        }
        return includeProtocols;
    }

    /**
     * Returns if the SNI host name must match
     */
    public boolean isSniHostCheck() {
        return sniHostCheck;
    }

    /**
     * @param sniHostCheck if the SNI host name must match
     */
    public void setSniHostCheck(boolean sniHostCheck) {
        this.sniHostCheck = sniHostCheck;
    }

    public static String[] getPreferredServerProtocols() {
        return DEFAULT_HTTPS_PROTOCOLS.toArray(new String [0]);
    }
}
