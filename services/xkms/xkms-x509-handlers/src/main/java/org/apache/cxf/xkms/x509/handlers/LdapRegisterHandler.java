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
package org.apache.cxf.xkms.x509.handlers;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.x509.utils.X509Utils;

public class LdapRegisterHandler extends AbstractX509RegisterHandler {
    private static final String ATTR_OBJECT_CLASS = "objectClass";

    private final LdapSearch ldapSearch;
    private final LdapSchemaConfig ldapConfig;
    private final String rootDN;

    public LdapRegisterHandler(LdapSearch ldapSearch, LdapSchemaConfig ldapConfig, String rootDN)
        throws CertificateException {
        this.ldapSearch = ldapSearch;
        this.ldapConfig = ldapConfig;
        this.rootDN = rootDN;
    }

    @Override
    public void saveCertificate(X509Certificate cert, UseKeyWithType id) {
        Attributes attribs = new BasicAttributes();
        attribs.put(new BasicAttribute(ATTR_OBJECT_CLASS, ldapConfig.getCertObjectClass()));
        attribs.put(new BasicAttribute(ldapConfig.getAttrUID(), cert.getSubjectX500Principal().getName()));
        attribs.put(new BasicAttribute(ldapConfig.getAttrIssuerID(), cert.getIssuerX500Principal().getName()));
        attribs.put(new BasicAttribute(ldapConfig.getAttrSerialNumber(), cert.getSerialNumber().toString(16)));
        addConstantAttributes(ldapConfig.getConstAttrNamesCSV(), ldapConfig.getConstAttrValuesCSV(), attribs);
        try {
            attribs.put(new BasicAttribute(ldapConfig.getAttrCrtBinary(), cert.getEncoded()));
            String dn = X509Utils.getDN(id.getApplication(), id.getIdentifier(),
                                        ldapConfig.getServiceCertRDNTemplate(), rootDN);
            ldapSearch.bind(dn, attribs);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    
    private void addConstantAttributes(String names, String values, Attributes attribs) {
        String[] arrNames = names.split(",");
        String[] arrValues = values.split(",");
        if (arrNames.length != arrValues.length) {
            throw new IllegalArgumentException(
                      String.format("Inconsintent constant attributes: %s; %s",  names, values));
        }
        for (int i = 0; i < arrNames.length; i++) {
            attribs.put(new BasicAttribute(arrNames[i], arrValues[i]));
        }
    }

}
