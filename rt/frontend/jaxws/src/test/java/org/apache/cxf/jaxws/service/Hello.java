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
package org.apache.cxf.jaxws.service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public class Hello {

    private boolean postConstructCalled;
    
    @PostConstruct
    @WebMethod(exclude = true)
    public void init() {
        postConstructCalled = true;
    }
    
    @WebMethod(exclude = true)
    public boolean isPostConstructCalled() {
        return postConstructCalled;
    }
    
    @WebMethod
    public String sayHi(String text) {
        return text;
    }
    
    @WebMethod(action = "myaction")
    public List<String> getGreetings() {
        List<String> strings = new ArrayList<String>();
        strings.add("Hello");
        strings.add("Bonjour");
        return strings;
    }
    
    @WebMethod
    public String[] getStringArray(String[] strs) {
        String[] strings = new String[2];
        strings[0] = "Hello" + strs[0];
        strings[1] = "Bonjour" + strs[1];
        return strings;
    }
    @WebMethod
    public List<String> getStringList(List<String> list) {
        List<String> ret = new ArrayList<String>();
        ret.add("Hello" + list.get(0));
        ret.add("Bonjour" + list.get(1));
        return ret;
    }
}
