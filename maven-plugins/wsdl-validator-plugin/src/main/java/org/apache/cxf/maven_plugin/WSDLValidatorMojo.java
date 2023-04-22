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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.tools.common.toolspec.ToolSpec;
import org.apache.cxf.tools.validator.WSDLValidator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "wsdlvalidator", threadSafe = true)
public class WSDLValidatorMojo extends AbstractMojo {

    @Parameter
    private Boolean verbose;

    @Parameter
    private Boolean quiet;

    @Parameter(property = "cxf.wsdlRoot", defaultValue = "${basedir}/src/main/resources/wsdl")
    private File wsdlRoot;

    @Parameter(property = "cxf.testWsdlRoot", defaultValue = "${basedir}/src/test/resources/wsdl")
    private File testWsdlRoot;

    /**
     * Directory in which the "DONE" markers are saved that
     */
    @Parameter(property = "cxf.markerDirectory", defaultValue = "${project.build.directory}/cxf-wsdl-validator-markers")
    private File markerDirectory;

    /**
     * A list of wsdl files to include. Can contain ant-style wildcards and double wildcards. Defaults to *.wsdl
     */
    @Parameter
    private String[] includes = {
        "*.wsdl"
    };

    /**
     * A list of wsdl files to exclude. Can contain ant-style wildcards and double wildcards.
     */
    @Parameter
    private String[] excludes;

    private static String getIncludeExcludeString(String[] arr) {
        if (arr == null || arr.length == 0) {
            return "";
        }
        return String.join(",", arr);
    }

    private List<File> getWsdlFiles(File dir) throws MojoExecutionException {

        List<String> exList = new ArrayList<>();
        if (excludes != null) {
            Collections.addAll(exList, excludes);
        }
        Collections.addAll(exList, org.codehaus.plexus.util.FileUtils.getDefaultExcludes());

        String inc = getIncludeExcludeString(includes);
        String ex = getIncludeExcludeString(exList.toArray(new String[0]));

        try {
            return org.codehaus.plexus.util.FileUtils.getFiles(dir, inc, ex);
        } catch (IOException exc) {
            throw new MojoExecutionException(exc.getMessage(), exc);
        }
    }

    private void processWsdl(File file) throws MojoExecutionException {

        // If URL to WSDL, replace ? and & since they're invalid chars for file names
        File doneFile =
            new File(markerDirectory, '.' + file.getName().replace('?', '_').replace('&', '_') + ".DONE");
        boolean doWork = false;
        if (!doneFile.exists()) {
            doWork = true;
        } else if (file.lastModified() > doneFile.lastModified()) {
            doWork = true;
        }

        if (doWork) {
            doneFile.delete();

            List<String> list = new ArrayList<>();

            // verbose arg
            if (verbose != null && verbose) {
                list.add("-verbose");
            }

            // quiet arg
            if (quiet != null && quiet) {
                list.add("-quiet");
            }

            getLog().debug("Calling wsdlvalidator with args: " + list);
            final boolean ok;
            try (InputStream toolspecStream = WSDLValidator.class .getResourceAsStream("wsdlvalidator.xml")) {
                list.add(file.getCanonicalPath());

                WSDLValidator validator = new WSDLValidator(new ToolSpec(toolspecStream, false));
                validator.setArguments(list.toArray(new String[0]));
                ok = validator.executeForMaven();

                doneFile.createNewFile();
            } catch (Throwable e) {
                throw new MojoExecutionException(file.getName() + ": "
                                                 + e.getMessage(), e);
            }
            if (!ok) {
                throw new MojoExecutionException("WSDL failed validation: " + file.getName());
            }
        }
    }

    public void execute() throws MojoExecutionException {
        System.setProperty("org.apache.cxf.JDKBugHacks.defaultUsesCaches", "true");

        markerDirectory.mkdirs();

        List<File> wsdls = new ArrayList<>();
        if (wsdlRoot != null && wsdlRoot.exists()) {
            wsdls.addAll(getWsdlFiles(wsdlRoot));
        }
        if (testWsdlRoot != null && testWsdlRoot.exists()) {
            wsdls.addAll(getWsdlFiles(testWsdlRoot));
        }

        for (File wsdl : wsdls) {
            processWsdl(wsdl);
        }
    }
}
