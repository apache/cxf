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

package org.apache.cxf.maven_plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Option {

    protected List<String> packagenames;
    protected List<String> extraargs = new ArrayList<String>();
    protected File outputDir;
    File dependencies[];
    File redundantDirs[];
    String bindingFiles[] = new String[0];

    public Option() {
        super();
    }

    public List<String> getExtraargs() {
        return extraargs;
    }

    public void setExtraargs(List<String> ea) {
        this.extraargs.clear();
        this.extraargs.addAll(ea);
    }

    public List<String> getPackagenames() {
        return packagenames;
    }

    public void setPackagenames(List<String> pn) {
        this.packagenames = pn;
    }

    public void setDependencies(File files[]) {
        dependencies = files;
    }

    public File[] getDependencies() {
        return dependencies;
    }

    public void setDeleteDirs(File files[]) {
        redundantDirs = files;
    }

    public File[] getDeleteDirs() {
        return redundantDirs;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File f) {
        outputDir = f;
    }
    
    public void setBindingFiles(String files[]) {
        bindingFiles = files;
    }
    public String[] getBindingFiles() {
        return bindingFiles;
    }
    public void addBindingFile(File file) {
        String tmp[] = new String[bindingFiles.length + 1];
        System.arraycopy(bindingFiles, 0, tmp, 0, bindingFiles.length);
        bindingFiles = tmp;
        bindingFiles[bindingFiles.length - 1] = file.toURI().toString();
    }
    

}
