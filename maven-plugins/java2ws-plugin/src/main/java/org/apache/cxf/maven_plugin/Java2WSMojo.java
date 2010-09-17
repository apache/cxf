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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.tools.common.CommandInterfaceUtils;
import org.apache.cxf.tools.java2ws.JavaToWS;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

/**
 * @goal java2ws
 * @description CXF Java To Webservice Tool
 * @requiresDependencyResolution test
*/
public class Java2WSMojo extends AbstractMojo {
    /**
     * @parameter 
     * @required
     */
    private String className;

    /**
     * @parameter  expression="${project.build.outputDirectory}"
     * @required
     */
    private String classpath;

    /**
     * @parameter
     */
    private String outputFile;
    
    /**
     * @parameter
     */
    private Boolean soap12;

    /**
     * @parameter
     */
    private String targetNamespace;

    /**
     * @parameter
     */
    private String serviceName;

    /**
     * @parameter
     */
    private Boolean verbose;

    /**
     * @parameter
     */
    private Boolean quiet;

    /**
     * @parameter  expression="${project.compileClasspathElements}"
     * @required
     */
    private List classpathElements;

    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * Maven ProjectHelper.
     * 
     * @component
     * @readonly
     */
    private MavenProjectHelper projectHelper;    

    /**
     * @parameter
     */
    private String argline;
    
    /**
     * @parameter
     */
    private String frontend;
    
    /**
     * @parameter
     */
    private String databinding;
    /**
     * @parameter default-value="false"
     */
    private Boolean genWsdl;
    /**
     * @parameter default-value="false"
     */
    private Boolean genServer;
    /**
     * @parameter default-value="false"
     */
    private Boolean genClient;
    /**
     * @parameter default-value="false"
     */
    private Boolean genWrapperbean;
    
    /**
     * Attach the generated wsdl file to the list of files to be deployed
     * on install. This means the wsdl file will be copied to the repository
     * with groupId, artifactId and version of the project and type "wsdl".
     * 
     * With this option you can use the maven repository as a Service Repository.
     * 
     * @parameter default-value="true"
     */
    private Boolean attachWsdl;
    
    public void execute() throws MojoExecutionException {
        ClassLoaderSwitcher classLoaderSwitcher = new ClassLoaderSwitcher(getLog());
        

        try {
            String cp = classLoaderSwitcher.switchClassLoader(project, false, 
                                                              classpath, classpathElements);
            processJavaClass(cp);
        } finally {
            classLoaderSwitcher.restoreClassLoader();
        }

        System.gc();
    }

    private void processJavaClass(String cp) throws MojoExecutionException {
        List<String> args = new ArrayList<String>();

        // outputfile arg
        if (outputFile == null && project != null) {
            // Put the wsdl in target/generated/wsdl
            int i = className.lastIndexOf('.');
            // Prone to OoBE, but then it's wrong anyway
            String name = className.substring(i + 1); 
            outputFile = (project.getBuild().getDirectory() + "/generated/wsdl/" + name + ".wsdl")
                .replace("/", File.separator);
        }
        if (outputFile != null) {
            // JavaToWSDL freaks out if the directory of the outputfile doesn't exist, so lets
            // create it since there's no easy way for the user to create it beforehand in maven
            FileUtils.mkDir(new File(outputFile).getParentFile());
            args.add("-o");
            args.add(outputFile);

            /*
              Contributor's comment:
              Sometimes JavaToWSDL creates Java code for the wrappers.  I don't *think* this is
              needed by the end user.
            */
            
            // Commiter's comment:
            // Yes, it's required, it's defined in the JAXWS spec.

            if (project != null) {
                project.addCompileSourceRoot(new File(outputFile).getParentFile().getAbsolutePath());
            }
        }
        
        if (frontend != null) {
            args.add("-frontend");
            args.add(frontend);
        }
        
        if (databinding != null) {
            args.add("-databinding");
            args.add(databinding);
        }
        
        if (genWrapperbean) {
            args.add("-wrapperbean");
        }
        
        if (genWsdl) {
            args.add("-wsdl");
        }
        
        if (genServer) {
            args.add("-server");
        }
        
        if (genClient) {
            args.add("-client");
        }
        
        // classpath arg
        args.add("-cp");
        args.add(cp);

        // soap12 arg
        if (soap12 != null && soap12.booleanValue()) {
            args.add("-soap12");
        }

        // target namespace arg
        if (targetNamespace != null) {
            args.add("-t");
            args.add(targetNamespace);
        }

        // servicename arg
        if (serviceName != null) {
            args.add("-servicename");
            args.add(serviceName);
        }

        // verbose arg
        if (verbose != null && verbose.booleanValue()) {
            args.add("-verbose");
        }

        // quiet arg
        if (quiet != null && quiet.booleanValue()) {
            args.add("-quiet");
        }

        if (argline != null) {
            StringTokenizer stoken = new StringTokenizer(argline, " ");
            while (stoken.hasMoreTokens()) {
                args.add(stoken.nextToken());
            }
        }

        // classname arg
        args.add(className);

        try {
            CommandInterfaceUtils.commandCommonMain();
            JavaToWS j2w = new JavaToWS(args.toArray(new String[args.size()]));
            j2w.run();
        } catch (Throwable e) {
            getLog().debug(e);
            throw new MojoExecutionException(e.getMessage(), e);
        }

        // Attach the generated wsdl file to the artifacts that get deployed
        // with the enclosing project
        if (attachWsdl && outputFile != null) {
            File wsdlFile = new File(outputFile);
            projectHelper.attachArtifact(project, "wsdl", wsdlFile);
        }
    }
       
}