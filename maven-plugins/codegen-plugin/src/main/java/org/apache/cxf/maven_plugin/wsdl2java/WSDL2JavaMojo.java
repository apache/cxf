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

package org.apache.cxf.maven_plugin.wsdl2java;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.maven_plugin.AbstractCodegenMoho;
import org.apache.cxf.maven_plugin.GenericWsdlOption;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.wsdlto.WSDLToJava;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal wsdl2java
 * @phase generate-sources
 * @description CXF WSDL To Java Tool
 * @requiresDependencyResolution test
 * @threadSafe
 */
public class WSDL2JavaMojo extends AbstractCodegenMoho {

    /**
     * @parameter expression="${cxf.testSourceRoot}"
     */
    File testSourceRoot;

    /**
     * Path where the generated sources should be placed
     * 
     * @parameter expression="${cxf.sourceRoot}"
     *            default-value="${project.build.directory}/generated-sources/cxf"
     * @required
     */
    File sourceRoot;

    /**
     * Options that specify WSDLs to process and/or control the processing of wsdls. 
     * If you have enabled wsdl scanning, these elements attach options to particular wsdls.
     * If you have not enabled wsdl scanning, these options call out the wsdls to process. 
     * @parameter
     */
    WsdlOption wsdlOptions[];

    /**
     * Default options to be used when a wsdl has not had it's options explicitly specified.
     * 
     * @parameter
     */
    Option defaultOptions = new Option();

    /**
     * Encoding to use for generated sources
     * 
     * @parameter default-value="${project.build.sourceEncoding}"
     */
    String encoding;

    /**
     * Merge WsdlOptions that point to the same file by adding the extraargs to the first option and deleting
     * the second from the options list
     * 
     * @param options
     */
    protected void mergeOptions(List<GenericWsdlOption> effectiveWsdlOptions) {

        File outputDirFile = getGeneratedTestRoot() == null 
                             ? getGeneratedSourceRoot() : getGeneratedTestRoot();

        List<GenericWsdlOption> newList = new ArrayList<GenericWsdlOption>();

        for (GenericWsdlOption go : effectiveWsdlOptions) {
            WsdlOption o = (WsdlOption) go;
            if (defaultOptions != null) {
                o.merge(defaultOptions);
            }
            /*
             * If not output dir at all, go for tests, and failing that, source.
             */
            if (o.getOutputDir() == null) {
                o.setOutputDir(outputDirFile);
            }

            File file = o.getWsdlFile(project.getBasedir());
            if (file != null && file.exists()) {
                file = file.getAbsoluteFile();
                boolean duplicate = false;
                for (GenericWsdlOption o2w : newList) {
                    WsdlOption o2 = (WsdlOption) o2w;
                    File file2 = o2.getWsdlFile(project.getBasedir());
                    if (file2 != null && file2.exists() && file2.getAbsoluteFile().equals(file)) {
                        o2.getExtraargs().addAll(0, o.getExtraargs());
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    newList.add(o);
                }
            } else {
                newList.add(o);
            }
        }
        effectiveWsdlOptions.clear();
        effectiveWsdlOptions.addAll(newList);
    }

    /**
     * Determine if code should be generated from the given wsdl
     * 
     * @param wsdlOption
     * @param doneFile
     * @param wsdlURI
     * @return
     */
    protected boolean shouldRun(GenericWsdlOption genericWsdlOption, 
                                File doneFile, URI wsdlURI) {
        WsdlOption wsdlOption = (WsdlOption) genericWsdlOption;
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
        } else if (wsdlOption.isDefServiceName()) {
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

    protected List<String> generateCommandLine(GenericWsdlOption wsdlOption)
        throws MojoExecutionException {
        List<String> ret = super.generateCommandLine(wsdlOption);
        if (encoding != null) {
            ret.add(0, "-encoding");
            ret.add(1, encoding);
        }
        return ret;
    }

    @Override
    protected Bus generate(GenericWsdlOption genericWsdlOption, 
                           Bus bus,
                           Set<URI> classPath) throws MojoExecutionException {
        WsdlOption wsdlOption = (WsdlOption) genericWsdlOption;
        File outputDirFile = wsdlOption.getOutputDir();
        outputDirFile.mkdirs();
        URI basedir = project.getBasedir().toURI();
        URI wsdlURI = wsdlOption.getWsdlURI(basedir);
        File doneFile = getDoneFile(basedir, wsdlURI, "java");

        if (!shouldRun(wsdlOption, doneFile, wsdlURI)) {
            return bus;
        }
        doneFile.delete();

        List<String> list = wsdlOption.generateCommandLine(outputDirFile, basedir, wsdlURI, 
                                                           getLog().isDebugEnabled());
        if (encoding != null) {
            list.add(0, "-encoding");
            list.add(1, encoding);
        }
        String[] args = list.toArray(new String[list.size()]);
        getLog().debug("Calling wsdl2java with args: " + Arrays.toString(args));

        if (!"false".equals(fork)) {
            Set<URI> artifactsPath = new LinkedHashSet<URI>();
            for (Artifact a : pluginArtifacts) {
                File file = a.getFile();
                if (file == null) {
                    throw new MojoExecutionException("Unable to find " + file + " for artifact "
                                                     + a.getGroupId() + ":" + a.getArtifactId()
                                                     + ":" + a.getVersion());
                }
                artifactsPath.add(file.toURI());
            }
            addPluginArtifact(artifactsPath);
            artifactsPath.addAll(classPath);

            runForked(artifactsPath, WSDLToJava.class.getName(), args);

        } else {
            if (bus == null) {
                bus = BusFactory.newInstance().createBus();
                BusFactory.setThreadDefaultBus(bus);
            }
            try {
                new WSDLToJava(args).run(new ToolContext());
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

    /**
     * @return effective WsdlOptions
     * @throws MojoExecutionException
     */
    protected List<GenericWsdlOption> createWsdlOptionsFromScansAndExplicitWsdlOptions()
        throws MojoExecutionException {
        List<GenericWsdlOption> effectiveWsdlOptions = new ArrayList<GenericWsdlOption>();

        if (wsdlOptions != null) {
            for (WsdlOption wo : wsdlOptions) {
                effectiveWsdlOptions.add(wo);
            }
        }

        List<GenericWsdlOption> temp;
        if (wsdlRoot != null && wsdlRoot.exists() && !disableDirectoryScan) {
            temp = WsdlOptionLoader.loadWsdlOptionsFromFiles(wsdlRoot, includes, excludes,
                                                             getGeneratedSourceRoot());
            effectiveWsdlOptions.addAll(temp);
        }
        if (testWsdlRoot != null && testWsdlRoot.exists() && !disableDirectoryScan) {
            temp = WsdlOptionLoader.loadWsdlOptionsFromFiles(testWsdlRoot, includes, excludes,
                                                             getGeneratedTestRoot());
            effectiveWsdlOptions.addAll(temp);
        }
        if (!disableDependencyScan) {
            temp = WsdlOptionLoader.loadWsdlOptionsFromDependencies(project, 
                                                                    getGeneratedSourceRoot());
            effectiveWsdlOptions.addAll(temp);
        }
        mergeOptions(effectiveWsdlOptions);
        downloadRemoteWsdls(effectiveWsdlOptions);
        return effectiveWsdlOptions;
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
    protected Class<?> getForkClass() {
        return ForkOnceWSDL2Java.class;
    }

    @Override
    public void execute() throws MojoExecutionException {
        defaultOptions.addDefaultBindingFileIfExists(project.getBasedir());
        super.execute();
    }

    @Override
    protected String getMarkerSuffix() {
        return "java";
    }

}
