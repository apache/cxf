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
package org.apache.cxf.systest.sts.common;

import java.util.Map;
import java.util.StringTokenizer;

import org.apache.cxf.sts.RealmParser;
import org.apache.cxf.ws.security.sts.provider.STSException;

public class UriRealmParser implements RealmParser {

    @Override
    public String parseRealm(Map<String, Object> messageContext) throws STSException {


        String realm = null;
        try {
            String url = (String)messageContext.get("org.apache.cxf.request.url");

            StringTokenizer st = new StringTokenizer(url, "/");

            int count = st.countTokens();
            if (count <= 4) {
                return null;
            }
            count--;
            for (int i = 0; i < count; i++) {
                realm = st.nextToken();
            }
        } catch (Exception ex) {
          // No realm found
        }
        return realm;

    }

}
