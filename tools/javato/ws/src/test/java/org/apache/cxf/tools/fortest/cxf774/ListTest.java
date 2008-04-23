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

package org.apache.cxf.tools.fortest.cxf774;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebService;

import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "ListTest", targetNamespace = "http://cxf.apache/")
public interface ListTest {

    @WebMethod(operationName = "sayHi", exclude = false)
    @ResponseWrapper(className = "apache.cxf.SayHiResponse", 
                     localName = "sayHiResponse", 
                     targetNamespace = "http://cxf.apache/")
    @RequestWrapper(className = "apache.cxf.SayHi", 
                    localName = "sayHi", 
                    targetNamespace = "http://cxf.apache/")
    List sayHi(String hi);

}
