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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.security.auth.x500.X500Principal;

import org.apache.cxf.helpers.CastUtils;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.Filter;

public final class LdapUtils {

    private LdapUtils() {
    }

    public static boolean isDN(String user) {
        try {
            new X500Principal(user);
            return true;
            //Principal contains a DN -> ldap.lookup
        } catch (Exception ex) {
            //Principal does not contain a DN -> ldap.search
            return false;
        }
    }

    public static Map<String, Attribute> getAttributesOfEntry(LdapTemplate ldapTemplate, String baseDN,
        String objectClass, String filterAttributeName, String filterAttributeValue,
        String[] searchAttributes) {

        Map<String, Attribute> ldapAttributes = null;

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

        AndFilter filter = new AndFilter();
        filter.and(
                new EqualsFilter("objectclass", objectClass)).and(
                        new EqualsFilter(filterAttributeName, filterAttributeValue));

        List<Map<String, Attribute>> result = ldapTemplate.search((baseDN == null) ? "" : baseDN, filter.toString(),
            SearchControls.SUBTREE_SCOPE, searchAttributes, mapper);
        if (result != null && !result.isEmpty()) {
            ldapAttributes = result.get(0);
        }

        return ldapAttributes;
    }

    public static List<String> getAttributeOfEntries(
        LdapTemplate ldapTemplate, String baseDN,
        String objectClass, String filterAttributeName, String filterAttributeValue,
        String searchAttribute) {

        List<Filter> filters =
            Collections.singletonList(new EqualsFilter(filterAttributeName, filterAttributeValue));
        return getAttributeOfEntries(ldapTemplate, baseDN, objectClass, filters, searchAttribute);
    }

    public static List<String> getAttributeOfEntries(
        LdapTemplate ldapTemplate, String baseDN,
        String objectClass, List<Filter> filters,
        String searchAttribute) {

        List<String> ldapAttributes = null;

        AttributesMapper<Object> mapper =
            new AttributesMapper<Object>() {
            public Object mapFromAttributes(Attributes attrs) throws NamingException {
                NamingEnumeration<? extends Attribute> attrEnum = attrs.getAll();
                while (attrEnum.hasMore()) {
                    return attrEnum.next().get();
                }
                return null;
            }
        };

        String[] searchAttributes = new String[] {searchAttribute};

        AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter("objectclass", objectClass));
        if (filters != null) {
            for (Filter f : filters) {
                filter.and(f);
            }
        }

        List<?> result = ldapTemplate.search((baseDN == null) ? "" : baseDN, filter.toString(),
            SearchControls.SUBTREE_SCOPE, searchAttributes, mapper);
        if (result != null && !result.isEmpty()) {
            ldapAttributes = CastUtils.cast((List<?>)result);
        }

        return ldapAttributes;
    }

    public static Name getDnOfEntry(LdapTemplate ldapTemplate, String baseDN,
        String objectClass, String filterAttributeName, String filterAttributeValue) {

        ContextMapper<Name> mapper =
            new AbstractContextMapper<Name>() {
                public Name doMapFromContext(DirContextOperations ctx) {
                    return ctx.getDn();
                }
            };

        AndFilter filter = new AndFilter();
        filter.and(
            new EqualsFilter("objectclass", objectClass)).and(
                new EqualsFilter(filterAttributeName, filterAttributeValue));

        List<Name> result = ldapTemplate.search((baseDN == null) ? "" : baseDN, filter.toString(),
            SearchControls.SUBTREE_SCOPE, mapper);

        if (result != null && !result.isEmpty()) {
            //not only the first one....
            return result.get(0);
        }
        return null;
    }

}
