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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * Manages switching to the classloader needed for creating the java sources and restoring the old classloader
 * when finished
 */
public class ClassLoaderSwitcher {

    private Log log;
    private String origClassPath;
    private Properties origProps;
    private ClassLoader origContextClassloader;

    public ClassLoaderSwitcher(Log log) {
        this.log = log;
    }

    /**
     * Create and set the classloader that is needed for creating the java sources from wsdl
     *
     * @param project
     * @param useCompileClasspath
     * @param classesDir
     */
    public String switchClassLoader(MavenProject project,
                                    boolean useCompileClasspath,
                                    String classpath,
                                    List<?> classpathElements) {
        List<URL> urlList = new ArrayList<>();
        StringBuilder buf = new StringBuilder();


        try {
            buf.append(classpath);
            buf.append(File.pathSeparatorChar);
            urlList.add(new File(project.getBuild().getOutputDirectory()).toURI().toURL());
        } catch (MalformedURLException e) {
            // ignore
        }
        for (Object classpathElement : classpathElements) {
            buf.append(classpathElement.toString());
            buf.append(File.pathSeparatorChar);
        }

        buf.append(File.pathSeparatorChar);
        @SuppressWarnings("deprecation")
        List<?> artifacts = useCompileClasspath ? project.getCompileArtifacts() : project.getTestArtifacts();
        for (Artifact a : CastUtils.cast(artifacts, Artifact.class)) {
            try {
                if (a.getFile() != null && a.getFile().exists()) {
                    urlList.add(a.getFile().toURI().toURL());
                    buf.append(a.getFile().getAbsolutePath());
                    buf.append(File.pathSeparatorChar);
                    // System.out.println("     " +
                    // a.getFile().getAbsolutePath());
                }
            } catch (MalformedURLException e) {
                // ignore
            }
        }

        origContextClassloader = Thread.currentThread().getContextClassLoader();
        ClassLoader loader = ClassLoaderUtils.getURLClassLoader(urlList, origContextClassloader);
        String newCp = buf.toString();

        log.debug("Classpath: " + urlList.toString());

        origProps = (Properties)System.getProperties().clone();

        origClassPath = System.getProperty("java.class.path");

        Thread.currentThread().setContextClassLoader(loader);
        System.setProperty("java.class.path", newCp);
        return newCp;
    }

    /**
     * Restore the old classloader
     */
    public void restoreClassLoader() {
        if (origContextClassloader != null) {
            Thread.currentThread().setContextClassLoader(origContextClassloader);
            origContextClassloader = null; // don't hold a reference.
        }

        if (origProps != null) {
            System.setProperties(origProps);
        } else if (origClassPath != null) {
            System.setProperty("java.class.path", origClassPath);
        }
    }
}
