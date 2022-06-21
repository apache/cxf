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

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.ext.ParamConverter;
import org.apache.cxf.jaxrs.utils.HttpUtils;

public class UrlEncodingParamConverter implements ParamConverter<String> {

    private Set<Character> encodeClientParametersList;

    public UrlEncodingParamConverter() {
        this(null);
    }
    public UrlEncodingParamConverter(String encodeClientParametersListStr) {
        if (encodeClientParametersListStr != null) {
            String[] chars = encodeClientParametersListStr.trim().split(" ");
            encodeClientParametersList = new HashSet<>();
            for (String ch : chars) {
                // this may need to be tuned though this should cover URI reserved chars
                encodeClientParametersList.add(Character.valueOf(ch.charAt(0)));
            }
        }
    }

    @Override
    public String fromString(String s) {
        return HttpUtils.urlDecode(s);
    }

    @Override
    public String toString(String s) {
        if (encodeClientParametersList == null || encodeClientParametersList.isEmpty()) {
            return HttpUtils.urlEncode(s);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            Character ch = s.charAt(i);
            if (encodeClientParametersList.contains(ch)) {
                sb.append(HttpUtils.urlEncode(ch.toString()));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();

    }

}
