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
package org.apache.cxf.tools.fortest.headers;


import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

@WebService(targetNamespace = "http://apache.org/cxf/test", name = "SomeHeaders")
public interface SomeHeaders {

    // using four parameters where two being headers
    @WebMethod()
    @WebResult (name = "someHeadersResponse")
    String hello(
                 @WebParam(name = "body1") String body1,
                 @WebParam(name = "body2") String body2,
                 @WebParam(header = true, name = "header1") String header1,
                 @WebParam(header = true, name = "header2") String header2);

}
