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

package org.apache.cxf.microprofile.client.mock;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

public class MyClientHeadersFactory implements ClientHeadersFactory {

    private static MultivaluedMap<String, String> initialHeaders;

    public static MultivaluedMap<String, String> getInitialHeaders() {
        return initialHeaders;
    }

    public static void setInitialHeaders(MultivaluedMap<String, String> newHeaders) {
        initialHeaders = newHeaders;
    }

    private static String reverse(String s) {
        StringBuilder sb = new StringBuilder();
        char[] ch = s.toCharArray();
        //CHECKSTYLE:OFF
        for (int i = ch.length-1; i >= 0; i--) {
            sb.append(ch[i]);
        }
        //CHECKSTYLE:ON
        return sb.toString();
    }

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                 MultivaluedMap<String, String> clientOutgoingHeaders) {

        initialHeaders = new MultivaluedHashMap<>();
        initialHeaders.putAll(clientOutgoingHeaders);
        MultivaluedMap<String, String> updatedMap = new MultivaluedHashMap<>();
        clientOutgoingHeaders.forEach((k, v) -> {
            updatedMap.putSingle(k, reverse(v.get(0))); });
        return updatedMap;
    }
}
