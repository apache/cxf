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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.maven_plugin.AbstractCodegenMoho;
import org.apache.cxf.maven_plugin.GenericWsdlOption;
import org.apache.cxf.maven_plugin.WsdlUtilities;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.wsdlto.javascript.WSDLToJavaScript;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * @goal wsdl2js
 * @phase generate-sources
 * @description CXF WSDL To JavaScript Tool
 * @requiresDependencyResolution test
 * @threadSafe
 */
public class WSDL2JavaScriptMojo extends AbstractCodegenMoho {

    /**
     * @parameter expression="${cxf.testJavascriptRoot}"
     */
    File testSourceRoot;

    /**
     * Path where the generated sources should be placed
     * 
     * @parameter expression="${cxf.sourceJavascriptRoot}"
     *            default-value="${project.build.directory}/generated-sources/cxf-js"
     * @required
     */
    File sourceRoot;

    /**
     * Default options to be applied to all of the wsdls.
     * 
     * @parameter
     */
    Option defaultOptions = new Option();
    
    /**
     * Options that specify WSDLs to process and/or control the processing of wsdls. 
     * If you have enabled wsdl scanning, these elements attach options to particular wsdls.
     * If you have not enabled wsdl scanning, these options call out the wsdls to process. 
     * @parameter
     */
    WsdlOption wsdlOptions[];

