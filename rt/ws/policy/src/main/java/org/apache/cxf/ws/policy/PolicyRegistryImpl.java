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

package org.apache.cxf.ws.policy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.neethi.Policy;
import org.apache.neethi.PolicyRegistry;

/**
 *
 */
public class PolicyRegistryImpl implements PolicyRegistry {

    private Map<String, Policy> reg = new ConcurrentHashMap<>(16, 0.75f, 4);

    public Policy lookup(String key) {
        return reg.get(key);
    }

    public void register(String key, Policy policy) {
        reg.put(key, policy);
    }

    public void remove(String key) {
        reg.remove(key);
    }

}
