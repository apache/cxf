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
package org.apache.cxf.interceptor.security.callback;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

public class CertKeyToUserNameMapper implements CertificateToNameMapper {
    private String key;

    /**
     * Returns Subject DN from X509Certificate
     *
     * @param cert
     * @return Subject DN as a user name
     */
    @Override
    public String getUserName(Certificate cert) {
        X509Certificate certificate = (X509Certificate) cert;
        String dn = certificate.getSubjectDN().getName();
        LdapName ldapDn = getLdapName(dn);

        if (key == null) {
            throw new IllegalArgumentException("Must set a key");
        }

        for (Rdn rdn : ldapDn.getRdns()) {
            if (key.equalsIgnoreCase(rdn.getType())) {
                return (String)rdn.getValue();
            }
        }

        throw new IllegalArgumentException("No " + key + " key found in certificate DN: " + dn);
    }

    private LdapName getLdapName(String dn) {
        try {
            return new LdapName(dn);
        } catch (InvalidNameException e) {
            throw new IllegalArgumentException("Invalid DN", e);
        }
    }

    public void setKey(String key) {
        this.key = key;
    }

}
