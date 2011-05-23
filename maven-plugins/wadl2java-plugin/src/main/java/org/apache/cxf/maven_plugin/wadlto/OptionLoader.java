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
import java.util.Arrays;
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
    private static final String WADL_BINDINGS = "-binding-?\\d*.xml$";
    
    private OptionLoader() {
    }
    
    @SuppressWarnings("unchecked")
    public static List<WadlOption> loadWsdlOptionsFromDependencies(MavenProject project, 
                                                                   Option defaultOptions, File outputDir) {
        List<WadlOption> options = new ArrayList<WadlOption>();
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
    public static List<WadlOption> loadWsdlOptionsFromFiles(File wsdlBasedir, String includes[],
                                                            String excludes[], Option defaultOptions,
                                                            File defaultOutputDir)
        throws MojoExecutionException {

        if (wsdlBasedir == null) {
            return new ArrayList<WadlOption>();
        }

        if (!wsdlBasedir.exists()) {
            throw new MojoExecutionException(wsdlBasedir + " does not exist");
        }

        List<File> wsdlFiles = getWsdlFiles(wsdlBasedir, includes, excludes);
        List<WadlOption> wsdlOptions = new ArrayList<WadlOption>();
        for (File wsdl : wsdlFiles) {
            WadlOption wsdlOption = generateWsdlOptionFromFile(wsdl, defaultOptions, defaultOutputDir);
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


    protected static WadlOption generateWsdlOptionFromFile(final File wadl, final Option defaultOptions,
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

        final WadlOption wsdlOption = new WadlOption();
        final String wadlName = wadlFileName.substring(0, idx);

        List<File> bindingFiles = FileUtils.getFiles(wadl.getParentFile(), wadlName + WADL_BINDINGS);
        if (bindingFiles != null) {
            for (File binding : bindingFiles) {
                wsdlOption.addBindingFile(binding);
            }
        }
        wsdlOption.setWadl(wadl.toURI().toString());

        if (wsdlOption.getOutputDir() == null) {
            wsdlOption.setOutputDir(defaultOutputDir);
        }

        return wsdlOption;
    }
}
