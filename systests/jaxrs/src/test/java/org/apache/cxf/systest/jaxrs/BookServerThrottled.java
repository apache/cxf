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

package org.apache.cxf.systest.jaxrs;

import java.util.Collections;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;
import org.apache.cxf.throttling.ThrottleResponse;
import org.apache.cxf.throttling.ThrottlingFeature;
import org.apache.cxf.throttling.ThrottlingManager;


public class BookServerThrottled extends AbstractServerTestServerBase {
    public static final String PORT = allocatePort(BookServerThrottled.class);

    @Override
    protected Server createServer(Bus bus) throws Exception {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(BookStore.class);
        sf.setFeatures(Collections.singletonList(
            new ThrottlingFeature(new ThrottlingManagerImpl())));
        sf.setResourceProvider(BookStore.class,
                               new SingletonResourceProvider(new BookStore(), true));
        sf.setAddress("http://localhost:" + PORT + "/");
        return sf.create();
    }

    public static void main(String[] args) throws Exception {
        new BookServerThrottled().start();
    }

    private static final class ThrottlingManagerImpl implements ThrottlingManager {

        @Override
        public List<String> getDecisionPhases() {
            return CastUtils.cast(Collections.singletonList(Phase.PRE_STREAM));
        }

        @Override
        public ThrottleResponse getThrottleResponse(String phase, Message m) {
            AuthorizationPolicy ap = m.get(AuthorizationPolicy.class);
            if (ap != null && "alice".equals(ap.getUserName())) {
                return null;
            }
            return new ThrottleResponse(503, 2000);
        }

    }
}
