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

package org.apache.cxf.maven_plugin.eclipse;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.tools.common.VelocityGenerator;
import org.apache.cxf.tools.util.FileWriterUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * @goal eclipseplugin
 * @description CXF eclipse plugin generator
 */
public class EclipsePluginMojo extends AbstractMojo {
    private static final String LIB_PATH = "lib";
    private static final String ECLIPSE_VERSION = "3.2";
    
    
    /**
     * @parameter expression="${project}"
     * @required
     */
    MavenProject project;

    /**
     * The set of dependencies required by the project 
     * @parameter default-value="${project.artifacts}"
     * @required
     * @readonly
     */
    Set dependencies;
    
    /**
     * @parameter  expression="${project.build.directory}";
     * @required
     */
    File targetDir;

    private String getTemplateFile(String version) {
        return "/org/apache/cxf/maven_plugin/eclipse/" + version + "/MANIFEST.vm";
    }

    private List<File> listJars() throws Exception {
        List<File> jars = new ArrayList<File>();
        if (dependencies != null && !dependencies.isEmpty()) {            
            for (Iterator it = dependencies.iterator(); it.hasNext();) {
                Artifact artifact = (Artifact)it.next();
                File oldJar = artifact.getFile();
                jars.add(oldJar);
            }            
        }
        return jars;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        
        try {
            generatePluginXML(listJars());
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }


    private String getVersion() {
        return StringUtils.formatVersionNumber(project.getVersion());
    }

    private List<String> getExportedPackages(List<File> jars) throws Exception {
        List<String> packages = new ArrayList<String>();
        for (File jarFile : jars) {
            packages.addAll(ReflectionUtil.getPackagesFromJar(jarFile));
        }
        return packages;
    }

    private void generatePluginXML(List<File> jars) throws Exception {
        VelocityGenerator velocity = new VelocityGenerator(false);

        String templateFile = getTemplateFile(ECLIPSE_VERSION);

        velocity.setAttributes("ECLIPSE_VERSION", ECLIPSE_VERSION);
        velocity.setAttributes("PLUGIN_VERSION", getVersion());
        velocity.setAttributes("GROUP_ID", project.getGroupId());
        velocity.setAttributes("libPath", LIB_PATH);
        velocity.setAttributes("jars", jars);
        
        velocity.setAttributes("exportedPackages", getExportedPackages(jars));
        File outputFile = new File(targetDir, "MANIFEST.MF");

        velocity.doWrite(templateFile, FileWriterUtil.getWriter(outputFile));
    }
}
