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
package org.apache.cxf.spring.boot.autoconfigure.ssl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Client-side SSL integration properties for CXF when Spring Boot SslBundles are present.
 * bundles are applied once when an HTTPConduit is configured.
 */
@ConfigurationProperties("cxf.client.ssl")
public class CxfClientSslProperties {
    private boolean enabled;
    //Name of the Spring SSL bundle to use for outbound CXF clients. */
    private String defaultBundle = "cxf-client";
    // Convenience flag for tests
    private Boolean disableCnCheck = Boolean.FALSE;
    private List<CxfClientSslBundle> cxfClientSslBundles = new ArrayList<>();

    public static class CxfClientSslBundle {
        private String name;
        private String address;
        private String bundle;
        private String protocol;
        private List<String> cipherSuites;
        private Boolean disableCnCheck = Boolean.FALSE;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getBundle() {
            return bundle;
        }

        public void setBundle(String bundle) {
            this.bundle = bundle;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public List<String> getCipherSuites() {
            return cipherSuites;
        }

        public void setCipherSuites(List<String> cipherSuites) {
            this.cipherSuites = cipherSuites;
        }
        
        public Boolean getDisableCnCheck() {
            return disableCnCheck;
        }

        public void setDisableCnCheck(Boolean disableCnCheck) {
            this.disableCnCheck = disableCnCheck;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultBundle() {
        return defaultBundle;
    }

    public void setDefaultBundle(String bundle) {
        this.defaultBundle = bundle;
    }
    
    public Boolean getDisableCnCheck() {
        return disableCnCheck;
    }

    public void setDisableCnCheck(Boolean disableCnCheck) {
        this.disableCnCheck = disableCnCheck;
    }

    public List<CxfClientSslBundle> getCxfClientSslBundles() {
        return cxfClientSslBundles;
    }

    public void setCxfClientSslBundles(List<CxfClientSslBundle> clientSslBundles) {
        this.cxfClientSslBundles = clientSslBundles;
    }
}
