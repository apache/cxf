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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
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

    @Override
    protected Bus generate(WsdlOption wsdlOption, 
                              Bus bus,
                              Set<URI> classPath) throws MojoExecutionException {
        File outputDirFile = wsdlOption.getOutputDir();
        outputDirFile.mkdirs();
        URI basedir = project.getBasedir().toURI();
        URI wsdlURI = wsdlOption.getWsdlURI(basedir);
        File doneFile = getDoneFile(basedir, wsdlURI);

        if (!shouldRun(wsdlOption, doneFile, wsdlURI)) {
            return bus;
        }
        doneFile.delete();

        List<String> list = wsdlOption.generateCommandLine(outputDirFile, basedir, wsdlURI, getLog()
                                                           .isDebugEnabled());
        String[] args = (String[])list.toArray(new String[list.size()]);
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
            
            runForked(artifactsPath, WSDLToJava.class, args);

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

    @Override
    protected File getGeneratedSourceRoot() {
        return sourceRoot;
    }

    @Override
    protected File getGeneratedTestRoot() {
        return testSourceRoot;
    }

}
