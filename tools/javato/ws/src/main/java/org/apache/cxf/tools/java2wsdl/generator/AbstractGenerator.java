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

package org.apache.cxf.tools.java2wsdl.generator;

import java.io.File;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ToolContext;

public abstract class AbstractGenerator<T> {
    private ServiceInfo service;
    private boolean allowImports;
    private File outputdir;
    private Bus bus;
    private ToolContext context;
    
    public void setToolContext(ToolContext arg) {
        this.context = arg;
    }
    public ToolContext getToolContext() {
        return this.context;
    }

    public void setOutputBase(File out) {
        this.outputdir = out;
    }

    public File getOutputBase() {
        return this.outputdir;
    }

    public void setServiceModel(ServiceInfo s) {
        this.service = s;
    }

    public ServiceInfo getServiceModel() {
        return this.service;
    }
    
    public Bus getBus() {
        return bus;
    }
    public void setBus(Bus b) {
        bus = b;
    }
    
    public void setAllowImports(boolean b) {
        allowImports = b;
    }
    public boolean allowImports() {
        return allowImports;
    }

    public abstract T generate(File file);

    protected File createOutputDir(File file) {
        String parent = file.getParent();
        if (parent == null) {
            return null;
        }
        File parentDir = new File(parent);
        if (parentDir.isDirectory() && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        return parentDir;
    }
}
