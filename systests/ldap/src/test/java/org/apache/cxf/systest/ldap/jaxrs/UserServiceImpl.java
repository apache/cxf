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

package org.apache.cxf.systest.ldap.jaxrs;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.apache.cxf.jaxrs.ext.search.ldap.LdapQueryVisitor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.HardcodedFilter;

/**
 * Search for a user's attributes in an LDAP backend. Note this is a test implementation only - care needs to be
 * taken to avoid LDAP injection attacks in real-world scenarios!
 */
public class UserServiceImpl implements UserService {

    private boolean encodeQueryValues = true;

    @Override
    public User searchUser(@PathParam("query") String query, @Context SearchContext searchContext)
        throws UserNotFoundFault {

        SearchCondition<User> sc = searchContext.getCondition(query, User.class);
        if (sc == null) {
            throw new UserNotFoundFault("Search exception");
        }

        LdapQueryVisitor<User> visitor =
            new LdapQueryVisitor<>(Collections.singletonMap("name", "cn"));
        visitor.setEncodeQueryValues(encodeQueryValues);
        sc.accept(visitor.visitor());
        String parsedQuery = visitor.getQuery();

        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext("ldap-jaxrsport.xml");
        LdapTemplate template = (LdapTemplate)appContext.getBean("ldapTemplate");

        String userBaseDn = "OU=users,DC=example,DC=com";
        String[] attributes = new String[] {"sn", "cn"};
        Map<String, Attribute> attrs =
            getAttributesOfEntry(template, userBaseDn, "person", parsedQuery, attributes);

        appContext.close();

        if (attrs == null || attrs.isEmpty()) {
            throw new UserNotFoundFault("Search exception");
        }
        User user = new User();
        try {
            for (Entry<String, Attribute> result : attrs.entrySet()) {
                if ("sn".equals(result.getKey())) {
                    user.setSurname((String)result.getValue().get());
                } else if ("cn".equals(result.getKey())) {
                    user.setName((String)result.getValue().get());
                }
            }
        } catch (NamingException e) {
            throw new UserNotFoundFault("Search exception");
        }

        return user;
    }

    private static Map<String, Attribute> getAttributesOfEntry(LdapTemplate ldapTemplate, String baseDN,
                                                               String objectClass, String searchFilter,
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
        filter.and(new EqualsFilter("objectclass", objectClass)).and(new HardcodedFilter(searchFilter));

        List<?> result = ldapTemplate.search((baseDN == null) ? "" : baseDN, filter.toString(),
            SearchControls.SUBTREE_SCOPE, searchAttributes, mapper);
        if (result != null && !result.isEmpty()) {
            ldapAttributes = CastUtils.cast((Map<?, ?>)result.get(0));
        }

        return ldapAttributes;
    }

    public boolean isEncodeQueryValues() {
        return encodeQueryValues;
    }

    public void setEncodeQueryValues(boolean encodeQueryValues) {
        this.encodeQueryValues = encodeQueryValues;
    }
}


