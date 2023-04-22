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

import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;

@ClientHeaderParam(name = "IntfHeader1", value = "value1")
@ClientHeaderParam(name = "IntfHeader2", value = {"value2", "value3"})
@ClientHeaderParam(name = "IntfHeader3", value = "{computeHeader}")
@ClientHeaderParam(name = "IntfHeader4",
    value = "{org.apache.cxf.microprofile.client.mock.HeaderGenerator.generateHeader}")
public interface HeadersOnInterfaceClient {

    default String computeHeader(String headerName) {
        return "HeadersOnInterfaceClientValueFor" + headerName;
    }

    @PUT
    @Path("/")
    String put(String someValue);
}
