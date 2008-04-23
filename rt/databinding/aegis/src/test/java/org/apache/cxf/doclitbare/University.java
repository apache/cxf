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

package org.apache.cxf.doclitbare;

/**
 * 
 */
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService(name = "University", targetNamespace = "http://cxf.apache.org/dlb/")
@SOAPBinding(use = SOAPBinding.Use.LITERAL, style = SOAPBinding.Style.DOCUMENT, 
             parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface University {
    @WebResult(targetNamespace = "http://education.toorosystems.com/", name = "return", partName = "return")
    @WebMethod(operationName = "getTeacher", exclude = false)
    Teacher getTeacher(
                              @WebParam(targetNamespace = "http://cxf.apache.org/dlb/", 
                                        name = "course", mode = WebParam.Mode.IN)
                              Course course);
}
