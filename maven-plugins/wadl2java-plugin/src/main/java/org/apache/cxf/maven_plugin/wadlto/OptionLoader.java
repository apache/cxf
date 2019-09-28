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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.maven_plugin.common.DocumentArtifact;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

public final class OptionLoader {
    private static final String WADL_TYPE = "wadl";
    private static final String WADL_OPTIONS = "-options$";
    private static final String WADL_BINDINGS = "-binding-?\\d*.xml$";

    private OptionLoader() {
    }

    public static List<WadlOption> loadWsdlOptionsFromDependencies(MavenProject project,
                                                                   Option defaultOptions, File outputDir) {
        List<WadlOption> options = new ArrayList<>();
        Set<Artifact> dependencies = project.getDependencyArtifacts();
        for (Artifact artifact : dependencies) {
            WadlOption option = generateWsdlOptionFromArtifact(artifact, outputDir);
            if (option != null) {
                if (defaultOptions != null) {
                    option.merge(defaultOptions);
                }
                options.add(option);
            }
        }
        return options;
    }

    private static WadlOption generateWsdlOptionFromArtifact(Artifact artifact, File outputDir) {
        if (!WADL_TYPE.equals(artifact.getType())) {
            return null;
        }
        WadlOption option = new WadlOption();
        DocumentArtifact wsdlArtifact = new DocumentArtifact();
        wsdlArtifact.setArtifactId(artifact.getArtifactId());
        wsdlArtifact.setGroupId(artifact.getGroupId());
        wsdlArtifact.setType(artifact.getType());
        wsdlArtifact.setVersion(artifact.getVersion());
        option.setWadlArtifact(wsdlArtifact);
        option.setOutputDir(outputDir);
        return option;
    }

    /**
     * Scan files in a directory and generate one wadlOption per file found. Extra args for code generation
     * can be defined in a file that is named like the wadl file and ends in -options. Binding files can be
     * defined in files named like the wadl file and end in -binding-*.xml
     *
     * @param wadlBasedir
     * @param includes file name patterns to include
     * @param excludes file name patterns to exclude
     * @param defaultOptions options that should be used if no special file is given
     * @return list of one WadlOption object for each wadl found
     * @throws MojoExecutionException
     */
    public static List<WadlOption> loadWadlOptionsFromFile(File wadlBasedir,
                                                           String[] includes,
                                                            String[] excludes,
                                                            Option defaultOptions,
                                                            File defaultOutputDir)
        throws MojoExecutionException {

        if (wadlBasedir == null) {
            return new ArrayList<>();
        }

        if (!wadlBasedir.exists()) {
            throw new MojoExecutionException(wadlBasedir + " does not exist");
        }

        List<File> wadlFiles = getWadlFiles(wadlBasedir, includes, excludes);
        List<WadlOption> wadlOptions = new ArrayList<>();
        for (File wadl : wadlFiles) {
            WadlOption wadlOption = generateWadlOptionFromFile(wadl, defaultOptions, defaultOutputDir);
            if (wadlOption != null) {
                wadlOptions.add(wadlOption);
            }
        }
        return wadlOptions;
    }

    private static String joinWithComma(String[] arr) {
        if (arr == null || arr.length == 0) {
            return "";
        }
        return String.join(",", arr);
    }

    private static List<File> getWadlFiles(File dir, String[] includes, String[] excludes)
        throws MojoExecutionException {

        List<String> exList = new ArrayList<>();
        if (excludes != null) {
            Collections.addAll(exList, excludes);
        }
        Collections.addAll(exList, org.codehaus.plexus.util.FileUtils.getDefaultExcludes());

        String inc = joinWithComma(includes);
        String ex = joinWithComma(exList.toArray(new String[0]));

        try {
            List<?> newfiles = org.codehaus.plexus.util.FileUtils.getFiles(dir, inc, ex);
            return CastUtils.cast(newfiles);
        } catch (IOException exc) {
            throw new MojoExecutionException(exc.getMessage(), exc);
        }
    }


    protected static WadlOption generateWadlOptionFromFile(final File wadl,
                                                           final Option defaultOptions,
                                                           File defaultOutputDir)
        throws MojoExecutionException {

        if (wadl == null || !wadl.exists()) {
            return null;
        }

        final String wadlFileName = wadl.getName();
        int idx = wadlFileName.toLowerCase().lastIndexOf(".wadl");
        if (idx == -1) {
            idx = wadlFileName.lastIndexOf('.');
        }
        if (idx == -1) {
            return null;
        }

        final WadlOption wadlOption = new WadlOption();
        if (defaultOptions != null) {
            wadlOption.merge(defaultOptions);
        }


        final String wadlName = wadlFileName.substring(0, idx);

        final String[] options = readOptionsFromFile(wadl.getParentFile(), wadlName);
        if (options.length > 0) {
            Collections.addAll(wadlOption.getExtraargs(), options);
        }

        List<File> bindingFiles = FileUtils.getFiles(wadl.getParentFile(), wadlName + WADL_BINDINGS);
        if (bindingFiles != null) {
            for (File binding : bindingFiles) {
                wadlOption.addBindingFile(binding);
            }
        }
        wadlOption.setWadl(wadl.toURI().toString());

        if (wadlOption.getOutputDir() == null) {
            wadlOption.setOutputDir(defaultOutputDir);
        }

        return wadlOption;
    }

    private static String[] readOptionsFromFile(File dir, String wsdlName) throws MojoExecutionException {
        String[] noOptions = new String[] {};
        List<File> files = FileUtils.getFiles(dir, wsdlName + WADL_OPTIONS);
        if (files.isEmpty()) {
            return noOptions;
        }
        File optionsFile = files.iterator().next();
        if (optionsFile == null || !optionsFile.exists()) {
            return noOptions;
        }
        try {
            List<String> lines = FileUtils.readLines(optionsFile);
            if (lines.isEmpty()) {
                return noOptions;
            }
            return lines.iterator().next().split(" ");
        } catch (Exception e) {
            throw new MojoExecutionException("Error reading options from file "
                                             + optionsFile.getAbsolutePath(), e);
        }
    }
}
