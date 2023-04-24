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
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.naming.Name;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.token.realm.RealmSupport;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.Filter;

public class LdapGroupClaimsHandler implements ClaimsHandler, RealmSupport {

    private static final Logger LOG = LogUtils.getL7dLogger(LdapGroupClaimsHandler.class);

    private static final String SCOPE = "%SCOPE%";
    private static final String ROLE = "%ROLE%";

    private LdapTemplate ldap;
    private String userBaseDn;
    private String groupBaseDn;
    private String userObjectClass = "person";
    private String groupObjectClass = "groupOfNames";
    private String userNameAttribute = "cn";
    private String groupMemberAttribute = "member";
    private String groupURI = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
    private String groupNameGlobalFilter = ROLE;
    private String groupNameScopedFilter = SCOPE + "_" + ROLE;
    private Map<String, String> appliesToScopeMapping;
    private boolean useFullGroupNameAsValue;
    private List<String> supportedRealms;
    private List<Filter> customFilters;
    private String realm;


    public void setSupportedRealms(List<String> supportedRealms) {
        this.supportedRealms = supportedRealms;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public boolean isUseFullGroupNameAsValue() {
        return useFullGroupNameAsValue;
    }

    public void setUseFullGroupNameAsValue(boolean useFullGroupNameAsValue) {
        this.useFullGroupNameAsValue = useFullGroupNameAsValue;
    }

    public String getUserObjectClass() {
        return userObjectClass;
    }

    public void setUserObjectClass(String userObjectClass) {
        this.userObjectClass = userObjectClass;
    }

    public String getGroupObjectClass() {
        return groupObjectClass;
    }

    public void setGroupObjectClass(String groupObjectClass) {
        this.groupObjectClass = groupObjectClass;
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

    public void setUserBaseDN(String userBaseDN) {
        this.userBaseDn = userBaseDN;
    }

    public String getUserBaseDN() {
        return userBaseDn;
    }

    public String getGroupMemberAttribute() {
        return groupMemberAttribute;
    }

    public void setGroupMemberAttribute(String groupMemberAttribute) {
        this.groupMemberAttribute = groupMemberAttribute;
    }

    public String getGroupURI() {
        return groupURI;
    }

    public void setGroupURI(String groupURI) {
        this.groupURI = groupURI;
    }

    public void setAppliesToScopeMapping(Map<String, String> appliesToScopeMapping) {
        this.appliesToScopeMapping = appliesToScopeMapping;
    }

    public Map<String, String> getAppliesToScopeMapping() {
        return appliesToScopeMapping;
    }

    public String getGroupBaseDN() {
        return groupBaseDn;
    }

    public void setGroupBaseDN(String groupBaseDN) {
        this.groupBaseDn = groupBaseDN;
    }

    public String getGroupNameGlobalFilter() {
        return groupNameGlobalFilter;
    }

    public void setGroupNameGlobalFilter(String groupNameGlobalFilter) {
        this.groupNameGlobalFilter = groupNameGlobalFilter;
    }

    public String getGroupNameScopedFilter() {
        return groupNameScopedFilter;
    }

    public void setGroupNameScopedFilter(String groupNameScopedFilter) {
        this.groupNameScopedFilter = groupNameScopedFilter;
    }

    public List<String> getSupportedClaimTypes() {
        return Collections.singletonList(groupURI);
    }

    public ProcessedClaimCollection retrieveClaimValues(
            ClaimCollection claims, ClaimsParameters parameters) {

        boolean found = false;
        for (Claim claim: claims) {
            if (claim.getClaimType().equals(this.groupURI)) {
                found = true;
                break;
            }
        }
        if (!found) {
            return new ProcessedClaimCollection();
        }

        String user = null;

        Principal principal = parameters.getPrincipal();
        if (principal instanceof KerberosPrincipal) {
            KerberosPrincipal kp = (KerberosPrincipal)principal;
            StringTokenizer st = new StringTokenizer(kp.getName(), "@");
            user = st.nextToken();
        } else if (principal instanceof X500Principal) {
            X500Principal x500p = (X500Principal)principal;
            LOG.warning("Unsupported principal type X500: " + x500p.getName());
        } else if (principal != null) {
            user = principal.getName();
            if (user == null) {
                LOG.warning("Principal name must not be null");
            }
        } else {
            LOG.warning("Principal is null");
        }
        if (user == null) {
            return new ProcessedClaimCollection();
        }

        if (!LdapUtils.isDN(user)) {
            Name dn = LdapUtils.getDnOfEntry(ldap, this.userBaseDn, this.getUserObjectClass(),
                                             this.getUserNameAttribute(), user);
            if (dn != null) {
                user = dn.toString();
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("DN for (" + this.getUserNameAttribute() + "=" + user + ") found: " + user);
                }
            } else {
                LOG.warning("DN not found for user '" + user + "'");
                return new ProcessedClaimCollection();
            }
        }

        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("Retrieve groups for user " + user);
        }

        List<Filter> filters = new ArrayList<>();
        filters.add(new EqualsFilter(this.groupMemberAttribute, user));
        if (customFilters != null && !customFilters.isEmpty()) {
            filters.addAll(customFilters);
        }

        List<String> groups =
            LdapUtils.getAttributeOfEntries(ldap, this.groupBaseDn, this.getGroupObjectClass(),
                                            filters, "cn");

        if (groups == null || groups.isEmpty()) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info("No groups found for user '" + user + "'");
            }
            return new ProcessedClaimCollection();
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Groups for user '" + parameters.getPrincipal().getName() + "': " + groups);
        }

        String scope = null;
        if (getAppliesToScopeMapping() != null && !getAppliesToScopeMapping().isEmpty()
            && parameters.getAppliesToAddress() != null) {
            scope = getAppliesToScopeMapping().get(parameters.getAppliesToAddress());
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("AppliesTo matches with scope: " + scope);
            }
        }

        String regex = this.groupNameGlobalFilter;
        regex = regex.replaceAll(ROLE, ".*");
        Pattern globalPattern = Pattern.compile(regex);

        //If AppliesTo value can be mapped to a Scope Name
        //ex. https://localhost/doubleit/services/doubleittransport  -> Demo
        Pattern scopePattern = null;
        if (scope != null) {
            regex = this.groupNameScopedFilter;
            regex = regex.replaceAll(SCOPE, scope).replaceAll(ROLE, ".*");
            scopePattern = Pattern.compile(regex);
        }

        List<String> filteredGroups = new ArrayList<>();
        for (String group: groups) {
            if (scopePattern != null && scopePattern.matcher(group).matches()) {
                //Group matches the scoped filter
                //ex. (default groupNameScopeFilter)
                //  Demo_User -> Role=User
                //  Demo_Admin -> Role=Admin
                String filter = this.groupNameScopedFilter;
                final String role;
                if (isUseFullGroupNameAsValue()) {
                    role = group;
                } else {
                    role = parseRole(group, filter.replaceAll(SCOPE, scope));
                }
                filteredGroups.add(role);
            } else {
                if (globalPattern.matcher(group).matches()) {
                    //Group matches the global filter
                    //ex. (default groupNameGlobalFilter)
                    //  User -> Role=User
                    //  Admin -> Role=Admin
                    final String role;
                    if (isUseFullGroupNameAsValue()) {
                        role = group;
                    } else {
                        role = parseRole(group, this.groupNameGlobalFilter);
                    }
                    filteredGroups.add(role);
                } else if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("Group '" + group + "' doesn't match scoped and global group filter");
                }
            }
        }

        LOG.info("Filtered groups: " + filteredGroups);
        if (filteredGroups.isEmpty()) {
            LOG.info("No matching groups found for user '" + principal + "'");
            return new ProcessedClaimCollection();
        }

        ProcessedClaimCollection claimsColl = new ProcessedClaimCollection();
        ProcessedClaim c = new ProcessedClaim();
        c.setClaimType(URI.create(this.groupURI));
        c.setPrincipal(principal);
        c.setValues(new ArrayList<>(filteredGroups));
        // c.setIssuer(issuer);
        // c.setOriginalIssuer(originalIssuer);
        // c.setNamespace(namespace);
        claimsColl.add(c);

        return claimsColl;
    }

    @Override
    public List<String> getSupportedRealms() {
        return supportedRealms;
    }

    @Override
    public String getHandlerRealm() {
        return realm;
    }

    private String parseRole(String group, String filter) {
        int roleStart = filter.indexOf(ROLE);
        int trimEnd = filter.length() - ROLE.length() - roleStart;
        return group.substring(roleStart, group.length() - trimEnd);
    }

    public List<Filter> getCustomFilters() {
        return customFilters;
    }

    /**
     * Define some custom filters to use in retrieving group membership information. This allows you to restrict
     * the groups that are returned based on some attribute value, for example.
     * @param customFilters
     */
    public void setCustomFilters(List<Filter> customFilters) {
        this.customFilters = customFilters;
    }

}
