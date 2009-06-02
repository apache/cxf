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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.ws.Holder;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import org.apache.cxf.feature.Features;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.systest.jaxws.types.Bar;

@WebService(name = "DocLitWrappedCodeFirstService",
            targetNamespace = "http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService")
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT,
             use = SOAPBinding.Use.LITERAL)
//@Features(features = { "org.apache.cxf.feature.FastInfosetFeature" })
@Features(features = { "org.apache.cxf.transport.http.gzip.GZIPFeature", 
                       "org.apache.cxf.feature.FastInfosetFeature" })
public interface DocLitWrappedCodeFirstService {

    @Oneway
    @WebMethod
    void doOneWay();
    
    @WebMethod
    String[] arrayOutput();

    @WebMethod
    String arrayInput(
            @WebParam(name = "input") String[] inputs);

    @WebMethod
    Vector<String> listOutput();
    
    @WebMethod 
    String echoStringNotReallyAsync(String s);
    
    @WebMethod
    int[] echoIntArray(int[] ar, Exchange ex);
    
    @WebMethod
    @WebResult(partName = "parameters")
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
    List<Foo> listObjectOutput();

    @WebMethod
    boolean listObjectIn(@WebParam(mode = WebParam.Mode.INOUT)
                         Holder<List<Foo[]>> foos);

    
    @WebMethod
    List<Foo[]> listObjectArrayOutput();
    
    @WebMethod
    int throwException(int i) 
        throws ServiceTestFault, CustomException, ComplexException;
    
    @RequestWrapper(localName = "echoIntX")
    @ResponseWrapper(localName = "echoIntXResponse")
    int echoIntDifferentWrapperName(int i);
    
    @WebMethod
    @WebResult(targetNamespace = "http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
               name = "result")
    @RequestWrapper(className = "org.apache.cxf.systest.jaxws.Echo")
    @ResponseWrapper(className = "org.apache.cxf.systest.jaxws.EchoResponse")
    String echo(@WebParam(targetNamespace = 
            "http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService2", 
                          name = "String_1")
                        String msg);

    Bar createBar(String val);
    
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
    
    Set<Foo> getFooSet();
    
    @RequestWrapper(className = "org.apache.cxf.systest.jaxws.DocLitWrappedCodeFirstService$DoFooListRequest")
    @WebMethod(operationName = "doFooList")
    String doFooList(@WebParam(name = "dbRef") List<Foo> fooList);
    
    
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "doFooList", propOrder = { "dbReves" })
    public static class DoFooListRequest {
        @XmlElement(name = "dbRef", required = true)
        protected List<Foo> dbReves = new ArrayList<Foo>();

        public List<Foo> getDbReves() {
            return dbReves;
        }
    }
}
