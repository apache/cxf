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

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;

public class LdapClaimsHandler implements ClaimsHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(LdapClaimsHandler.class);

    private LdapTemplate ldap;
    private Map<String, String> claimMapping;
    private String userBaseDn;

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


    public List<URI> getSupportedClaimTypes() {
        List<URI> uriList = new ArrayList<URI>();
        for (String uri : getClaimsLdapAttributeMapping().keySet()) {
            try {
                uriList.add(new URI(uri));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        return uriList;
    }

    public ClaimCollection retrieveClaimValues(Principal principal, RequestClaimCollection claims) {

        String dn = getDnOfPrincipal(principal.getName());

        List<String> searchAttributeList = new ArrayList<String>();
        for (RequestClaim claim : claims) {
            if (getClaimsLdapAttributeMapping().keySet().contains(claim.getClaimType().toString())) {
                searchAttributeList.add(
                    getClaimsLdapAttributeMapping().get(claim.getClaimType().toString())
                );
            } else {
                LOG.warning("Unsupported claim: " + claim.getClaimType());
            }
        }

        String[] searchAttributes = null;
        searchAttributes = searchAttributeList.toArray(new String[] {});

        AttributesMapper mapper = 
            new AttributesMapper() {
                public Object mapFromAttributes(Attributes attrs) throws NamingException {
                    Map<String, String> map = new HashMap<String, String>();
                    NamingEnumeration<? extends Attribute> attrEnum = attrs.getAll();
                    while (attrEnum.hasMore()) {
                        Attribute att = attrEnum.next();
                        map.put(att.getID(), (String)att.get());
                    }
                    return map;
                }
            };
        @SuppressWarnings("unchecked")
        Map<String, String> ldapAttributes = 
            (Map<String, String>) ldap.lookup(dn, searchAttributes, mapper);
        ClaimCollection claimsColl = new ClaimCollection();

        for (RequestClaim claim : claims) {
            URI claimType = claim.getClaimType();
            String ldapAttribute = getClaimsLdapAttributeMapping().get(claimType.toString());
            String claimValue = ldapAttributes.get(ldapAttribute);
            if (claimValue == null) {
                if (!claim.isOptional()) {
                    LOG.warning("Mandatory claim not found in LDAP: " + claim.getClaimType());
                    throw new STSException("Mandatory claim '" + claim.getClaimType() + "' not found");
                } else {
                    LOG.fine("Claim '" + claim.getClaimType() + "' is null");
                }
            } else {
                Claim c = new Claim();
                c.setClaimType(claimType);
                c.setPrincipal(principal);
                c.setValue(claimValue);
                // c.setIssuer(issuer);
                // c.setOriginalIssuer(originalIssuer);
                // c.setNamespace(namespace);
                claimsColl.add(c);
            }
        }

        return claimsColl;
    }


    private String getDnOfPrincipal(String principal) {
        String dn = null;
        AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter("objectclass", "person")).and(new EqualsFilter("cn", principal));

        //find DN of user
        AttributesMapper mapper = 
            new AttributesMapper() {
                public Object mapFromAttributes(Attributes attrs) throws NamingException {
                    return attrs.get("distinguishedName").get();
                }
            };
        @SuppressWarnings("rawtypes")
        List users = 
            ldap.search(this.userBaseDn, filter.toString(), SearchControls.SUBTREE_SCOPE, mapper);
        if (users.size() == 1) {
            dn = (String)users.get(0);
        }
        return dn;
    }

}

