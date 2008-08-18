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
package org.apache.cxf.tools.fortest.xmllist;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlList;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;


@WebService(name = "AddNumbersPortType", targetNamespace = "http://apache.org/xmllist")
public interface AddNumbersPortType {


    /**
     *
     * @param arg
     * @return
     *     returns java.util.List<java.lang.Integer>
     */
    @WebMethod
    @WebResult(name = "arg0", targetNamespace = "http://apache.org/xmllist")
    @RequestWrapper(localName = "addNumbers", targetNamespace = "http://apache.org/xmllist")
    @ResponseWrapper(localName = "addNumbersResponse", targetNamespace = "http://apache.org/xmllist")
    List<Integer> addNumbers(
        @WebParam(name = "arg", targetNamespace = "http://apache.org/xmllist")
        @XmlList
        List<String> arg);
    
    //test for CXF-1752 
    @WebMethod(action = "testCXF1752")
    @WebResult(name = "result") 
    UserObject testCXF1752(
        @WebParam(name = "receivers") List<Long> receivers, 
        @WebParam(name = "item") UserObject item, 
        @WebParam(name = "binaryContent") byte[] binaryContent, 
        @WebParam(name = "userObjects") UserObject[] objects, 
        @WebParam(name = "userObjectList") List<UserObject> objectList, 
        @WebParam(name = "fileName") String fileName);
    
    
    static class UserObject {
        String myData;
        
        public void setMyData(String s) {
            myData = s;
        }
        public String getMyData() {
            return myData;
        }
    }
}
