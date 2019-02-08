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
package org.apache.cxf.rs.security.oauth2.grants.code;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.junit.runner.RunWith;


/**
 * Runs the same tests as JPAOAuthDataProviderTest but within a Spring Managed Transaction.
 *
 * Spring spawns a transaction before each call to <code><oauthProvider</code>.
 *
 * Note : this test needs <code>@DirtiesContext</code>, otherwise
 * spring tests cache and reuse emf across test classes
 * while non spring unit tests are closing emf (hence connection exception: closed).
 *
 * @author agonzalez
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("JPACMTCodeDataProvider.xml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles(value = "openJPA", inheritProfiles = false)
public class JPACMTOAuthDataProviderOpenJPATest extends JPACMTOAuthDataProviderTest {
}
