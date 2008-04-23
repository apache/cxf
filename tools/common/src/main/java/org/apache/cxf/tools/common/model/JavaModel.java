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

package org.apache.cxf.tools.common.model;

import java.util.*;

public class JavaModel {

    private final Map<String, JavaInterface> interfaces;
    private final Map<String, JavaExceptionClass> exceptionClasses;
    private final Map<String, JavaServiceClass> serviceClasses;
    
    private String location;

    public JavaModel() {
        interfaces = new LinkedHashMap<String, JavaInterface>();
        exceptionClasses = new LinkedHashMap<String, JavaExceptionClass>();
        serviceClasses = new LinkedHashMap<String, JavaServiceClass>();
    }

    public void addInterface(String name, JavaInterface i) {
        this.interfaces.put(name, i);
    }

    public Map<String, JavaInterface> getInterfaces() {
        return this.interfaces;
    }


    public void addExceptionClass(String name, JavaExceptionClass ex) {
        this.exceptionClasses.put(name, ex);
    }
    
    public Map<String, JavaExceptionClass> getExceptionClasses() {
        return this.exceptionClasses;
    }

    public void addServiceClass(String name, JavaServiceClass service) {
        this.serviceClasses.put(name, service);
    }
    
    public Map<String, JavaServiceClass> getServiceClasses() {
        return this.serviceClasses;
    }

    public void setLocation(String l) {
        this.location = l;
    }

    public String getLocation() {
        return this.location;
    }
}
