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

package org.apache.cxf.maven_plugin.java2swagger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.apache.cxf.helpers.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.reflections.Reflections;

import io.swagger.annotations.Api;
import io.swagger.jaxrs.Reader;
import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.models.License;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.util.Yaml;



/**
 * @goal java2swagger
 * @description CXF Java To swagger payload Tool
 * @requiresDependencyResolution test
 * @threadSafe
 */
public class Java2SwaggerMojo extends AbstractMojo {

    /**
    * @parameter
    * @required
    */
    private List<String> resourcePackages;

    /**
     * @parameter default-value="${project.version}"
     */
    private String version;


    /**
     * @parameter default-value="/api"
     */
    private String basePath;

    /**
     * @parameter default-value="${project.name}"
     */
    private String title;


    /**
     * @parameter default-value="${project.description}"
     */
    private String description;


    /**
     * @parameter
     */
    private String contact;


    /**
     * @parameter default-value="Apache 2.0 License"
     */
    private String license;

    /**
     * @parameter default-value="http://www.apache.org/licenses/LICENSE-2.0.html"
     */
    private String licenseUrl;

    /**
     * @parameter
     */
    private String host;

    /**
     * @parameter
     */
    private List<String> schemes;


    /**
     * @parameter default-value="json"
     */
    private String payload;


    /**
     * @parameter
     */
    private String outputFile;


    /**
     * Attach the generated swagger file to the list of files to be deployed
     * on install. This means the swagger file will be copied to the repository
     * with groupId, artifactId and version of the project and type "json" or "yaml".
     * <p/>
     * With this option you can use the maven repository as a Service Repository.
     *
     * @parameter default-value="false"
     */
    private Boolean attachSwagger;


    /**
     * @parameter
     */
    private String classifier;


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
    private String outputFileName;


    private ClassLoader resourceClassLoader;

    private Swagger swagger;

    private ObjectMapper mapper = new ObjectMapper();

    private Set<Class<?>> resourceClasses;


    public void execute() throws MojoExecutionException {
        try {
            configureSwagger();
            loadSwaggerAnnotation();
            generateSwaggerPayLoad();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private void generateSwaggerPayLoad() throws MojoExecutionException {

        if (outputFile == null && project != null) {
            // Put the json in target/generated/json
            // put the yaml in target/generated/yaml

            String name = null;
            if (outputFileName != null) {
                name = outputFileName;
            } else if (resourceClasses.size() == 1) {
                name = resourceClasses.iterator().next().getSimpleName();
            } else {
                name = "swagger";
            }
            outputFile = (project.getBuild().getDirectory() + "/generated/" + payload.toLowerCase() + "/" + name + "."
                    + payload.toLowerCase()).replace("/", File.separator);
        }

        FileUtils.mkDir(new File(outputFile).getParentFile());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            if ("json".equals(this.payload)) {
                ObjectWriter jsonWriter = mapper.writer(new DefaultPrettyPrinter());
                writer.write(jsonWriter.writeValueAsString(swagger));
            } else if ("yaml".equals(this.payload)) {
                writer.write(Yaml.pretty().writeValueAsString(swagger));
            }

        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        // Attach the generated swagger file to the artifacts that get deployed
        // with the enclosing project
        if (attachSwagger && outputFile != null) {
            File jsonFile = new File(outputFile);
            if (jsonFile.exists()) {
                if (classifier != null) {
                    projectHelper.attachArtifact(project, payload.toLowerCase(), classifier, jsonFile);
                } else {
                    projectHelper.attachArtifact(project, payload.toLowerCase(), jsonFile);
                }

            }
        }
    }


    private ClassLoader getClassLoader() throws MojoExecutionException {
        if (resourceClassLoader == null) {
            try {
                List<?> runtimeClasspathElements = project.getRuntimeClasspathElements();
                URL[] runtimeUrls = new URL[runtimeClasspathElements.size()];
                for (int i = 0; i < runtimeClasspathElements.size(); i++) {
                    String element = (String) runtimeClasspathElements.get(i);
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

    private Set<Class<?>> loadResourceClasses(Class<? extends Annotation> clazz) throws MojoExecutionException {
        resourceClasses = new LinkedHashSet<>(this.resourcePackages.size());
        Thread.currentThread().setContextClassLoader(getClassLoader());
        for (String resourcePackage : resourcePackages) {
            Set<Class<?>> c = new Reflections(resourcePackage).getTypesAnnotatedWith(clazz, true);
            resourceClasses.addAll(c);
            Set<Class<?>> inherited = new Reflections(resourcePackage).getTypesAnnotatedWith(clazz);
            resourceClasses.addAll(inherited);
        }

        return resourceClasses;
    }



    private void loadSwaggerAnnotation() throws MojoExecutionException {
        Reader reader = new Reader(swagger);
        swagger = reader.read(loadResourceClasses(Api.class));
    }

    private void configureSwagger() {
        swagger = new Swagger();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        Info info = new Info();
        Contact swaggerContact = new Contact();
        License swaggerLicense = new License();
        swaggerLicense.name(this.license)
            .url(this.licenseUrl);
        swaggerContact.name(this.contact);
        info.version(this.version)
            .description(this.description)
            .contact(swaggerContact)
            .license(swaggerLicense)
            .title(this.title);
        swagger.setInfo(info);
        if (this.schemes != null) {
            for (String scheme : this.schemes) {
                swagger.scheme(Scheme.forValue(scheme));
            }
        }
        swagger.setHost(this.host);
        swagger.setBasePath(this.basePath);
    }
}
