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

    /**
     * As maven will set null for an empty parameter we need
     * this horrid inital value to tell if it has been 
     * configured or not yet.
     */
    private static final String DEFAULT_WSDL_LOCATION = "DEFAULTWSDLLOCATION - WORKAROUND";

    /**
     * 
     */
    protected List<String> packagenames;

    /**
     * Extra arguments to pass to the command-line code generator. For compatibility as well as to
     * specify any extra flags not addressed by other parameters
     */
    protected List<String> extraargs = new ArrayList<String>();

    /**
     * Extra arguments to pass to the XJC compiler command-line code generator.
     * For compatibility as well as to specify any extra flags not addressed by other parameters
     */
    protected List<String> xjcargs = new ArrayList<String>();

    /**
     * Directory where generated java classes will be created. Defaults to plugin 'sourceRoot' parameter
     */
    protected File outputDir;

    /**
     * Ignore the specified WSDL schema namespace when generating code.
     * Also, optionally specifies the Java package name used by types described in the excluded 
     * namespace(s) using schema-namespace[=java-packagename]
     */
    List<String> namespaceExcludes;

    /**
     * Enables or disables the loading of the default excludes namespace mapping. Default is true.
     */
    Boolean defaultExcludesNamespace;

    /**
     * Enables or disables the loading of the default namespace package name mapping. Default is true and 
     * <a href=""http://www.w3.org/2005/08/addressing">
     * http://www.w3.org/2005/08/addressing=org.apache.cxf.ws.addressingnamespace</a> 
     * package mapping will be enabled.
     */
    Boolean defaultNamespacePackageMapping;

    /**
     * A set of dependent files used to detect the generator must process WSDL, even 
     * if generator marker files are up to date.
     */
    File dependencies[];

    /**
     * Redundant directories to be deleted after code generation
     */
    File redundantDirs[];

    /**
     * Specifies JAXWS or JAXB binding files. Use spaces to separate multiple entries.
     */
    String bindingFiles[] = new String[0];

    /**
     * Specifies the value of the @WebServiceClient annotation's wsdlLocation property. 
     */
    String wsdlLocation = DEFAULT_WSDL_LOCATION;

    /**
     * Specifies that the wsdlurl contains a plain text, new line delimited,
     * list of wsdlurls instead of the wsdl itself.
     */
    boolean wsdlList;

    /**
     * Specifies the frontend. Default is JAXWS. Currently supports only JAXWS frontend.
     */
    String frontEnd;

    /**
     * Specifies the databinding. Default is JAXB. Currently supports only JAXB databinding.
     */
    String dataBinding;

    /**
     * Specifies the wsdl version .Default is WSDL1.1. Currently suppports only WSDL1.1 version.
     */
    String wsdlVersion;

    /**
     * Specify catalog file to map the imported wsdl/schema
     */
    String catalog;

    /**
     * Enables or disables processing of implicit SOAP headers (i.e. SOAP headers defined in the 
     * wsdl:binding but not wsdl:portType section.) Default is false.
     */
    boolean extendedSoapHeaders;

    /**
     * Enables validating the WSDL before generating the code. 
     */
    boolean validateWsdl;

    /**
     * The WSDL service name to use for the generated code
     */
    String serviceName;

    /**
     * Automatically resolve naming conflicts without requiring the use of binding customizations
     */
    boolean autoNameResolution;

    /**
     * Disable generation of service address binding in the generated Java classes
     */
    boolean noAddressBinding;
    
    /**
     * Allow element references when determining if an operation can be unwrapped or not 
     */
    boolean allowElementRefs;


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

    public List<String> getXJCargs() {
        return xjcargs;
    }

    public void setXJCargs(List<String> ea) {
        this.xjcargs.clear();
        this.xjcargs.addAll(ea);
    }

    public List<String> getPackagenames() {
        return packagenames;
    }

    public void setPackagenames(List<String> pn) {
        this.packagenames = pn;
    }

    public List<String> getNamespaceExcludes() {
        return namespaceExcludes;
    }

    public void setNamespaceExcludes(List<String> namespaceExcludes) {
        this.namespaceExcludes = namespaceExcludes;
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

    public void setWsdlLocation(String s) {
        wsdlLocation = s;
    }

    public String getWsdlLocation() {
        return isSetWsdlLocation() ? wsdlLocation : null;
    }

    public boolean isSetWsdlLocation() {
        return !DEFAULT_WSDL_LOCATION.equals(wsdlLocation);
    }

    public boolean isWsdlList() {
        return wsdlList;
    }

    public void setWsdlList(boolean wsdlList) {
        this.wsdlList = wsdlList;
    }

    public String getFrontEnd() {
        return frontEnd;
    }

    public void setFrontEnd(String frontEnd) {
        this.frontEnd = frontEnd;
    }

    public String getDataBinding() {
        return dataBinding;
    }

    public void setDataBinding(String dataBinding) {
        this.dataBinding = dataBinding;
    }

    public String getWsdlVersion() {
        return wsdlVersion;
    }

    public void setWsdlVersion(String wsdlVersion) {
        this.wsdlVersion = wsdlVersion;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public boolean isExtendedSoapHeaders() {
        return extendedSoapHeaders;
    }

    public void setExtendedSoapHeaders(boolean extendedSoapHeaders) {
        this.extendedSoapHeaders = extendedSoapHeaders;
    }

    public boolean isValidateWsdl() {
        return validateWsdl;
    }

    public void setValidateWsdl(boolean validateWsdl) {
        this.validateWsdl = validateWsdl;
    }

    public Boolean getDefaultExcludesNamespace() {
        return defaultExcludesNamespace;
    }

    public void setDefaultExcludesNamespace(Boolean defaultExcludesNamespace) {
        this.defaultExcludesNamespace = defaultExcludesNamespace;
    }

    public Boolean getDefaultNamespacePackageMapping() {
        return defaultNamespacePackageMapping;
    }

    public void setDefaultNamespacePackageMapping(Boolean defaultNamespacePackageMapping) {
        this.defaultNamespacePackageMapping = defaultNamespacePackageMapping;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public boolean isAutoNameResolution() {
        return autoNameResolution;
    }

    public void setAutoNameResolution(boolean autoNameResolution) {
        this.autoNameResolution = autoNameResolution;
    }

    public boolean isNoAddressBinding() {
        return noAddressBinding;
    }

    public void setNoAddressBinding(boolean noAddressBinding) {
        this.noAddressBinding = noAddressBinding;
    }

    public boolean isAllowElementRefs() {
        return allowElementRefs;
    }

    public void setAllowElementRefs(boolean allowElementRefs) {
        this.allowElementRefs = allowElementRefs;
    }

    public void copyOptions(Option destination) {
        destination.setAutoNameResolution(isAutoNameResolution());
        destination.setBindingFiles(getBindingFiles());
        destination.setCatalog(getCatalog());
        destination.setDataBinding(getDataBinding());
        destination.setDefaultExcludesNamespace(getDefaultExcludesNamespace());
        destination.setDefaultNamespacePackageMapping(getDefaultNamespacePackageMapping());
        destination.setDeleteDirs(getDeleteDirs());
        destination.setDependencies(getDependencies());
        destination.setExtendedSoapHeaders(isExtendedSoapHeaders());
        destination.setExtraargs(getExtraargs());
        destination.setXJCargs(getXJCargs());
        destination.setFrontEnd(getFrontEnd());
        destination.setNamespaceExcludes(namespaceExcludes);
        destination.setNoAddressBinding(isNoAddressBinding());
        destination.setOutputDir(getOutputDir());
        destination.setPackagenames(getPackagenames());
        destination.setServiceName(getServiceName());
        destination.setValidateWsdl(isValidateWsdl());
        destination.setAllowElementRefs(isAllowElementRefs());
        if (isSetWsdlLocation()) {
            destination.setWsdlLocation(getWsdlLocation());
        }
        destination.setWsdlVersion(getWsdlVersion());
    }
}
