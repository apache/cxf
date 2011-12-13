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
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 * 
 */
@WebService(targetNamespace = "uri:org.apache.cxf.javascript.fortest")
public interface SimpleDocLitWrapped {
    @RequestWrapper(className = "org.apache.cxf.javascript.fortest.BasicTypeFunctionReturnStringWrapper")
    @ResponseWrapper(className = "org.apache.cxf.javascript.fortest.StringWrapper")
    @WebResult(name = "returnValue", targetNamespace = "uri:org.apache.cxf.javascript.testns")
    @WebMethod
    String basicTypeFunctionReturnString(@WebParam(name = "s") String s, 
                                         @WebParam(name = "i") int i, 
                                         @WebParam(name = "l") long l, 
                                         @WebParam(name = "f") float f, 
                                         @WebParam(name = "d") double d);
    
    @WebMethod
    String basicTypeFunctionReturnStringNoWrappers(@WebParam(name = "s") String s, 
                                                   @WebParam(name = "i") int i, 
                                                   @WebParam(name = "l") long l, 
                                                   @WebParam(name = "f") float f, 
                                                   @WebParam(name = "d") double d);

    @WebMethod
    TestBean1 functionReturnTestBean1();
    
    @WebMethod
    String echoWithHeader(@WebParam(name = "what") String what);
    
    @WebMethod
    int basicTypeFunctionReturnInt(@WebParam(name = "s") String s, 
                                   @WebParam(name = "i") int i, 
                                   @WebParam(name = "l") long l, 
                                   @WebParam(name = "f") float f, 
                                   @WebParam(name = "d") double d);
    
    @RequestWrapper(className = "org.apache.cxf.javascript.fortest.BeanRequestWrapper")
    @WebMethod
    void beanFunctionWithWrapper(@WebParam(name = "bean1") TestBean1 bean, 
                                 @WebParam(name = "beanArray") TestBean1[] beans);
    
    @WebMethod
    void beanFunction(@WebParam(name = "bean1") TestBean1 bean, 
                      @WebParam(name = "beanArray") TestBean1[] beans);
    
    @WebMethod
    void genericTestFunction(@WebParam(name = "g1") SpecificGenericClass sgc,
                             @WebParam(name = "g2") GenericGenericClass<Double> ggc);
    
    @WebMethod 
    void inheritanceTestFunction(@WebParam(name = "d") InheritanceTestDerived d);
    
    @WebMethod
    AnEnum enumEcho(@WebParam(name = "ev") AnEnum value);
}
