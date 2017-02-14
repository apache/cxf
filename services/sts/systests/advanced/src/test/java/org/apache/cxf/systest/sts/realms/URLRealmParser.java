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
package org.apache.cxf.systest.sts.realms;

import java.util.Map;

import org.apache.cxf.sts.RealmParser;
import org.apache.cxf.ws.security.sts.provider.STSException;

/**
 * A test implementation of RealmParser which returns a realm depending on a String contained
 * in the URL of the service request.
 */
public class URLRealmParser implements RealmParser {

    public String parseRealm(Map<String, Object> messageContext) throws STSException {
        String url = (String)messageContext.get("org.apache.cxf.request.url");
        if (url.contains("realmA")) {
            return "A";
        } else if (url.contains("realmB")) {
            return "B";
        } else if (url.contains("realmC")) {
            return "C";
        }

        return null;
    }

}
