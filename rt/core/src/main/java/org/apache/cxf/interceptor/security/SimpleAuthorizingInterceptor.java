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
package org.apache.cxf.interceptor.security;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SimpleAuthorizingInterceptor extends AbstractAuthorizingInInterceptor {

    private Map<String, List<String>> methodRolesMap = Collections.emptyMap();
    private List<String> globalRoles = Collections.emptyList();
    
    
    @Override
    protected List<String> getExpectedRoles(Method method) {
        List<String> roles = methodRolesMap.get(method.getName());
        if (roles != null) {
            return roles;
        }
        return globalRoles;
    }



    public void setMethodRolesMap(Map<String, String> rolesMap) {
        methodRolesMap = new HashMap<String, List<String>>();
        for (Map.Entry<String, String> entry : rolesMap.entrySet()) {
            methodRolesMap.put(entry.getKey(), Arrays.asList(entry.getValue().split(" ")));
        }
    }
    
    public void setGlobalRoles(String roles) {
        globalRoles = Arrays.asList(roles.split(" "));
    }



    

}
