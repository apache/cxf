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
package org.apache.cxf.jaxws.service;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface FooService {
//    @WebResult(name = "FooElementResult", targetNamespace = "http://namespace2")
//    Foo echo(@WebParam(name = "FooElementRequest", targetNamespace = "http://namespace1")
//             Foo foo);

    @WebMethod(operationName = "FooEcho2", action = "http://namespace4")
    @WebResult(name = "FooEcho2HeaderResult", partName = "fooPart", header = true, 
               targetNamespace = "http://namespace5")
    Foo echo2(@WebParam(name = "FooEcho2HeaderRequest", header = true, partName = "fooPart",
                        targetNamespace = "http://namespace3")
              Foo foo, String name);
}
