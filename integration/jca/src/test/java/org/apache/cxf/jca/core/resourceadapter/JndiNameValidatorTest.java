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
package org.apache.cxf.jca.core.resourceadapter;

import org.junit.Assert;
import org.junit.Test;

public class JndiNameValidatorTest {

    @Test
    public void testValidateJndiNamePlainNamesAllowed() {
        JndiNameValidator.validateJndiName("java:comp/env/ejb/MyBean");
        JndiNameValidator.validateJndiName("ejb/MyBeanLocal");
        JndiNameValidator.validateJndiName(null);
    }

    @Test
    public void testValidateJndiNameRemoteUrlRejected() {
        for (String malicious : new String[]{
            "ldap://attacker.com/exploit",
            "rmi://attacker.com/exploit",
            "iiop://attacker.com/exploit"
        }) {
            try {
                JndiNameValidator.validateJndiName(malicious);
                Assert.fail("Expected IllegalArgumentException for: " + malicious);
            } catch (IllegalArgumentException e) {
                Assert.assertTrue(e.getMessage().contains("JNDI name must not contain a URL"));
            }
        }
    }
}