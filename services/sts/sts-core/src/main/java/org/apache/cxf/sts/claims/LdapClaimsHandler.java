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
package org.apache.cxf.sts.claims;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.token.realm.RealmSupport;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;

public class LdapClaimsHandler implements ClaimsHandler, RealmSupport {

    private static final Logger LOG = LogUtils.getL7dLogger(LdapClaimsHandler.class);

    private LdapTemplate ldap;
    private Map<String, String> claimMapping;
    private String userBaseDn;
    private List<String> userBaseDNs;
    private String delimiter;
    private boolean x500FilterEnabled = true;
    private String objectClass = "person";
    private String userNameAttribute = "cn";
    private List<String> supportedRealms;
    private String realm;


    public void setSupportedRealms(List<String> supportedRealms) {
        this.supportedRealms = supportedRealms;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public String getUserNameAttribute() {
        return userNameAttribute;
    }

    public void setUserNameAttribute(String userNameAttribute) {
        this.userNameAttribute = userNameAttribute;
    }

    public void setLdapTemplate(LdapTemplate ldapTemplate) {
        this.ldap = ldapTemplate;
    }

    public LdapTemplate getLdapTemplate() {
        return ldap;
    }

    public void setClaimsLdapAttributeMapping(Map<String, String> ldapClaimMapping) {
        this.claimMapping = ldapClaimMapping;
    }

    public Map<String, String> getClaimsLdapAttributeMapping() {
        return claimMapping;
    }

    public void setUserBaseDN(String userBaseDN) {
        this.userBaseDn = userBaseDN;
    }

    public String getUserBaseDN() {
        return userBaseDn;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public boolean isX500FilterEnabled() {
        return x500FilterEnabled;
    }

    public void setX500FilterEnabled(boolean x500FilterEnabled) {
        this.x500FilterEnabled = x500FilterEnabled;
    }


    public List<String> getSupportedClaimTypes() {
        return new ArrayList<>(getClaimsLdapAttributeMapping().keySet());
    }

    public ProcessedClaimCollection retrieveClaimValues(
            ClaimCollection claims, ClaimsParameters parameters) {
        final String user;
        boolean useLdapLookup = false;

        Principal principal = parameters.getPrincipal();
        if (principal instanceof KerberosPrincipal) {
            KerberosPrincipal kp = (KerberosPrincipal)principal;
            StringTokenizer st = new StringTokenizer(kp.getName(), "@");
            user = st.nextToken();
        } else if (principal instanceof X500Principal) {
            X500Principal x500p = (X500Principal)principal;
            LOG.warning("Unsupported principal type X500: " + x500p.getName());
            return new ProcessedClaimCollection();
        } else if (principal != null) {
            user = principal.getName();
            if (user == null) {
                LOG.warning("User must not be null");
                return new ProcessedClaimCollection();
            }
            useLdapLookup = LdapUtils.isDN(user);

        } else {
            LOG.warning("Principal is null");
            return new ProcessedClaimCollection();
        }

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Retrieve claims for user " + user);
        }

        Map<String, Attribute> ldapAttributes = null;
        if (useLdapLookup) {
            AttributesMapper<Map<String, Attribute>> mapper =
                new AttributesMapper<Map<String, Attribute>>() {
                    public Map<String, Attribute> mapFromAttributes(Attributes attrs) throws NamingException {
                        Map<String, Attribute> map = new HashMap<>();
                        NamingEnumeration<? extends Attribute> attrEnum = attrs.getAll();
                        while (attrEnum.hasMore()) {
                            Attribute att = attrEnum.next();
                            map.put(att.getID(), att);
                        }
                        return map;
                    }
                };

            ldapAttributes = ldap.lookup(user, mapper);
        } else {
            List<String> searchAttributeList = new ArrayList<>();
            for (Claim claim : claims) {
                String claimType = claim.getClaimType();
                if (getClaimsLdapAttributeMapping().keySet().contains(claimType)) {
                    searchAttributeList.add(
                        getClaimsLdapAttributeMapping().get(claimType)
                    );
                } else {
                    if (LOG.isLoggable(Level.FINER)) {
                        LOG.finer("Unsupported claim: " + claimType);
                    }
                }
            }

            String[] searchAttributes = searchAttributeList.toArray(new String[0]);

            if (this.userBaseDn != null) {
                ldapAttributes = LdapUtils.getAttributesOfEntry(ldap, this.userBaseDn, this.getObjectClass(), this
                    .getUserNameAttribute(), user, searchAttributes);
            }
            if (this.userBaseDNs != null && (ldapAttributes == null || ldapAttributes.isEmpty())) {
                for (String userBase : userBaseDNs) {
                    ldapAttributes = LdapUtils.getAttributesOfEntry(ldap, userBase, this.getObjectClass(), this
                        .getUserNameAttribute(), user, searchAttributes);
                    if (ldapAttributes != null && !ldapAttributes.isEmpty()) {
                        break; // User found
                    }
                }
            }
        }

        if (ldapAttributes == null || ldapAttributes.isEmpty()) {
            //No result
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info("User '" + user + "' not found");
            }
            return new ProcessedClaimCollection();
        }

        ProcessedClaimCollection claimsColl = new ProcessedClaimCollection();
        for (Claim claim : claims) {
            ProcessedClaim c = processClaim(claim, ldapAttributes, principal);
            if (c != null) {
                // c.setIssuer(issuer);
                // c.setOriginalIssuer(originalIssuer);
                // c.setNamespace(namespace);
                claimsColl.add(c);
            }
        }

        return claimsColl;
    }

