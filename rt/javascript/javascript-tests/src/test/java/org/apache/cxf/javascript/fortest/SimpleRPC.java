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

package org.apache.cxf.javascript.fortest;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

/**
 * Test SEI for RPC.
 */
@WebService(targetNamespace = "uri:cxf.apache.org.javascript.rpc")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface SimpleRPC {
    @WebMethod
    String simpleType(@WebParam(name = "P1") String p1, @WebParam(name = "P2") int p2);
    @WebMethod
    void returnVoid(@WebParam(name = "P1") String p1, @WebParam(name = "P2") int p2);
    @WebMethod
    void beanType(@WebParam(name = "param1") TestBean1 p1);
    
}
