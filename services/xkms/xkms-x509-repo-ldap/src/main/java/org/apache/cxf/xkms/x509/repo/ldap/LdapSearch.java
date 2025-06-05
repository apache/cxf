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
package org.apache.cxf.xkms.x509.repo.ldap;

import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.CommunicationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.xkms.exception.XKMSException;
import org.apache.cxf.xkms.model.xkms.ResultMajorEnum;
import org.apache.cxf.xkms.model.xkms.ResultMinorEnum;

public class LdapSearch {
    private static final String SECURITY_AUTHENTICATION = "simple";
    private static final Logger LOG = LogUtils.getL7dLogger(LdapSearch.class);

    private String ldapuri;
    private String bindDN;
    private String bindPassword;
    private int numRetries;

    private InitialDirContext dirContext;

    public LdapSearch(String ldapuri, String bindDN, String bindPassword, int numRetries) {
        this.ldapuri = ldapuri;
        this.bindDN = bindDN;
        this.bindPassword = bindPassword;
        this.numRetries = numRetries;
    }

    //CHECKSTYLE:OFF
    private InitialDirContext createInitialContext() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>(5);
        env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(javax.naming.Context.PROVIDER_URL, ldapuri);
        env.put(javax.naming.Context.SECURITY_AUTHENTICATION, SECURITY_AUTHENTICATION);
        env.put(javax.naming.Context.SECURITY_PRINCIPAL, bindDN);
        env.put(javax.naming.Context.SECURITY_CREDENTIALS, bindPassword);
        return new InitialLdapContext(env, null);
    }
    //CHECKSTYLE:ON

    public NamingEnumeration<SearchResult> searchSubTree(String rootEntry, String filter) throws NamingException {
        int retry = 0;
        while (true) {
            try {
                if (this.dirContext == null) {
                    this.dirContext = createInitialContext();
                }
                SearchControls ctls = new SearchControls();
                ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                return dirContext.search(rootEntry, filter, ctls);
            } catch (CommunicationException e) {
                LOG.log(Level.WARNING, "Error in ldap search: " + e.getMessage(), e);
                this.dirContext = null;
                retry++;
                if (retry >= numRetries) {
                    throw new XKMSException(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_RECEIVER,
                                            ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_FAILURE, "Backend failure");
                }
            }
        }
    }

    public Attributes getAttributes(String dn) throws NamingException {
        int retry = 0;
        while (true) {
            try {
                if (this.dirContext == null) {
                    this.dirContext = createInitialContext();
                }
                return dirContext.getAttributes(dn);
            } catch (CommunicationException e) {
                LOG.log(Level.WARNING, "Error in ldap search: " + e.getMessage(), e);
                this.dirContext = null;
                retry++;
                if (retry >= numRetries) {
                    throw new XKMSException(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_RECEIVER,
                                            ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_FAILURE, "Backend failure");
                }
            }
        }
    }

    public Attribute getAttribute(String dn, String attrName) throws NamingException {
        Attribute attr = getAttributes(dn).get(attrName);
        if (attr != null) {
            return attr;
        }
        throw new RuntimeException("Did not find a matching attribute for dn: " + dn
                                   + " attributeName: " + attrName);
    }

    public Attributes findAttributes(String rootDN, String filter) throws NamingException {
        NamingEnumeration<SearchResult> answer = searchSubTree(rootDN, filter);
        if (answer.hasMore()) {
            SearchResult sr = answer.next();
            return sr.getAttributes();
        }
        return null;
    }

    public Attribute findAttribute(String rootDN, String filter, String attrName) throws NamingException {
        Attributes attrs = findAttributes(rootDN, filter);
        if (attrs != null) {
            Attribute attr = attrs.get(attrName);
            if (attr == null) {
                throw new RuntimeException("Did not find a matching attribute for root: " + rootDN
                                           + " filter: " + filter + " attributeName: " + attrName);
            }
            return attr;
        }
        return null;
    }

    public void bind(String dn, Attributes attribs) throws NamingException {
        int retry = 0;
        while (true) {
            try {
                if (this.dirContext == null) {
                    this.dirContext = createInitialContext();
                }
                dirContext.bind(dn, null, attribs);
                return;
            } catch (CommunicationException e) {
                LOG.log(Level.WARNING, "Error in ldap search: " + e.getMessage(), e);
                this.dirContext = null;
                retry++;
                if (retry >= numRetries) {
                    throw new XKMSException(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_RECEIVER,
                                            ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_FAILURE, "Backend failure");
                }
            }
        }
    }

}
