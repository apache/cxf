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
package org.apache.cxf.rs.security.oauth2.common;

import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UserSubjectTest {

    @Test
    public void testId() {
        UserSubject userSubject = new UserSubject();
        assertNotNull(userSubject.getId());
        userSubject = new UserSubject("someLogin");
        assertNotNull(userSubject.getId());
        userSubject = new UserSubject("someLogin", Collections.singletonList("somerole"));
        assertNotNull(userSubject.getId());
        UserSubject newSubject = new UserSubject(userSubject);
        assertEquals(userSubject.getId(), newSubject.getId());
    }
}