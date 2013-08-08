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
package org.apache.cxf.xkms.x509.repo;

import org.apache.cxf.xkms.x509.repo.file.FileCertificateRepo;
import org.apache.cxf.xkms.x509.repo.ldap.LdapCertificateRepo;
import org.apache.cxf.xkms.x509.repo.ldap.LdapSchemaConfig;
import org.apache.cxf.xkms.x509.repo.ldap.LdapSearch;

public final class CertificateRepoFactory {

    private CertificateRepoFactory() {
    }

    public static CertificateRepo createRepository(String type, LdapSearch ldapSearch,
                                                   LdapSchemaConfig ldapSchemaConfig, String rootDN,
                                                   String storageDir) {
        if ("ldap".equals(type)) {
            return new LdapCertificateRepo(ldapSearch, ldapSchemaConfig, rootDN);
        } else if ("file".equals(type)) {
            return new FileCertificateRepo(storageDir);
        } else {
            throw new RuntimeException("Invalid repo type " + type + ". Valid types are file, ldap");
        }
    }

}
