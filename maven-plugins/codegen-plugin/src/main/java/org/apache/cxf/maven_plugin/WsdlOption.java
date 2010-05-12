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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.tools.util.URIParserUtil;

public class WsdlOption extends Option {

    /**
     * The WSDL file to process.
     */
    String wsdl;

    /**
     * Alternatively to the wsdl string an artifact can be specified
     */
    WsdlArtifact wsdlArtifact;

    public String getWsdl() {
        return wsdl;
    }

    public void setWsdl(String w) {
        wsdl = w;
    }

    public WsdlArtifact getWsdlArtifact() {
        return wsdlArtifact;
    }

    public void setWsdlArtifact(WsdlArtifact wsdlArtifact) {
        this.wsdlArtifact = wsdlArtifact;
    }
    
    /**
     * Try to find a file matching the wsdl path (either absolutely, relatively to the current dir or to
     * the project base dir)
     * 
     * @return wsdl file
     */
    public File getWsdlFile(File baseDir) {
        if (wsdl == null) {
            return null;
        }
        File file = null;
        try {
            URI uri = new URI(wsdl);
            if (uri.isAbsolute()) {
                file = new File(uri);
            }
        } catch (Exception e) {
            // ignore
        }
        if (file == null || !file.exists()) {
            file = new File(wsdl);
        }
        if (!file.exists()) {
            file = new File(baseDir, wsdl);
        }
        return file;
    }
    
    public URI getWsdlURI(URI baseURI) {
        String wsdlLocation = getWsdl();
        File wsdlFile = new File(wsdlLocation);
        return wsdlFile.exists() ? wsdlFile.toURI() 
            : baseURI.resolve(URIParserUtil.escapeChars(wsdlLocation));
    }

    public boolean isDefServiceName() {
        if (extraargs == null) {
            return false;
        }
        for (int i = 0; i < extraargs.size(); i++) {
            if ("-sn".equalsIgnoreCase(extraargs.get(i))) {
                return true;
            }
        }
        return false;

    }

    public int hashCode() {
        if (wsdl != null) {
            return wsdl.hashCode();
        }
        return -1;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof WsdlOption)) {
            return false;
        }

        WsdlOption t = (WsdlOption)obj;
        return t.getWsdl().equals(getWsdl());
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WSDL: ").append(wsdl).append('\n');
        builder.append("OutputDir: ").append(outputDir).append('\n');
        builder.append("Extraargs: ").append(extraargs).append('\n');
        builder.append("XJCargs: ").append(xjcargs).append('\n');
        builder.append("Packagenames: ").append(packagenames).append('\n');
        builder.append('\n');
        return builder.toString();
    }

    public List<String> generateCommandLine(File outputDirFile, URI basedir, URI wsdlURI, boolean debug) {
        List<String> list = new ArrayList<String>();
        addList(list, "-p", true, getPackagenames());
        addList(list, "-nexclude", true, getNamespaceExcludes());
        addIfNotNull(list, outputDirFile, "-d");
        for (String binding : getBindingFiles()) {
            File bindingFile = new File(binding);
            URI bindingURI = bindingFile.exists() ? bindingFile.toURI() : basedir.resolve(binding);
            list.add("-b");
            list.add(bindingURI.toString());
        }
        addIfNotNull(list, getFrontEnd(), "-fe");
        addIfNotNull(list, getDataBinding(), "-db");
        addIfNotNull(list, getWsdlVersion(), "-wv");
        addIfNotNull(list, getCatalog(), "-catalog");
        if (isExtendedSoapHeaders()) {
            list.add("-exsh");
            list.add("true");
        }
        addIfTrue(list, isAllowElementRefs(), "-allowElementRefs");
        addIfTrue(list, isValidateWsdl(), "-validate");
        addIfTrue(list, isUseFQCNForFaultSerialVersionUID(), "-useFQCNForFaultSerialVersionUID");
        addIfNotNull(list, getDefaultExcludesNamespace(), "-dex");
        addIfNotNull(list, getDefaultNamespacePackageMapping(), "-dns");
        addIfNotNull(list, getServiceName(), "-sn");
        addIfTrue(list, isAutoNameResolution(), "-autoNameResolution");
        addIfTrue(list, isNoAddressBinding(), "-noAddressBinding");
        addList(list, "-xjc", false, getXJCargs());
        addList(list, "", false, getExtraargs());
        if (isSetWsdlLocation()) {
            list.add("-wsdlLocation");
            list.add(getWsdlLocation() == null ? "" : getWsdlLocation());
        }
        addIfTrue(list, isWsdlList(), "-wsdlList");
        addIfTrue(list, debug && !list.contains("-verbose"), "-verbose");
        list.add(wsdlURI.toString());
        return list;
    }

    private static void addIfTrue(List<String> list, boolean expression, String key) {
        if (expression) {
            list.add(key);
        }
    }

    private static void addIfNotNull(List<String> list, Object value, String key) {
        if (value != null) {
            list.add(key);
            list.add(value.toString());
        }
    }

    private static void addList(List<String> destList, String key, boolean keyAsOwnElement,
                                List<String> sourceList) {
        if (sourceList == null) {
            return;
        }
        for (String value : sourceList) {
            if (keyAsOwnElement) {
                destList.add(key);
                destList.add(value);
            } else {
                // Maven makes empty tags into null
                // instead of empty strings. so replace null by ""
                destList.add(key + ((value == null) ? "" : value));
            }
        }
    }

}
