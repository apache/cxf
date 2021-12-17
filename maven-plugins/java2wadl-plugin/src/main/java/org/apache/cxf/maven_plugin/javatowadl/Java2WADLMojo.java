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
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Path;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ClasspathScanner;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.doc.DocumentationProvider;
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



    private List<ClassResourceInfo> classResourceInfos = new ArrayList<>();


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
     * @parameter
     */
    private String customWadlGenerator;


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
     */
    private List<String> classResourceNames;

    /**
     * @parameter
     */
    private String basePackages;

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
     * @parameter default-value="true"
     */
    private boolean incrementNamespacePrefix;

    /**
     * @parameter default-value="true"
     */
    private boolean singleResourceMultipleMethods;

    /**
     * @parameter default-value="false"
     */
    private boolean useSingleSlashResource;

    /**
     * @parameter default-value="false"
     */
    private boolean includeDefaultWadlSchemaLocation;

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
     * @parameter default-value="false"
     */
    private boolean ignoreOverloadedMethods;

    /**
     * @parameter default-value="true"
     */
    private boolean useJaxbContextForQnames;


    /**
     * @parameter default-value="true"
     */
    private boolean usePathParamsToCompareOperations;

    /**
     * @parameter default-value="true"
     */
    private boolean supportCollections;

    /**
     * @parameter default-value="true"
     */
    private boolean supportJaxbXmlType;

    /**
     * @parameter default-value="true"
     */
    private boolean supportJaxbSubstitutions;

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

    /**
     * @parameter
     */
    private String stylesheetReference;

    private ClassLoader resourceClassLoader;

    public void execute() throws MojoExecutionException {
        System.setProperty("org.apache.cxf.JDKBugHacks.defaultUsesCaches", "true");
        List<Class<?>> resourceClasses = loadResourceClasses();
        initClassResourceInfoList(resourceClasses);
        WadlGenerator wadlGenerator = null;
        if (customWadlGenerator != null) {
            try {
                wadlGenerator = (WadlGenerator)getClassLoader().loadClass(customWadlGenerator).
                    getConstructor(new Class[] {Bus.class}).
                    newInstance(new Object[] {getBus()});
            } catch (Throwable e) {
                getLog().debug("Custom WADLGenerator can not be created, using the default one");
            }
        }
        if (wadlGenerator == null) {
            wadlGenerator = new WadlGenerator(getBus());
        }
        if (docProvider != null) {
            try {
                DocumentationProvider documentationProvider =
                    (DocumentationProvider)getClassLoader().loadClass(docProvider).
                    getConstructor(new Class[] {String.class}).
                    newInstance(new Object[] {project.getBuild().getDirectory()});
                wadlGenerator.setDocumentationProvider(documentationProvider);
            } catch (Exception e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        setExtraProperties(wadlGenerator);

        StringBuilder sbMain = wadlGenerator.generateWADL(getBaseURI(), classResourceInfos, useJson, null, null);
        if (getLog().isDebugEnabled()) {
            getLog().debug("the wadl is =====> \n" + sbMain.toString());
        }
        generateWadl(resourceClasses, sbMain.toString());
    }

    private void setExtraProperties(WadlGenerator wg) {
        wg.setSingleResourceMultipleMethods(singleResourceMultipleMethods);
        wg.setIncrementNamespacePrefix(incrementNamespacePrefix);
        wg.setUseSingleSlashResource(useSingleSlashResource);
        wg.setIncludeDefaultWadlSchemaLocation(includeDefaultWadlSchemaLocation);
        wg.setIgnoreForwardSlash(ignoreForwardSlash);
        wg.setAddResourceAndMethodIds(addResourceAndMethodIds);
        wg.setLinkAnyMediaTypeToXmlSchema(linkAnyMediaTypeToXmlSchema);
        wg.setCheckAbsolutePathSlash(checkAbsolutePathSlash);
        wg.setIgnoreOverloadedMethods(ignoreOverloadedMethods);
        wg.setUseJaxbContextForQnames(useJaxbContextForQnames);
        wg.setUsePathParamsToCompareOperations(usePathParamsToCompareOperations);
        wg.setSupportCollections(supportCollections);
        wg.setSupportJaxbXmlType(supportJaxbXmlType);
        wg.setSupportJaxbSubstitutions(supportJaxbSubstitutions);
        if (applicationTitle != null) {
            wg.setApplicationTitle(applicationTitle);
        }
        if (namespacePrefix != null) {
            wg.setNamespacePrefix(namespacePrefix);
        }
        wg.setStylesheetReference(stylesheetReference);
    }

    private void generateWadl(List<Class<?>> resourceClasses, String wadl) throws MojoExecutionException {

        if (outputFile == null && project != null) {
            // Put the wadl in target/generated/wadl

            final String name;
            if (outputFileName != null) {
                name = outputFileName;
            } else if (resourceClasses.size() == 1) {
                name = resourceClasses.get(0).getSimpleName();
            } else {
                name = "application";
            }
            outputFile = (project.getBuild().getDirectory() + "/generated/wadl/" + name + '.'
                + outputFileExtension).replace('/', File.separatorChar);
        }

        try {
            FileUtils.mkDir(new File(outputFile).getParentFile());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                writer.write(wadl);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
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
        }
        // the consumer may use the original target URI to figure out absolute URI
        return "/";
    }



    private ClassLoader getClassLoader() throws MojoExecutionException {
        if (resourceClassLoader == null) {
            try {
                List<?> runtimeClasspathElements = project.getRuntimeClasspathElements();
                URL[] runtimeUrls = new URL[runtimeClasspathElements.size()];
                for (int i = 0; i < runtimeClasspathElements.size(); i++) {
                    String element = (String)runtimeClasspathElements.get(i);
                    runtimeUrls[i] = new File(element).toURI().toURL();
                }
                resourceClassLoader = new URLClassLoader(runtimeUrls, Thread.currentThread()
                    .getContextClassLoader());
            } catch (Exception e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        return resourceClassLoader;
    }
    private List<Class<?>> loadResourceClasses() throws MojoExecutionException {
        if (classResourceNames == null
            && basePackages == null) {
            throw new MojoExecutionException(
                "either classResourceNames or basePackages should be specified");
        }
        List<Class<?>> resourceClasses = new ArrayList<>(
            classResourceNames == null ? 0 : classResourceNames.size());
        if (classResourceNames != null) {
            for (String className : classResourceNames) {
                try {
                    resourceClasses.add(getClassLoader().loadClass(className));
                } catch (Exception e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
            }
        }
        if (resourceClasses.isEmpty() && basePackages != null) {
            try {
                List<Class<? extends Annotation>> anns = new ArrayList<>();
                anns.add(Path.class);
                final Map< Class< ? extends Annotation >, Collection< Class< ? > > > discoveredClasses =
                    ClasspathScanner.findClasses(ClasspathScanner.parsePackages(basePackages),
                                                 anns,
                                                 getClassLoader());
                if (discoveredClasses.containsKey(Path.class)) {
                    resourceClasses.addAll(discoveredClasses.get(Path.class));
                }
            } catch (Exception ex) {
                // ignore
            }
        }
        return resourceClasses;
    }

    private void initClassResourceInfoList(List<Class<?>> resourceClasses) throws MojoExecutionException {
        for (Class<?> beanClass : resourceClasses) {
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

