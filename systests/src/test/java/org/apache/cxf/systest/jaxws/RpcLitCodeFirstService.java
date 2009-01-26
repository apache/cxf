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
package org.apache.cxf.systest.jaxws;

import java.util.List;
import java.util.Vector;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.Holder;

import org.apache.cxf.systest.jaxws.DocLitWrappedCodeFirstService.Foo;


@WebService(name = "RpcLitCodeFirstService",
            targetNamespace = "http://cxf.apache.org/systest/jaxws/RpcLitCodeFirstService")
@SOAPBinding(style = SOAPBinding.Style.RPC,
             use = SOAPBinding.Use.LITERAL)
public interface RpcLitCodeFirstService {
    
    @WebMethod(operationName = "ConvertToString")
    @WebResult(name = "stringNumbers")
    String[] convertToString(@WebParam(name = "intNumbers") int[] numbers);
    
    @WebMethod
    String[] arrayOutput();

    @WebMethod
    String arrayInput(
            @WebParam(name = "input") String[] inputs);

    @WebMethod
    Vector<String> listOutput();
    
    @WebMethod
    String listInput(List<String> inputs);

    @WebMethod
    String multiListInput(List<String> inputs1, List<String> inputs2, String x, int y);
    
    
    @WebMethod
    String multiInOut(@WebParam(mode = WebParam.Mode.OUT)
                      Holder<String> a,
                      @WebParam(mode = WebParam.Mode.INOUT)
                      Holder<String> b,
                      @WebParam(mode = WebParam.Mode.OUT)
                      Holder<String> c,
                      @WebParam(mode = WebParam.Mode.INOUT)
                      Holder<String> d,
                      @WebParam(mode = WebParam.Mode.INOUT)
                      Holder<String> e,
                      @WebParam(mode = WebParam.Mode.OUT)
                      Holder<String> f,
                      @WebParam(mode = WebParam.Mode.OUT)
                      Holder<String> g);
    
    @WebMethod
    String multiHeaderInOut(
                        @WebParam(mode = WebParam.Mode.OUT, header = true)
                        Holder<String> a,
                        @WebParam(mode = WebParam.Mode.INOUT)
                        Holder<String> b,
                        @WebParam(mode = WebParam.Mode.OUT)
                        Holder<String> c,
                        @WebParam(mode = WebParam.Mode.INOUT, header = true)
                        Holder<String> d,
                        @WebParam(mode = WebParam.Mode.INOUT)
                        Holder<String> e,
                        @WebParam(mode = WebParam.Mode.OUT, header = true)
                        Holder<String> f,
                        @WebParam(mode = WebParam.Mode.OUT)
                        Holder<String> g);
    
    @WebMethod
    List<Foo> listObjectOutput();

    @WebMethod
    List<Foo[]> listObjectArrayOutput();

    static class Foo  {
        String name;
        
        public Foo() {
        }
        
        public void setName(String n) {
            name = n;
        }
        public String getName() {
            return name;
        }
    }
    

}
