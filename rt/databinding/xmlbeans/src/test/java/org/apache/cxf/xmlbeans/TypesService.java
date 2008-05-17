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
package org.apache.cxf.xmlbeans;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.apache.cxf.helpers.IOUtils;

@WebService(targetNamespace = "urn:TypesService")
public class TypesService {

    @WebMethod
    public String testInt(int i, @WebParam(mode = WebParam.Mode.OUT) Holder<Integer> i2) {
        i2.value = i;
        return "In:" + i;
    }
    @WebMethod
    public String testInteger(Integer i,  @WebParam(mode = WebParam.Mode.OUT) Holder<Integer> i2) {
        i2.value = i;
        return "In:" + i;
    }
    @WebMethod
    public String testFloatPrim(float i,  @WebParam(mode = WebParam.Mode.OUT) Holder<Float> i2) {
        i2.value = i;
        return "In:" + i;
    }
    @WebMethod
    public String testFloat(Float i,  @WebParam(mode = WebParam.Mode.OUT) Holder<Float> i2) {
        i2.value = i;
        return "In:" + i;
    }
    @WebMethod
    public String testBooleanPrim(boolean i,  @WebParam(mode = WebParam.Mode.OUT) Holder<Boolean> i2) {
        i2.value = i;
        return "In:" + i;
    }
    @WebMethod
    public String testBoolean(Boolean i,  @WebParam(mode = WebParam.Mode.OUT) Holder<Boolean> i2) {
        i2.value = i;
        return "In:" + i;
    }
    @WebMethod
    public String testLongPrim(long i,  @WebParam(mode = WebParam.Mode.OUT) Holder<Long> i2) {
        i2.value = i;
        return "In:" + i;
    }
    @WebMethod
    public String testLong(Long i,  @WebParam(mode = WebParam.Mode.OUT) Holder<Long> i2) {
        i2.value = i;
        return "In:" + i;
    }
    
    @WebMethod 
    public String testBase64Binary(byte i[],  @WebParam(mode = WebParam.Mode.OUT) Holder<byte[]> i2) {
        i2.value = i;
        return "In:" + IOUtils.newStringFromBytes(i);
    }
}
