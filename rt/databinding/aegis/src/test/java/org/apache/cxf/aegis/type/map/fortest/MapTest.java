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

package org.apache.cxf.aegis.type.map.fortest;

import java.util.Map;

import javax.jws.WebService;

import org.apache.cxf.aegis.type.map.ns2.ObjectWithAMapNs2;

/**
 * 
 */
@WebService(targetNamespace = "uri:org.apache.cxf.aegis.fortest.map", 
            name = "MapTest")
public interface MapTest {
    ObjectWithAMap returnObjectWithAMap();
    void takeMap(ObjectWithAMap map);
    Map<String, Long> getMapStringToLong();
    Map getRawMapStringToInteger();
    Map<Long, String> getMapLongToString();
    
    ObjectWithAMapNs2 returnObjectWithAMapNs2();
    void takeMapNs2(ObjectWithAMapNs2 map);
    
}
