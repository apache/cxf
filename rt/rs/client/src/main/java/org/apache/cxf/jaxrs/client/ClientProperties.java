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

package org.apache.cxf.jaxrs.client;

import org.apache.cxf.common.util.SystemPropertyAction;

public interface ClientProperties {
    String HTTP_CONNECTION_TIMEOUT_PROP = "http.connection.timeout";
    String HTTP_RECEIVE_TIMEOUT_PROP = "http.receive.timeout";
    String HTTP_PROXY_SERVER_PROP = "http.proxy.server.uri";
    String HTTP_PROXY_SERVER_PORT_PROP = "http.proxy.server.port";
    String HTTP_AUTOREDIRECT_PROP = "http.autoredirect";
    String HTTP_MAINTAIN_SESSION_PROP = "http.maintain.session";
    String HTTP_RESPONSE_AUTOCLOSE_PROP = "http.response.stream.auto.close";
    String THREAD_SAFE_CLIENT_PROP = "thread.safe.client";
    String THREAD_SAFE_CLIENT_STATE_CLEANUP_PROP = "thread.safe.client.state.cleanup.period";
    Boolean DEFAULT_THREAD_SAFETY_CLIENT_STATUS =
        Boolean.parseBoolean(SystemPropertyAction.getPropertyOrNull(THREAD_SAFE_CLIENT_PROP));
    Integer THREAD_SAFE_CLIENT_STATE_CLEANUP_PERIOD =
        getIntValue(SystemPropertyAction.getPropertyOrNull(THREAD_SAFE_CLIENT_STATE_CLEANUP_PROP));

    static Integer getIntValue(Object o) {
        return o instanceof Integer ? (Integer)o : o instanceof String ? Integer.valueOf(o.toString()) : null;
    }
}
