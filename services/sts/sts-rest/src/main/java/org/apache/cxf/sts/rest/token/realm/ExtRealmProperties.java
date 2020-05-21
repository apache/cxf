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
package org.apache.cxf.sts.rest.token.realm;

import java.util.Map;

import org.apache.cxf.sts.token.realm.RealmProperties;

import static java.util.Optional.ofNullable;

public class ExtRealmProperties extends RealmProperties {

    private Map<String, String> rsSecurityProperties;

    public Map<String, String> getRsSecurityProperties() {
        return rsSecurityProperties;
    }

    public void setRsSecurityProperties(Map<String, String> rsSecurityProperties) {
        this.rsSecurityProperties = rsSecurityProperties;
    }

    public Object getRsSecurityProperty(final String propertyName) {
        return ofNullable(getRsSecurityProperties())
                .map(map -> map.get(propertyName))
                .orElse(null);
    }
}