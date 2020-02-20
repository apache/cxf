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

package org.apache.cxf.maven_plugin.wadlto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class Option {


    /**
     * Directory where generated java classes will be created. Defaults to plugin 'sourceRoot' parameter
     */
    protected File outputDir;

    /**
     * A set of dependent files used to detect the generator must process WSDL, even
     * if generator marker files are up to date.
     */
    File[] dependencies;

    /**
     * Redundant directories to be deleted after code generation
     */
    File[] redundantDirs;

    /**
     * Extra arguments to pass to the command-line code generator. For compatibility as well as to
     * specify any extra flags not addressed by other parameters
     */
    List<String> extraargs = new ArrayList<>();

    /**
     * Specifies JAXB binding files. Use spaces to separate multiple entries.
     */
    String[] bindingFiles = new String[0];

    /**
     * Specifies catalog file to map the imported wadl/schema
     */
    String catalog;

    /**
     * Specifies resource id
     */
    private String resourcename;

    /**
     * Specifies package name of WADL resource elements
     */
    private String packagename;

    /**
     * Enables or disables generation of the impl classes. Default value is false.
     * If set then only implementation classes will be generated
     */
    private Boolean generateImpl;

    /**
     * Enables or disables generation of the interface classes. Setting this property
     * only makes sense when generateImpl is also set. In other cases it is ignored and
     * interfaces are always generated.
     *
     *
     *
     */
    private Boolean generateInterface;

    /**
     *
     */
    private List<String> schemaPackagenames = new ArrayList<>();
    
    /**
     * Specifies the library to use for JAX-RS 2.1 reactive extensions
     */
    private String rx;


    public Option() {
        super();
    }

    public void setDependencies(File[] files) {
        dependencies = files;
    }

    public File[] getDependencies() {
        return dependencies;
    }

    public void setDeleteDirs(File[] files) {
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

    public void setBindingFiles(String[] files) {
        bindingFiles = files;
    }
    public String[] getBindingFiles() {
        return bindingFiles;
    }
    public void addBindingFile(File file) {
        String[] tmp = new String[bindingFiles.length + 1];
        System.arraycopy(bindingFiles, 0, tmp, 0, bindingFiles.length);
        bindingFiles = tmp;
        bindingFiles[bindingFiles.length - 1] = file.getAbsolutePath();
    }

    public List<String> getSchemaPackagenames() {
        return schemaPackagenames;
    }

    public void setSchemaPackagenames(List<String> pn) {
        this.schemaPackagenames = pn;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getPackagename() {
        return packagename;
    }

    public void setPackagename(String name) {
        this.packagename = name;
    }

    public void setResourcename(String resourceName) {
        this.resourcename = resourceName;
    }

    public String getResourcename() {
        return resourcename;
    }

    public boolean isImpl() {
        return generateImpl != null && generateImpl;
    }

    public void setImpl(boolean impl) {
        this.generateImpl = impl;
    }

    public boolean isInterface() {
        return generateInterface != null && generateInterface;
    }

    public void setInterface(boolean interf) {
        this.generateInterface = interf;
    }

    public List<String> getExtraargs() {
        return extraargs;
    }

    public void setExtraargs(List<String> ea) {
        this.extraargs.clear();
        this.extraargs.addAll(ea);
    }

    public String getRx() {
        return rx;
    }

    public void setRx(String rx) {
        this.rx = rx;
    }
    
    public void copyOptions(Option destination) {
        destination.setBindingFiles(getBindingFiles());
        destination.setCatalog(getCatalog());
        destination.setResourcename(getResourcename());
        destination.setSchemaPackagenames(getSchemaPackagenames());
        destination.setDeleteDirs(getDeleteDirs());
        destination.setDependencies(getDependencies());
        destination.setOutputDir(getOutputDir());
        destination.setExtraargs(getExtraargs());
        destination.setRx(getRx());
    }

    private <T> T setIfNull(T dest, T source) {
        if (dest == null) {
            dest = source;
        }
        return dest;
    }

    public void merge(Option defaultOptions) {
        catalog = setIfNull(catalog, defaultOptions.catalog);
        generateImpl = setIfNull(generateImpl, defaultOptions.generateImpl);
        generateInterface = setIfNull(generateInterface, defaultOptions.generateInterface);
        packagename = setIfNull(packagename, defaultOptions.packagename);
        outputDir = setIfNull(outputDir, defaultOptions.outputDir);
        bindingFiles = mergeList(bindingFiles, defaultOptions.bindingFiles, String.class);
        dependencies = mergeList(dependencies, defaultOptions.dependencies, File.class);
        redundantDirs = mergeList(redundantDirs, defaultOptions.redundantDirs, File.class);
        schemaPackagenames.addAll(defaultOptions.schemaPackagenames);
        rx = setIfNull(rx, defaultOptions.rx);
        extraargs.addAll(defaultOptions.extraargs);
    }

    @SuppressWarnings("unchecked")
    private <T> T[] mergeList(T[] l1, T[] l2, Class<T> cls) {
        if (l1 == null) {
            return l2;
        } else if (l2 == null) {
            return l1;
        }
        int len = l1.length + l2.length;
        T[] ret = (T[])java.lang.reflect.Array.newInstance(cls, len);
        System.arraycopy(l1, 0, ret, 0, l1.length);
        System.arraycopy(l2, 0, ret, l1.length, l2.length);
        return ret;
    }
}