    protected ProcessedClaim processClaim(Claim claim, Map<String, Attribute> ldapAttributes, Principal principal) {
        String claimType = claim.getClaimType();
        String ldapAttribute = getClaimsLdapAttributeMapping().get(claimType);
        Attribute attr = ldapAttributes.get(ldapAttribute);
        if (attr == null) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("Claim '" + claim.getClaimType() + "' is null");
            }
            return null;
        }

        ProcessedClaim c = new ProcessedClaim();
        c.setClaimType(claimType);
        c.setPrincipal(principal);

        try {
            NamingEnumeration<?> list = attr.getAll();
            while (list.hasMore()) {
                Object obj = list.next();
                if (obj instanceof String) {
                    String itemValue = (String)obj;
                    if (this.isX500FilterEnabled()) {
                        try {
                            X500Principal x500p = new X500Principal(itemValue);
                            itemValue = x500p.getName();
                            int index = itemValue.indexOf('=');
                            itemValue = itemValue.substring(index + 1, itemValue.indexOf(',', index));
                        } catch (Throwable ex) {
                            //Ignore, not X500 compliant thus use the whole string as the value
                        }
                    }
                    if (delimiter != null) {
                        StringBuilder claimValue = new StringBuilder();
                        claimValue.append(itemValue);
                        if (list.hasMore()) {
                            claimValue.append(this.getDelimiter());
                        } else if (claimValue.length() > 0) {
                            c.addValue(claimValue.toString());
                        }
                    } else {
                        c.addValue(itemValue);
                    }
                } else if (obj instanceof byte[]) {
                    // Just store byte[]
                    c.addValue(obj);
                } else {
                    LOG.warning("LDAP attribute '" + ldapAttribute
                            + "' has got an unsupported value type");
                    break;
                }
            }
        } catch (NamingException ex) {
            LOG.warning("Failed to read value of LDAP attribute '" + ldapAttribute + "'");
        }
        return c;
    }

    @Override
    public List<String> getSupportedRealms() {
        return supportedRealms;
    }

    @Override
    public String getHandlerRealm() {
        return realm;
    }

    public List<String> getUserBaseDNs() {
        return userBaseDNs;
    }

    public void setUserBaseDNs(List<String> userBaseDNs) {
        this.userBaseDNs = userBaseDNs;
    }

}
