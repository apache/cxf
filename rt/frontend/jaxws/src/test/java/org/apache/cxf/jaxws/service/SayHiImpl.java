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

import javax.jws.WebService;

@WebService(serviceName = "SayHiService",
            portName = "HelloPort",
            targetNamespace = "http://mynamespace.com/",
            endpointInterface = "org.apache.cxf.jaxws.service.SayHi")
public class SayHiImpl implements SayHi {
    public long sayHi(long arg) {
        return arg;
    }
    public void greetMe() {

    }
    public String[] getStringArray(String[] strs) {
        String[] strings = new String[2];
        strings[0] = "Hello" + strs[0];
        strings[1] = "Bonjour" + strs[1];
        return strings;
    }
    public List<String> getStringList(List<String> list) {
        List<String> ret = new ArrayList<>();
        ret.add("Hello" + list.get(0));
        ret.add("Bonjour" + list.get(1));
        return ret;
    }

}
