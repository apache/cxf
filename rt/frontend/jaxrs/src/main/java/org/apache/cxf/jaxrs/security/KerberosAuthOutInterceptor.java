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
package org.apache.cxf.jaxrs.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.transport.http.auth.AbstractSpnegoAuthSupplier;

public class KerberosAuthOutInterceptor extends AbstractSpnegoAuthSupplier
    implements PhaseInterceptor<Message> {

    private String phase = Phase.MARSHAL;
    private AuthorizationPolicy policy;

    public KerberosAuthOutInterceptor() {

    }
    public KerberosAuthOutInterceptor(String phase) {
        this.phase = phase;
    }

    public void handleMessage(Message message) throws Fault {
        URI currentURI = getCurrentURI(message);
        String value = super.getAuthorization(getPolicy(),
                                              currentURI,
                                              message);
        Map<String, List<String>> headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
        if (headers == null) {
            headers = new HashMap<>();
            message.put(Message.PROTOCOL_HEADERS, headers);
        }
        headers.put("Authorization", Collections.singletonList(value));
    }

    private URI getCurrentURI(Message message) {
        try {
            return new URI((String)message.get(Message.ENDPOINT_ADDRESS));
        } catch (URISyntaxException e) {
            // is not expected to happen
            throw new RuntimeException(e);
        }
    }

    public void handleFault(Message message) {
        // complete
    }

    public Set<String> getAfter() {
        return Collections.emptySet();
    }

    public Set<String> getBefore() {
        return Collections.emptySet();
    }

    public String getId() {
        return getClass().getName();
    }

    public String getPhase() {
        return phase;
    }

    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        return Collections.emptySet();
    }
    public AuthorizationPolicy getPolicy() {
        return policy;
    }
    public void setPolicy(AuthorizationPolicy policy) {
        this.policy = policy;
    }

}
