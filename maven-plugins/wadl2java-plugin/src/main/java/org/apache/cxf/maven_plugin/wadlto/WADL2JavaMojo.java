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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.maven_plugin.common.ClassLoaderSwitcher;
import org.apache.maven.plugin.MojoExecutionException;


/**
 * @goal wadl2java
 * @phase generate-sources
 * @description CXF WADL To Java Tool
 * @requiresDependencyResolution test
 * @threadSafe
 */
public class WADL2JavaMojo extends AbstractCodeGeneratorMojo {
    /**
     * @parameter
     */
    WadlOption wadlOptions[];

    /**
     * @parameter expression="${cxf.wadlRoot}" default-value="${basedir}/src/main/resources/wadl"
     */
    File wadlRoot;

    /**
     * @parameter expression="${cxf.testWadlRoot}" default-value="${basedir}/src/test/resources/wadl"
     */
    File testWadlRoot;
    
    private void mergeOptions(List<WadlOption> effectiveOptions) {
        if (wadlOptions == null) {
            return;
        }
        File outputDirFile = testSourceRoot == null ? sourceRoot : testSourceRoot;
        for (WadlOption o : wadlOptions) {
            if (defaultOptions != null) {
                o.merge(defaultOptions);
            }
            if (o.getOutputDir() == null) {
                o.setOutputDir(outputDirFile);
            }

            effectiveOptions.add(o);
        }
    }

    public void execute() throws MojoExecutionException {
        if (includes == null) {
            includes = new String[] {
                "*.wadl"
            };
        }
        File classesDir = new File(classesDirectory);
        classesDir.mkdirs();
        markerDirectory.mkdirs();

        List<WadlOption> effectiveWsdlOptions = createWsdlOptionsFromScansAndExplicitWsdlOptions();

        if (effectiveWsdlOptions.size() == 0) {
            getLog().info("Nothing to generate");
            return;
        }

        ClassLoaderSwitcher classLoaderSwitcher = new ClassLoaderSwitcher(getLog());
        boolean result = true;

        Bus bus = null;
        try {
            Set<URI> cp = classLoaderSwitcher.switchClassLoader(project, useCompileClasspath, classesDir);

            if ("once".equals(fork) || "true".equals(fork)) {
                forkOnce(cp, effectiveWsdlOptions);
            } else {
                for (WadlOption o : effectiveWsdlOptions) {
                    bus = callCodeGenerator(o, bus, cp);
    
                    File dirs[] = o.getDeleteDirs();
                    if (dirs != null) {
                        for (int idx = 0; idx < dirs.length; ++idx) {
                            result = result && deleteDir(dirs[idx]);
                        }
                    }
                }
            }
        } finally {
            // cleanup as much as we can.
            if (bus != null) {
                bus.shutdown(true);
            }
            classLoaderSwitcher.restoreClassLoader();
        }
        if (project != null && sourceRoot != null && sourceRoot.exists()) {
            project.addCompileSourceRoot(sourceRoot.getAbsolutePath());
        }
        if (project != null && testSourceRoot != null && testSourceRoot.exists()) {
            project.addTestCompileSourceRoot(testSourceRoot.getAbsolutePath());
        }

        System.gc();
    }
    
    /**
     * @return effective WsdlOptions
     * @throws MojoExecutionException
     */
    private List<WadlOption> createWsdlOptionsFromScansAndExplicitWsdlOptions() 
        throws MojoExecutionException {
        List<WadlOption> effectiveOptions = new ArrayList<WadlOption>();
        
        mergeOptions(effectiveOptions);
        downloadRemoteDocs(effectiveOptions);
        return effectiveOptions;
    }
    
}
