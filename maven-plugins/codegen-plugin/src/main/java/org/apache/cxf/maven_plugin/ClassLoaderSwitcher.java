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
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<Object, Object> origProps;
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
    public void switchClassLoader(MavenProject project, boolean useCompileClasspath, File classesDir) {
        List<URL> urlList = new ArrayList<URL>();
        StringBuffer buf = new StringBuffer();

        try {
            urlList.add(classesDir.toURI().toURL());
            if (!useCompileClasspath) {
                urlList.add(new File(project.getBuild().getOutputDirectory()).toURI().toURL());
            }
        } catch (MalformedURLException e) {
            // ignore
        }

        buf.append(classesDir.getAbsolutePath());
        buf.append(File.pathSeparatorChar);
        if (!useCompileClasspath) {
            buf.append(project.getBuild().getOutputDirectory());
            buf.append(File.pathSeparatorChar);
        }
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
        URLClassLoader loader = new URLClassLoader(urlList.toArray(new URL[urlList.size()]),
                                                   origContextClassloader);
        String newCp = buf.toString();

        log.debug("Classpath: " + urlList.toString());

        origProps = new HashMap<Object, Object>(System.getProperties());

        origClassPath = System.getProperty("java.class.path");

        Thread.currentThread().setContextClassLoader(loader);
        System.setProperty("java.class.path", newCp);
    }

    /**
     * Restore the old classloader
     */
    public void restoreClassLoader() {
        Thread.currentThread().setContextClassLoader(origContextClassloader);
        System.setProperty("java.class.path", origClassPath);

        Map<Object, Object> newProps = new HashMap<Object, Object>(System.getProperties());
        for (Object o : newProps.keySet()) {
            if (!origProps.containsKey(o)) {
                System.clearProperty(o.toString());
            }
        }
        System.getProperties().putAll(origProps);
    }
}
