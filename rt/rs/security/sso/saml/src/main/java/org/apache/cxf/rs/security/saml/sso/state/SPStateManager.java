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
package org.apache.cxf.rs.security.saml.sso.state;

import java.io.Closeable;
import java.io.IOException;

/**
 * SSO Service Provider State Manager.
 *
 * TODO: review the possibility of working with the Servlet HTTPSession
 * instead; in that case it can be tricky to configure various containers
 * (Tomcat, Jetty) to make sure the cookies are shared across multiple
 * war contexts which will be needed if RequestAssertionConsumerService
 * needs to be run in its own war file instead of having every application
 * war on the SP side have a dedicated RequestAssertionConsumerService endpoint
 */
public interface SPStateManager extends Closeable {

    void setRequestState(String relayState, RequestState state);
    RequestState removeRequestState(String relayState);

    void setResponseState(String contextKey, ResponseState state);
    ResponseState getResponseState(String contextKey);
    ResponseState removeResponseState(String contextKey);

    void close() throws IOException;
}
