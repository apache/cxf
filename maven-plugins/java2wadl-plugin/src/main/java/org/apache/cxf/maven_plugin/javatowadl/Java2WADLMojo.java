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

package org.apache.cxf.maven_plugin.javatowadl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.wadl.DocumentationProvider;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

/**
 * @goal java2wadl
 * @description CXF Java To WADL Tool
 * @requiresDependencyResolution test
 * @threadSafe
*/
public class Java2WADLMojo extends AbstractMojo {
    
    public static final String WADL_NS = "http://wadl.dev.java.net/2009/02";
   
    
        
    private List<ClassResourceInfo> classResourceInfos = new ArrayList<ClassResourceInfo>();
    
        
    /**
     * @parameter
     */
    private String outputFile;

   
    
    /**
     * @parameter
     */
    private String address;
    
    /**
     * @parameter
     */
    private String docProvider;
    
    
    /**
     * Attach the generated wadl file to the list of files to be deployed
     * on install. This means the wadl file will be copied to the repository
     * with groupId, artifactId and version of the project and type "wadl".
     *
     * With this option you can use the maven repository as a Service Repository.
     *
     * @parameter default-value="true"
     */
    private Boolean attachWadl;
    
    
    /**
     * @parameter
     */
    private String classifier;

    /**
     * @parameter
     * @required
     */
    private List<String> classResourceNames;
    
    
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
     * @parameter default-value="false"
     */
    private boolean useJson;

    /**
     * @parameter default-value="false"
     */
    private boolean singleResourceMultipleMethods;
   
    /**
     * @parameter default-value="false"
     */
    private boolean useSingleSlashResource;
    
    /**
     * @parameter default-value="false"
     */
    private boolean ignoreForwardSlash;
    
    /**
     * @parameter default-value="false"
     */
    private boolean addResourceAndMethodIds;
    
    /**
     * @parameter default-value="false"
     */
    private boolean linkAnyMediaTypeToXmlSchema;
    
    /**
     * @parameter default-value="false"
     */
    private boolean checkAbsolutePathSlash;
    
    /**
     * @parameter
     */
    private String applicationTitle;
    
    /**
     * @parameter
     */
    private String namespacePrefix;
    
    /**
     * @parameter 
     */
    private String outputFileName;
    
    /**
     * @parameter default-value="wadl"
     */
    private String outputFileExtension;
    
    public void execute() throws MojoExecutionException {
        
        getResourcesList();
        WadlGenerator wadlGenerator = new WadlGenerator(getBus());
        DocumentationProvider documentationProvider = null;
        if (docProvider != null) {
            try {
                documentationProvider = (DocumentationProvider)getClassLoader().loadClass(docProvider).
                    getConstructor(new Class[] {String.class}).
                    newInstance(new Object[] {project.getBuild().getDirectory()});
                wadlGenerator.setDocumentationProvider(documentationProvider);
            } catch (Exception e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        setExtraProperties(wadlGenerator);
        
        StringBuilder sbMain = wadlGenerator.generateWADL(getBaseURI(), classResourceInfos, useJson, null, null);
        getLog().debug("the wadl is =====> \n" + sbMain.toString());
        generateWadl(sbMain.toString());
    }
    
    private void setExtraProperties(WadlGenerator wg) {
        wg.setSingleResourceMultipleMethods(singleResourceMultipleMethods);
        wg.setUseSingleSlashResource(useSingleSlashResource);
        wg.setIgnoreForwardSlash(ignoreForwardSlash);
        wg.setAddResourceAndMethodIds(addResourceAndMethodIds);
        wg.setLinkAnyMediaTypeToXmlSchema(linkAnyMediaTypeToXmlSchema);
        wg.setCheckAbsolutePathSlash(checkAbsolutePathSlash);
         
        if (applicationTitle != null) {
            wg.setApplicationTitle(applicationTitle);
        } 
        if (namespacePrefix != null) {
            wg.setNamespacePrefix(namespacePrefix);
        }
    }
    
    private void generateWadl(String wadl) throws MojoExecutionException {
     
        if (outputFile == null && project != null) {
            // Put the wadl in target/generated/wadl
            
            String name = null;
            if (outputFileName != null) {
                name = outputFileName;
            } else if (applicationTitle != null) {
                name = applicationTitle.replaceAll(" ", "");    
            } else if (classResourceNames.size() == 1) {
                String className = classResourceNames.get(0);
                int i = className.lastIndexOf('.');
                name = className.substring(i + 1);
            } else {
                name = "application";
            }
            outputFile = (project.getBuild().getDirectory() + "/generated/wadl/" + name + "." 
                + outputFileExtension).replace("/", File.separator);
        }
        
        BufferedWriter writer = null;
        try {
            FileUtils.mkDir(new File(outputFile).getParentFile());
            /*File wadlFile = new File(outputFile);
            if (!wadlFile.exists()) {
                wadlFile.createNewFile();
            }*/
            writer = new BufferedWriter(new FileWriter(outputFile));
            writer.write(wadl);

        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        // Attach the generated wadl file to the artifacts that get deployed
        // with the enclosing project
        if (attachWadl && outputFile != null) {
            File wadlFile = new File(outputFile);
            if (wadlFile.exists()) {
                if (classifier != null) {
                    projectHelper.attachArtifact(project, "wadl", classifier, wadlFile);
                } else {
                    projectHelper.attachArtifact(project, "wadl", wadlFile);
                }
                
            }
        }
    }

    private String getBaseURI() {
        if (address != null) {
            return address;
        } else {
            // the consumer may use the original target URI to figure out absolute URI 
            return "/";
        }
    }

    
    
    private ClassLoader getClassLoader() throws MojoExecutionException {
        try {
            List<?> runtimeClasspathElements = project.getRuntimeClasspathElements();
            URL[] runtimeUrls = new URL[runtimeClasspathElements.size()];
            for (int i = 0; i < runtimeClasspathElements.size(); i++) {
                String element = (String)runtimeClasspathElements.get(i);
                runtimeUrls[i] = new File(element).toURI().toURL();
            }
            URLClassLoader newLoader = new URLClassLoader(runtimeUrls, Thread.currentThread()
                .getContextClassLoader());
            return newLoader;
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
    
    private void getResourcesList() throws MojoExecutionException {
        for (String className : classResourceNames) {
            Class<?> beanClass = null;
            try {
                beanClass = getClassLoader().loadClass(className);
            } catch (Exception e) {
                throw new MojoExecutionException(e.getMessage(), e);
            } 
            ClassResourceInfo cri = getCreatedFromModel(beanClass);
            if (cri != null) {
                if (!InjectionUtils.isConcreteClass(cri.getServiceClass())) {
                    cri = new ClassResourceInfo(cri);
                    classResourceInfos.add(cri);
                }
                cri.setResourceClass(beanClass);
                continue;
            }
            
            cri = ResourceUtils.createClassResourceInfo(beanClass, beanClass, true, true,
                                                        getBus());
            if (cri != null) {
                classResourceInfos.add(cri);
            }
        }
    }
    
    private Bus getBus() {
        return BusFactory.getDefaultBus();
    }

    private ClassResourceInfo getCreatedFromModel(Class<?> realClass) {
        
        for (ClassResourceInfo cri : classResourceInfos) {
            if (cri.isCreatedFromModel() 
                && cri.isRoot() && cri.getServiceClass().isAssignableFrom(realClass)) {
                return cri;
            }
        }
        return null;
    }

}

