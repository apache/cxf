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

import java.util.ArrayList;
import java.util.List;

public class JavaServiceClass extends JavaClass {

    private final List<JavaPort> ports = new ArrayList<JavaPort>();
  
    private String serviceName;
    private String classJavaDoc;
    private String packageJavaDoc;
    
    public JavaServiceClass(JavaModel model) {
        super(model);
    }

    public void addPort(JavaPort port) {
        ports.add(port);
    }

    public List<JavaPort> getPorts() {
        return ports;
    }
    
    public void setServiceName(String name) {
        this.serviceName = name;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setPackageJavaDoc(String doc) {
        packageJavaDoc = doc;
    }
    
    public String getPackageJavaDoc() {   
        return (packageJavaDoc != null) ? packageJavaDoc : "";
    }
    
    public void setClassJavaDoc(String doc) {
        classJavaDoc = doc;
    }
    
    public String getClassJavaDoc() {   
        return (classJavaDoc != null) ? classJavaDoc : "";
    } 

}
