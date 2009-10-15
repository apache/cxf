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

package org.apache.cxf.jaxws.spring;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 */
public class ClientHolderBean {

    @Autowired(required = true)
    Collection<org.apache.hello_world_soap_http.Greeter> greeters;
    
    org.apache.hello_world_soap_http.Greeter greet1;
    org.apache.hello_world_soap_http.Greeter greet2;
    
    
    public int greeterCount() {
        return greeters.size();
    }
    
    public void setGreet1(org.apache.hello_world_soap_http.Greeter g1) {
        greet1 = g1;
    }
    public void setGreet2(org.apache.hello_world_soap_http.Greeter g1) {
        greet2 = g1;
    }
    public org.apache.hello_world_soap_http.Greeter getGreet1() {
        return greet1;
    }
    public org.apache.hello_world_soap_http.Greeter getGreet2() {
        return greet2;
    }

}
