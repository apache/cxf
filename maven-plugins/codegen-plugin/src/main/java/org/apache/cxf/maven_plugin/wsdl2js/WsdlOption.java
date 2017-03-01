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

package org.apache.cxf.maven_plugin.wsdl2js;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.maven_plugin.WsdlArtifact;

/**
 * An option for javascript generation.
 */
public class WsdlOption extends Option implements org.apache.cxf.maven_plugin.GenericWsdlOption {

    private String wsdl;
    private WsdlArtifact artifact;

    /**
     * @return Pathname or URI to wsdl.
     */
    public String getWsdl() {
        return wsdl;
    }

    /**
     * Set pathname or URI to WSDL.
     * @param wsdl path.
     */
    public void setWsdl(String wsdl) {
        this.wsdl = wsdl;
    }

    /**
     * Maven coordinates
     * @return
     */
    public WsdlArtifact getArtifact() {
        return artifact;
    }
    public void setArtifact(WsdlArtifact artifact) {
        this.artifact = artifact;
    }

    public String getUri() {
        return wsdl;
    }

    public void setUri(String uri) {
        this.wsdl = uri;
    }

    public File[] getDeleteDirs() {
        /*
         * Until we figure out what this amounts to. I suspect it stays null for Javascript.
         */
        return new File[0];
    }

    public List<String> generateCommandLine(File outputDirFile, URI basedir, URI wsdlURI, boolean debug) {
        List<String> options = new ArrayList<>();
        if (wsdlVersion != null && !"".equals(wsdlVersion)) {
            options.add("-wv");
            options.add(wsdlVersion);
        }
        if (packagePrefixes != null) {
            for (UriPrefixPair upp : packagePrefixes) {
                options.add("-p");
                options.add(String.format("%s=%s", upp.getPrefix(), upp.getUri()));
            }
        }
        if (catalog != null) {
            options.add("-catalog");
            options.add(catalog.getAbsolutePath());
        }

        options.add("-d");
        if (output != null) {
            options.add(output.getAbsolutePath());
        } else {
            options.add(outputDirFile.getAbsolutePath());
        }
        if (validate != null) {
            options.add("-validate=" + validate);
        }
        if (debug) {
            options.add("-v");
            options.add("-V");
        }
        /*
         * By the time we get here there's supposed to be a string
         * in 'wsdl' that we can use as the uri.
         */
        options.add(wsdl);
        return options;
    }

    @Override
    public String toString() {
        return String.format("WsdlOption [wsdl=%s, artifact=%s, %s]", wsdl, artifact,
                             super.toString());
    }
}
