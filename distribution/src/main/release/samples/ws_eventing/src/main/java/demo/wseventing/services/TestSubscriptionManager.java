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

package demo.wseventing.services;

import javax.jws.WebService;

import org.apache.cxf.ws.eventing.backend.manager.SubscriptionManagerInterfaceForManagers;
import org.apache.cxf.ws.eventing.manager.AbstractSubscriptionManager;

import demo.wseventing.SingletonSubscriptionManagerContainer;

@WebService(endpointInterface = "org.apache.cxf.ws.eventing.manager.SubscriptionManagerEndpoint")
public class TestSubscriptionManager extends AbstractSubscriptionManager {

    @Override
    protected SubscriptionManagerInterfaceForManagers getSubscriptionManagerBackend() {
        return SingletonSubscriptionManagerContainer.getInstance();
    }
}
