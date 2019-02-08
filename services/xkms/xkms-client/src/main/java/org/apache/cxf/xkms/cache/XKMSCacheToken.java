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

package org.apache.cxf.xkms.cache;

import java.io.Serializable;
import java.security.cert.X509Certificate;

public class XKMSCacheToken implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 7097585680022947024L;
    private X509Certificate x509Certificate;
    private boolean xkmsValidated;

    public XKMSCacheToken() {
        //
    }

    public XKMSCacheToken(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public boolean isXkmsValidated() {
        return xkmsValidated;
    }

    public void setXkmsValidated(boolean xkmsValidated) {
        this.xkmsValidated = xkmsValidated;
    }

}