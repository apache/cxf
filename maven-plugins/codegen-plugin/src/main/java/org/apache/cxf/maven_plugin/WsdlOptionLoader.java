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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;

public final class WsdlOptionLoader {
    private static final String WSDL_OPTIONS = "-options$";
    private static final String WSDL_BINDINGS = "-binding-?\\d*.xml$";
    
    private WsdlOptionLoader() {
    }

    /**
     * Scan files in a directory and generate one wsdlOption per file found. Extra args for code generation
     * can be defined in a file that is named like the wsdl file and ends in -options. Binding files can be
     * defined in files named like the wsdl file and end in -binding-*.xml
     * 
     * @param wsdlBasedir
     * @param includes file name patterns to include
     * @param excludes file name patterns to exclude
     * @param defaultOptions options that should be used if no special file is given
     * @return list of one WsdlOption object for each wsdl found
     * @throws MojoExecutionException
     */
    public static List<WsdlOption> loadWsdlOptionsFromFiles(File wsdlBasedir, String includes[],
                                                            String excludes[], Option defaultOptions,
                                                            File defaultOutputDir)
        throws MojoExecutionException {

        if (wsdlBasedir == null) {
            return new ArrayList<WsdlOption>();
        }

        if (!wsdlBasedir.exists()) {
            throw new MojoExecutionException(wsdlBasedir + " does not exist");
        }

        List<File> wsdlFiles = getWsdlFiles(wsdlBasedir, includes, excludes);
        List<WsdlOption> wsdlOptions = new ArrayList<WsdlOption>();
        for (File wsdl : wsdlFiles) {
            WsdlOption wsdlOption = generateWsdlOptionFromFile(wsdl, defaultOptions, defaultOutputDir);
            if (wsdlOption != null) {
                wsdlOptions.add(wsdlOption);
            }
        }
        return wsdlOptions;
    }

    private static String joinWithComma(String[] arr) {
        if (arr == null) {
            return "";
        }
        StringBuilder str = new StringBuilder();

        if (arr != null) {
            for (String s : arr) {
                if (str.length() > 0) {
                    str.append(',');
                }
                str.append(s);
            }
        }
        return str.toString();
    }

    private static List<File> getWsdlFiles(File dir, String includes[], String excludes[])
        throws MojoExecutionException {

        List<String> exList = new ArrayList<String>();
        if (excludes != null) {
            exList.addAll(Arrays.asList(excludes));
        }
        exList.addAll(Arrays.asList(org.codehaus.plexus.util.FileUtils.getDefaultExcludes()));

        String inc = joinWithComma(includes);
        String ex = joinWithComma(exList.toArray(new String[exList.size()]));

        try {
            List newfiles = org.codehaus.plexus.util.FileUtils.getFiles(dir, inc, ex);
            return CastUtils.cast(newfiles);
        } catch (IOException exc) {
            throw new MojoExecutionException(exc.getMessage(), exc);
        }
    }

    private static String[] readOptionsFromFile(File dir, String wsdlName) throws MojoExecutionException {
        String[] noOptions = new String[] {};
        List<File> files = FileUtils.getFiles(dir, wsdlName + WSDL_OPTIONS);
        if (files.size() <= 0) {
            return noOptions;
        }
        File optionsFile = files.iterator().next();
        if (optionsFile == null || !optionsFile.exists()) {
            return noOptions;
        }
        try {
            List<String> lines = FileUtils.readLines(optionsFile);
            if (lines.size() <= 0) {
                return noOptions;
            }
            return lines.iterator().next().split(" ");
        } catch (Exception e) {
            throw new MojoExecutionException("Error reading options from file "
                                             + optionsFile.getAbsolutePath(), e);
        }
    }

    protected static WsdlOption generateWsdlOptionFromFile(final File wsdl, final Option defaultOptions,
                                                           File defaultOutputDir)
        throws MojoExecutionException {

        if (wsdl == null || !wsdl.exists()) {
            return null;
        }

        final String wsdlFileName = wsdl.getName();
        int idx = wsdlFileName.toLowerCase().lastIndexOf(".wsdl");
        if (idx == -1) {
            idx = wsdlFileName.lastIndexOf('.');
        }
        if (idx == -1) {
            return null;
        }

        final WsdlOption wsdlOption = new WsdlOption();
        final String wsdlName = wsdlFileName.substring(0, idx);

        final String[] options = readOptionsFromFile(wsdl.getParentFile(), wsdlName);
        if (options.length > 0) {
            wsdlOption.getExtraargs().addAll(Arrays.asList(options));
        } else if (defaultOptions != null) {
            // no options specified use the defaults
            defaultOptions.copyOptions(wsdlOption);
        }

        List<File> bindingFiles = FileUtils.getFiles(wsdl.getParentFile(), wsdlName + WSDL_BINDINGS);
        if (bindingFiles != null) {
            for (File binding : bindingFiles) {
                wsdlOption.addBindingFile(binding);
            }
        }
        wsdlOption.setWsdl(wsdl.toURI().toString());

        if (wsdlOption.getOutputDir() == null) {
            wsdlOption.setOutputDir(defaultOutputDir);
        }

        return wsdlOption;
    }
}
