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
package org.apache.cxf.maven_plugin.corba.maven.plugins;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.tools.corba.WSDLToIDL;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;


/**
 * @goal wsdl2idl
 * @requiresDependencyResolution test
 * @description CXF WSDL To IDL Tool
 */
public class WSDLToIDLPlugin extends AbstractMojo {

    /**
     * @parameter  expression="${project.build.directory}/generated/src/main/java"
     * @required
     */
    File outputDir;
    
    /**
     * @parameter
     */
    WsdltoidlOption wsdltoidlOptions[];
    
    /**
     * @parameter expression="${project}"
     * @required
     */
    MavenProject project;
    
    /**
     * Use the compile classpath rather than the test classpath for execution
     * useful if the test dependencies clash with those of wsdl2java
     * @parameter expression="${cxf.useCompileClasspath}" default-value="false"
     */
    boolean useCompileClasspath;

    public void execute() throws MojoExecutionException {
        outputDir.mkdirs();
        
        if (wsdltoidlOptions == null) {
            throw new MojoExecutionException("Please specify the wsdltoidl options");
        }

        
        List<URL> urlList = new ArrayList<URL>();
        StringBuffer buf = new StringBuffer();

        try {
            urlList.add(outputDir.toURI().toURL());
        } catch (MalformedURLException e) {
            //ignore
        }

        buf.append(outputDir.getAbsolutePath());
        buf.append(File.pathSeparatorChar);


        List artifacts = useCompileClasspath ? project.getCompileArtifacts() : project.getTestArtifacts();
        for (Artifact a : CastUtils.cast(artifacts, Artifact.class)) {
            try {
                if (a.getFile() != null
                    && a.getFile().exists()) {
                    urlList.add(a.getFile().toURI().toURL());
                    buf.append(a.getFile().getAbsolutePath());
                    buf.append(File.pathSeparatorChar);
                    //System.out.println("     " + a.getFile().getAbsolutePath());
                }
            } catch (MalformedURLException e) {
                //ignore
            }
        }
        
        ClassLoader origContext = Thread.currentThread().getContextClassLoader();
        URLClassLoader loader = new URLClassLoader(urlList.toArray(new URL[urlList.size()]),
                                                   origContext);
        String newCp = buf.toString();

        //with some VM's, creating an XML parser (which we will do to parse wsdls)
        //will set some system properties that then interferes with mavens 
        //dependency resolution.  (OSX is the major culprit here)
        //We'll save the props and then set them back later.
        Map<Object, Object> origProps = new HashMap<Object, Object>(System.getProperties());
        
        String cp = System.getProperty("java.class.path");
        
        try {
            Thread.currentThread().setContextClassLoader(loader);
            System.setProperty("java.class.path", newCp);
        
            for (int x = 0; x < wsdltoidlOptions.length; x++) {
                File file = new File(wsdltoidlOptions[x].getWSDL());
                File doneFile = new File(outputDir, "." + file.getName() + ".DONE");
    
                boolean doWork = file.lastModified() > doneFile.lastModified();
                if (!doneFile.exists()) {
                    doWork = true;
                } else if (file.lastModified() > doneFile.lastModified()) {
                    doWork = true;
                }
    
                if (doWork) {
                    List<String> list = new ArrayList<String>();
                    list.add("-d");
                    list.add(outputDir.getAbsolutePath());
                    if (wsdltoidlOptions[x].isCorbaEnabled()) {
                        list.add("-corba");
                    }
                    if (wsdltoidlOptions[x].isIdlEnabled()) {
                        list.add("-idl");
                    }
                    if (wsdltoidlOptions[x].getExtraargs() != null) {
                        list.addAll(wsdltoidlOptions[x].getExtraargs());
                    }
                    list.add(wsdltoidlOptions[x].getWSDL());            
                    try {
                        WSDLToIDL.run((String[])list.toArray(new String[list.size()]));
                        doneFile.delete();
                        doneFile.createNewFile();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        throw new MojoExecutionException(e.getMessage(), e);
                    }
                }
            }
        } finally {
            //cleanup as much as we can.
            Bus bus = BusFactory.getDefaultBus(false);
            if (bus != null) {
                bus.shutdown(true);
            }
            Thread.currentThread().setContextClassLoader(origContext);
            System.setProperty("java.class.path", cp);
            
            Map<Object, Object> newProps = new HashMap<Object, Object>(System.getProperties());
            for (Object o : newProps.keySet()) {
                if (!origProps.containsKey(o)) {
                    System.clearProperty(o.toString());
                }
            }
            System.getProperties().putAll(origProps);
        }

    }

}
