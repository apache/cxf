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
import java.util.Arrays;

public class Option {

    /**
     * Since an arbitrary URI can't be an XML element name,
     * these pairs are used to specify a mapping from URI
     * to prefix.
     */
    public static class UriPrefixPair {
        /**
         * The namespace URI.
         */
        String uri;
        /**
         * The identifier prefix.
         */
        String prefix;
        /**
         * @return the uri
         */
        public String getUri() {
            return uri;
        }
        /**
         * Set the URI.
         * @param uri the uri.
         */
        public void setUri(String uri) {
            this.uri = uri;
        }
        /**
         * @return the prefix.
         */
        public String getPrefix() {
            return prefix;
        }
        /**
         * Set the prefix.
         * @param prefix the prefix.
         */
        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

    }

    /**
     * mappings from namespace URIs to javascript identifier prefixes.
     */
    UriPrefixPair[] packagePrefixes;
    /**
     * OASIS catalog file for use when reading the WSDL.
     */
    File catalog;
    /**
     * Destination directory for the output.
     */
    File output;
    /**
     * Whether to validate the WSDL.
     */
    String validate;
    /**
     * The wsdl version.
     */
    String wsdlVersion;
    /**
     * A set of dependent files used to detect that the generator must process WSDL, even
     * if generator marker files are up to date.
     */
    File[] dependencies;

    public Option() {
    }

    public void merge(Option other) {
        if (catalog == null) {
            catalog = other.getCatalog();
        }
        if (output == null) {
            output = other.getOutput();
        }
        if (validate == null) {
            validate = other.getValidate();
        }
        if (wsdlVersion == null) {
            wsdlVersion = other.getWsdlVersion();
        }
    }

    /**
     * @return mappings from namespace URI to javascript name prefix.
     */
    public UriPrefixPair[] getPackagePrefixes() {
        return packagePrefixes;
    }

    /**
     * Set the mappings from namespace URI to Javascript name prefixes.
     * @param packagePrefixes
     */
    public void setPackagePrefixes(UriPrefixPair[] packagePrefixes) {
        this.packagePrefixes = packagePrefixes;
    }

    /**
     * @return catalog used to resolve XML URIs in the wsdl.
     */
    public File getCatalog() {
        return catalog;
    }

    /**
     * Set catalog used to resolve XML URIs in the wsdl.
     * @param catalog catalog.
     */
    public void setCatalog(File catalog) {
        this.catalog = catalog;
    }

    /**
     * @return output directory. Default is set
     * at the plugin level.
     */
    public File getOutput() {
        return output;
    }

    /**
     * Set the output directory.
     * @param output output directory.
     */
    public void setOutput(File output) {
        this.output = output;
    }

    /**
     * @return Validating the WSDL?
     */
    public String getValidate() {
        return validate;
    }

    /**
     * Control WSDL validation.
     * @param validate true or all to validate.
     */
    public void setValidate(String validate) {
        this.validate = validate;
    }

    public File getOutputDir() {
        return output;
    }

    public void setOutputDir(File outputDir) {
        output = outputDir;
    }

    public String getWsdlVersion() {
        return wsdlVersion;
    }

    public void setWsdlVersion(String wsdlVersion) {
        this.wsdlVersion = wsdlVersion;
    }

    public File[] getDependencies() {
        return dependencies;
    }

    public void setDependencies(File[] dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public String toString() {
        return String
            .format("Option [packagePrefixes=%s, catalog=%s, output=%s, "
                    + " validate=%s, wsdlVersion=%s, dependencies=%s]",
                    Arrays.toString(packagePrefixes), catalog, output, validate, wsdlVersion,
                    Arrays.toString(dependencies));
    }

}
