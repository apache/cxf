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

package org.apache.cxf.systest.aegis;

import java.util.Map;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.apache.cxf.systest.aegis.bean.Item;

/**
 *
 */
@WebService(name = "AegisJaxWs", targetNamespace = "http://test.cxf.apache.org")
public interface AegisJaxWs {
    @WebMethod
    void addItem(@WebParam(name = "item")
                 Item item);

    @WebMethod(operationName = "getItemsAsMap")
    Map<?, ?> getItemsMap();

    @WebMethod(operationName = "getItemsAsMapSpecified")
    Map<Integer, Item> getItemsMapSpecified();

    @WebMethod
    @org.apache.cxf.aegis.type.java5.XmlElement(nillable = false, minOccurs = "1")
    Item getItemByKey(@WebParam(name = "key1")
                      String key1, @WebParam(name = "key2")
                      String key2);

    @WebMethod
    Integer getSimpleValue(@WebParam(name = "a")Integer a, @WebParam(name = "b")String b);

    //try comment this method
    @WebMethod
    java.util.List<String> getStringList();

    @WebMethod
    java.util.List<String> echoBigList(@WebParam(name = "foo") java.util.List<String> l);

    @WebMethod
    byte[] export(java.util.List<java.lang.Integer> integers);
}