    @Override
    protected Bus generate(GenericWsdlOption genericWsdlOption, 
                           Bus bus, Set<URI> classPath)
        throws MojoExecutionException {

        WsdlOption wsdlOption = (WsdlOption)genericWsdlOption;
        File outputDirFile = wsdlOption.getOutputDir();
        outputDirFile.mkdirs();
        URI basedir = project.getBasedir().toURI();
        URI wsdlURI;
        try {
            wsdlURI = new URI(wsdlOption.getUri());
        } catch (URISyntaxException e) {
            throw new MojoExecutionException("Failed to get URI for wsdl " + wsdlOption.getUri(), e);
        }
        File doneFile = getDoneFile(basedir, wsdlURI, "js");

        if (!shouldRun(wsdlOption, doneFile, wsdlURI)) {
            return bus;
        }
        doneFile.delete();

        List<String> list = wsdlOption.generateCommandLine(outputDirFile, basedir, wsdlURI, getLog()
            .isDebugEnabled());
        String[] args = (String[])list.toArray(new String[list.size()]);
        getLog().debug("Calling wsdl2js with args: " + Arrays.toString(args));

        if (!"false".equals(fork)) {
            Set<URI> artifactsPath = new LinkedHashSet<URI>();
            for (Artifact a : pluginArtifacts) {
                File file = a.getFile();
                if (file == null) {
                    throw new MojoExecutionException("Unable to find " + file + " for artifact "
                                                     + a.getGroupId() + ":" + a.getArtifactId() + ":"
                                                     + a.getVersion());
                }
                artifactsPath.add(file.toURI());
            }
            addPluginArtifact(artifactsPath);
            artifactsPath.addAll(classPath);

            runForked(artifactsPath, WSDLToJavaScript.class.getName(), args);

        } else {
            if (bus == null) {
                bus = BusFactory.newInstance().createBus();
                BusFactory.setThreadDefaultBus(bus);
            }
            try {
                new WSDLToJavaScript(args).run(new ToolContext());
            } catch (Throwable e) {
                getLog().debug(e);
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        try {
            doneFile.createNewFile();
        } catch (Throwable e) {
            getLog().warn("Could not create marker file " + doneFile.getAbsolutePath());
            getLog().debug(e);
            throw new MojoExecutionException("Failed to create marker file " + doneFile.getAbsolutePath());
        }
        if (project != null && getGeneratedSourceRoot() != null && getGeneratedSourceRoot().exists()) {
            project.addCompileSourceRoot(getGeneratedSourceRoot().getAbsolutePath());
        }
        if (project != null && getGeneratedTestRoot() != null && getGeneratedTestRoot().exists()) {
            project.addTestCompileSourceRoot(getGeneratedTestRoot().getAbsolutePath());
        }
        return bus;
    }

    @Override
    protected File getGeneratedSourceRoot() {
        return sourceRoot;
    }

    @Override
    protected File getGeneratedTestRoot() {
        return testSourceRoot;
    }

    @Override
    protected boolean shouldRun(GenericWsdlOption genericWsdlOption, File doneFile,
                                URI wsdlURI) {
        WsdlOption wsdlOption = (WsdlOption)genericWsdlOption;
        long timestamp = 0;
        if ("file".equals(wsdlURI.getScheme())) {
            timestamp = new File(wsdlURI).lastModified();
        } else {
            try {
                timestamp = wsdlURI.toURL().openConnection().getDate();
            } catch (Exception e) {
                // ignore
            }
        }
        boolean doWork = false;
        if (!doneFile.exists()) {
            doWork = true;
        } else if (timestamp > doneFile.lastModified()) {
            doWork = true;
        } else {
            File files[] = wsdlOption.getDependencies();
            if (files != null) {
                for (int z = 0; z < files.length; ++z) {
                    if (files[z].lastModified() > doneFile.lastModified()) {
                        doWork = true;
                    }
                }
            }
        }
        return doWork;
    }

    protected void mergeOptions(List<GenericWsdlOption> effectiveWsdlOptions) {
        File outputDirFile = getGeneratedTestRoot() == null 
            ? getGeneratedSourceRoot() : getGeneratedTestRoot();
        for (GenericWsdlOption wo : effectiveWsdlOptions) {
            WsdlOption option = (WsdlOption)wo;
            option.merge(defaultOptions);
            if (option.getOutput() == null) {
                option.setOutput(outputDirFile);
            }
        }
    }

    @Override
    protected List<GenericWsdlOption> createWsdlOptionsFromScansAndExplicitWsdlOptions()
        throws MojoExecutionException {
        List<GenericWsdlOption> effectiveWsdlOptions = new ArrayList<GenericWsdlOption>();
        List<GenericWsdlOption> temp;
        
        if (wsdlOptions != null) {
            for (WsdlOption wo : wsdlOptions) {
                effectiveWsdlOptions.add(wo);
            }
        }
        
        if (wsdlRoot != null && wsdlRoot.exists() && !disableDirectoryScan) {
            temp = loadWsdlOptionsFromFiles(wsdlRoot, getGeneratedSourceRoot());
            effectiveWsdlOptions.addAll(temp);
        }
        if (testWsdlRoot != null && testWsdlRoot.exists() && !disableDirectoryScan) {
            temp = loadWsdlOptionsFromFiles(testWsdlRoot, getGeneratedTestRoot());
            effectiveWsdlOptions.addAll(temp);
        }
        if (!disableDependencyScan) {
            temp = loadWsdlOptionsFromDependencies(project, defaultOptions, getGeneratedSourceRoot());
            effectiveWsdlOptions.addAll(temp);
        }
        mergeOptions(effectiveWsdlOptions);
        downloadRemoteWsdls(effectiveWsdlOptions);
        return effectiveWsdlOptions;
    }

    private List<GenericWsdlOption> loadWsdlOptionsFromFiles(File wsdlBasedir, File defaultOutputDir)
        throws MojoExecutionException {

        if (wsdlBasedir == null) {
            return Collections.emptyList();
        }

        if (!wsdlBasedir.exists()) {
            throw new MojoExecutionException(wsdlBasedir + " does not exist");
        }

        List<File> wsdlFiles = WsdlUtilities.getWsdlFiles(wsdlBasedir, includes, excludes);
        List<GenericWsdlOption> options = new ArrayList<GenericWsdlOption>();
        for (File wsdl : wsdlFiles) {
            WsdlOption wsdlOption = new WsdlOption();
            wsdlOption.setOutputDir(defaultOutputDir);
            wsdlOption.setUri(wsdl.toURI().toString());
            options.add(wsdlOption);
        }
        return options;
    }

    public static List<GenericWsdlOption> 
    loadWsdlOptionsFromDependencies(MavenProject project,
                                    Option defaultOptions,
                                    File outputDir) {
        List<GenericWsdlOption> options 
            = new ArrayList<GenericWsdlOption>();
        Set<Artifact> dependencies = CastUtils.cast(project.getDependencyArtifacts());
        for (Artifact artifact : dependencies) {
            WsdlOption option = new WsdlOption();
            if (WsdlUtilities.fillWsdlOptionFromArtifact(option, artifact, outputDir)) {
                if (defaultOptions != null) {
                    option.merge(defaultOptions);
                }
                options.add(option);
            }
        }
        return options;
    }

    @Override
    protected Class<?> getForkClass() {
        return ForkOnceWSDL2Javascript.class;
    }

    @Override
    protected String getMarkerSuffix() {
        return "js";
    }

}
