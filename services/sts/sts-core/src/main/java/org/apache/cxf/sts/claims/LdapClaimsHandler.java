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
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
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
    private String delimiter = ";";
    private boolean x500FilterEnabled = true;
    private String objectClass = "person";
    private String userNameAttribute = "cn";
    
    
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

    public ClaimCollection retrieveClaimValues(
            Principal principal, RequestClaimCollection claims, WebServiceContext context, String realm) {

        String user = null;
        if (principal instanceof KerberosPrincipal) {
            KerberosPrincipal kp = (KerberosPrincipal)principal;
            StringTokenizer st = new StringTokenizer(kp.getName(), "@");
            user = st.nextToken();
        } else if (principal instanceof X500Principal) {
            X500Principal x500p = (X500Principal)principal;
            LOG.warning("Unsupported principal type X500: " + x500p.getName());
            return new ClaimCollection();
        } else if (principal != null) {
            user = principal.getName();
        } else {
            //[TODO] if onbehalfof -> principal == null
            LOG.info("Principal is null");
            return new ClaimCollection();
        }
        
        if (user == null) {
            LOG.warning("User must not be null");
            return new ClaimCollection();
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Retrieve claims for user " + user);
            }
        }
        
        AndFilter filter = new AndFilter();
        filter.and(
                new EqualsFilter("objectclass", this.getObjectClass())).and(
                        new EqualsFilter(this.getUserNameAttribute(), user));

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
                    Map<String, Attribute> map = new HashMap<String, Attribute>();
                    NamingEnumeration<? extends Attribute> attrEnum = attrs.getAll();
                    while (attrEnum.hasMore()) {
                        Attribute att = attrEnum.next();
                        map.put(att.getID(), att);
                    }
                    return map;
                }
            };
        
        
        List<?> result = ldap.search((this.userBaseDn == null) ? "" : this.userBaseDn, filter.toString(),
                SearchControls.SUBTREE_SCOPE, searchAttributes, mapper);
      
        Map<String, Attribute> ldapAttributes = null;
        if (result != null && result.size() > 0) {
            ldapAttributes = CastUtils.cast((Map<?, ?>)result.get(0));
        }
        
        ClaimCollection claimsColl = new ClaimCollection();

        for (RequestClaim claim : claims) {
            URI claimType = claim.getClaimType();
            String ldapAttribute = getClaimsLdapAttributeMapping().get(claimType.toString());
            Attribute attr = ldapAttributes.get(ldapAttribute);
            if (attr == null) {
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

                StringBuilder claimValue = new StringBuilder();
                try {
                    NamingEnumeration<?> list = (NamingEnumeration<?>)attr.getAll();
                    while (list.hasMore()) {
                        Object obj = list.next();
                        if (!(obj instanceof String)) {
                            LOG.warning("LDAP attribute '" + ldapAttribute 
                                    + "' has got an unsupported value type");
                            break;
                        }
                        String itemValue = (String)obj;
                        if (this.isX500FilterEnabled()) {
                            try {
                                X500Principal x500p = new X500Principal(itemValue);
                                itemValue = x500p.getName();
                                int index = itemValue.indexOf('=');
                                itemValue = itemValue.substring(index + 1, itemValue.indexOf(',', index));
                            } catch (Exception ex) {
                                //Ignore, not X500 compliant thus use the whole string as the value
                            }
                        }
                        claimValue.append(itemValue);
                        if (list.hasMore()) {
                            claimValue.append(this.getDelimiter());
                        }
                    }
                } catch (NamingException ex) {
                    LOG.warning("Failed to read value of LDAP attribute '" + ldapAttribute + "'");
                }
                
                c.setValue(claimValue.toString());
                // c.setIssuer(issuer);
                // c.setOriginalIssuer(originalIssuer);
                // c.setNamespace(namespace);
                claimsColl.add(c);
            }
        }
        
        return claimsColl;
    }

}

