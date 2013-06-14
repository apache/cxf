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

import java.net.URISyntaxException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests need a real ldap server
 */
public class LDAPSearchTest {
    @Test
    @Ignore
    public void testSearch() throws URISyntaxException, NamingException {
        LdapSearch ldapSearch = new LdapSearch("ldap://localhost:2389",
                                               "cn=Directory Manager,dc=example,dc=com", "test", 2);
        NamingEnumeration<SearchResult> answer = ldapSearch.searchSubTree("dc=example, dc=com",
                                                                          "(cn=Testuser)");
        while (answer.hasMore()) {
            SearchResult sr = answer.next();
            Attributes attrs = sr.getAttributes();
            Attribute cn = attrs.get("sn");
            System.out.println(cn.get());
        }
    }

}
