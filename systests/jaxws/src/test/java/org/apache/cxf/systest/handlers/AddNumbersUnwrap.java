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
package org.apache.cxf.systest.handlers;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

//CHECKSTYLE:OFF 
@WebService(targetNamespace = "http://apache.org/handlers", name = "AddNumbers")
public interface AddNumbersUnwrap {

    @WebResult(name = "return", targetNamespace = "http://apache.org/handlers/types")
    @RequestWrapper(localName = "addNumbers", targetNamespace = "http://apache.org/handlers/types", 
                    className = "org.apache.cxf.systest.handlers.types.AddNumbers")
    @WebMethod
    @ResponseWrapper(localName = "addNumbersResponse", targetNamespace = "http://apache.org/handlers/types", 
                     className = "org.apache.cxf.systest.handlers.types.AddNumbersResponse")
    public int addNumbers(
        @WebParam(name = "arg0", targetNamespace = "http://apache.org/handlers/types")
        int arg0,
        @WebParam(name = "arg1", targetNamespace = "http://apache.org/handlers/types")
        int arg1
    );
}
//CHECKSTYLE:ON