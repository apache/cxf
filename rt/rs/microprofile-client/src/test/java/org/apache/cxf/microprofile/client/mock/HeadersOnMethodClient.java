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

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;


public interface HeadersOnMethodClient {

    default String computeHeader(String headerName) {
        return "HeadersOnMethodClientValueFor" + headerName;
    }

    @ClientHeaderParam(name = "MethodHeader1", value = "valueA")
    @ClientHeaderParam(name = "MethodHeader2", value = {"valueB", "valueC"})
    @ClientHeaderParam(name = "MethodHeader3", value = "{computeHeader}")
    @ClientHeaderParam(name = "MethodHeader4",
        value = "{org.apache.cxf.microprofile.client.mock.HeaderGenerator.generateHeader}")
    @DELETE
    @Path("/")
    String delete(String someValue);
}
