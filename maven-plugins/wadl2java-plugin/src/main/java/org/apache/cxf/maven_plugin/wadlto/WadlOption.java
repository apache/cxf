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
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.cxf.common.util.ClasspathScanner;
import org.apache.cxf.common.util.URIParserUtil;
import org.apache.cxf.maven_plugin.common.DocumentArtifact;

public class WadlOption extends Option {

    /**
     * The WADL file to process.
     */
    String wadl;

    String wadlFileExtension = "wadl";

    /**
     * Alternatively to the wadl string an artifact can be specified
     */
    DocumentArtifact wadlArtifact;

    public WadlOption() {
        super();
    }

    public String getWadl() {
        return wadl;
    }

    public void setWadl(String w) {
        wadl = w;
    }

    public DocumentArtifact getWadlArtifact() {
        return wadlArtifact;
    }

    public void setWadlArtifact(DocumentArtifact wadlArtifact) {
        this.wadlArtifact = wadlArtifact;
    }

    /**
     * Try to find a file matching the wadl path (either absolutely, relatively to the current dir or to
     * the project base dir)
     *
     * @return wadl file
     */
    public File getDocumentFile(File baseDir) {
        if (wadl == null) {
            return null;
        }
        File file = null;
        try {
            URI uri = new URI(wadl);
            if (uri.isAbsolute()) {
                file = new File(uri);
            }
        } catch (Exception e) {
            // ignore
        }
        if (file == null || !file.exists()) {
            file = new File(wadl);
        }
        if (!file.exists()) {
            file = new File(baseDir, wadl);
        }
        return file;
    }

    public List<URI> getWadlURIs(URI baseURI, ClassLoader resourceLoader) {
        String wadlLocation = getWadl();
        if (wadlLocation.contains(".") && !wadlLocation.contains("*")) {
            return Collections.singletonList(getWadlURI(baseURI, wadlLocation));
        }
        List<URI> uris = new LinkedList<>();
        try {
            for (URL nextLocation : ClasspathScanner.findResources(wadlLocation, wadlFileExtension, resourceLoader)) {
                uris.add(getWadlURI(baseURI, nextLocation.toURI().getPath()));
            }
        } catch (Exception ex) {
            // ignore
        }
        return uris;
    }

    private URI getWadlURI(URI baseURI, String wadlLocation) {
        File wadlFile = new File(wadlLocation);
        return wadlFile.exists() ? wadlFile.toURI()
            : baseURI.resolve(URIParserUtil.escapeChars(wadlLocation));
    }


    public int hashCode() {
        if (wadl != null) {
            return wadl.hashCode();
        }
        return -1;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof WadlOption)) {
            return false;
        }

        WadlOption t = (WadlOption)obj;
        return t.getWadl().equals(getWadl());
    }

    public String toString() {
        StringBuilder builder = new StringBuilder(64);
        builder.append("WADL: ").append(wadl).append('\n');
        builder.append("OutputDir: ").append(outputDir).append('\n');
        builder.append('\n');
        return builder.toString();
    }

    public List<String> generateCommandLine(File outputDirFile, URI basedir, URI wadlURI, boolean debug) {
        List<String> list = new ArrayList<>();
        addIfNotNull(list, outputDirFile, "-d");
        for (String binding : getBindingFiles()) {
            File bindingFile = new File(binding);
            URI bindingURI = bindingFile.exists() ? bindingFile.toURI() : basedir.resolve(binding);
            list.add("-b");
            list.add(bindingURI.toString());
        }
        addIfNotNull(list, getCatalog(), "-catalog");
        addIfNotNull(list, getResourcename(), "-resource");
        addIfNotNull(list, getPackagename(), "-p");
        addList(list, "-sp", true, getSchemaPackagenames());
        addIfTrue(list, isImpl(), "-impl");
        addIfTrue(list, isInterface(), "-interface");
        addIfNotNull(list, getRx(), "-rx");
        addList(list, "", false, getExtraargs());
        list.add(wadlURI.toString());
        return list;
    }


    // TODO: the following 3 helpers can go to a superclass or common utility class
    //       to be used by WADL and WSDL Pptions
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

    private static void addIfTrue(List<String> list, boolean expression, String key) {
        if (expression) {
            list.add(key);
        }
    }

    public String getWadlFileExtension() {
        return wadlFileExtension;
    }

    public void setWadlFileExtension(String wadlFileExtension) {
        this.wadlFileExtension = wadlFileExtension;
    }
}
