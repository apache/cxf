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

package org.apache.cxf.rs.security.saml.sso;

/**
 * The replay cache type used by the SAML SSO module. It extends the common
 * {@link org.apache.cxf.rs.security.common.TokenReplayCache} (which defines all of its methods), and is
 * kept so that existing implementations and configurations continue to work unchanged.
 */
public interface TokenReplayCache<T> extends org.apache.cxf.rs.security.common.TokenReplayCache<T> {

}