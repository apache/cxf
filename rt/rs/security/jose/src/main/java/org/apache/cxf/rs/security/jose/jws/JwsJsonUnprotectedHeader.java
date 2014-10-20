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
package org.apache.cxf.rs.security.jose.jws;

import java.util.Map;

import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.JoseHeadersReaderWriter;

public class JwsJsonUnprotectedHeader {

    private JoseHeadersReaderWriter writer = new JoseHeadersReaderWriter();
    private JoseHeaders headerEntries;

    public JwsJsonUnprotectedHeader() {
    }
    public JwsJsonUnprotectedHeader(JoseHeaders headers) {
        headerEntries = headers;
    }

    public JwsJsonUnprotectedHeader(Map<String, Object> values) {
        this(new JoseHeaders(values));
    }

       
    public void addHeader(String name, Object value) {
        if (JoseConstants.HEADER_CRITICAL.equals(name)) {
            throw new SecurityException();
        }
        headerEntries.setHeader(name, value);
    }
    public Object getHeader(String name) {
        return headerEntries.getHeader(name);
    }
    public JoseHeaders getHeaderEntries() {
        return headerEntries;
    }
    public String toJson() {
        return writer.headersToJson(headerEntries);
    } 
}
