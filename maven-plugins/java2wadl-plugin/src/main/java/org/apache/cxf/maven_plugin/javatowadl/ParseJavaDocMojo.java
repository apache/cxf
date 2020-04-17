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

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.javadoc.AbstractJavadocMojo;
import org.apache.maven.plugins.javadoc.JavadocReport;
import org.apache.maven.plugins.javadoc.options.DocletArtifact;
import org.apache.maven.plugins.javadoc.resolver.ResourceResolver;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

/**
 * @goal parsejavadoc
 * @description CXF Java To WADL Tool
 * @requiresDependencyResolution compile
 * @threadSafe
 */
public class ParseJavaDocMojo extends AbstractMojo {
    
    /**
     * The Maven Session Object
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session; 

    /**
     * The source encoding.
     *
     * @parameter defaultValue = "${project.build.sourceEncoding}"
     */
    private String encoding;

    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject mavenProject;

    /**
     * @component
     */
    private ArchiverManager archiverManager;
    
    /**
     * @component
     */
    private ResourceResolver resourceResolver;
    
    /**
     * @component
     */
    private DependencyResolver dependencyResolver;

    
    /**
     * @component
     */
    private ArtifactResolver artifactResolver;
    
    

    /**
     * @component
     */
    private org.apache.maven.project.ProjectBuilder mavenProjectBuilder;

    
    /**
     * @component
     */
    private ToolchainManager toolchainManager;

    /**
     * @parameter default-value = "${project.reporting.outputDirectory}/apidocs"
     * @required
     */
    private File dumpFileOutputDirectory;

    /**
     * The local maven repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    
    /**
     * Directory into which assembled {@link JavadocOptions} instances will be written before they
     * are added to javadoc resources bundles.
     * @parameter expression="${project.build.directory}/javadoc-bundle-options"
     * @required
     * @readonly
     */
    private File javadocOptionsDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        AbstractJavadocMojo mojo = new JavadocReport();
        Locale locale = Locale.getDefault();
        try {
            Field f = AbstractJavadocMojo.class.getDeclaredField("doclet");
            f.setAccessible(true);
            f.set(mojo, "org.apache.cxf.maven_plugin.javatowadl.DumpJavaDoc");

            f = AbstractJavadocMojo.class.getDeclaredField("encoding");
            f.setAccessible(true);
            f.set(mojo, encoding);

            f = AbstractJavadocMojo.class.getDeclaredField("stylesheet");
            f.setAccessible(true);
            f.set(mojo, "stylesheet");

            f = AbstractJavadocMojo.class.getDeclaredField("javadocOptionsDir");
            f.setAccessible(true);
            f.set(mojo, javadocOptionsDir);

            f = AbstractJavadocMojo.class.getDeclaredField("docletArtifact");
            f.setAccessible(true);
            DocletArtifact docletArtifact = new DocletArtifact();
            for (Object o : this.mavenProject.getPluginArtifacts()) {
                if (o instanceof Artifact) {
                    Artifact artifact = (Artifact)o;
                    if ("cxf-java2wadl-plugin".equals(artifact.getArtifactId())) {
                        docletArtifact.setGroupId(artifact.getGroupId());
                        docletArtifact.setArtifactId(artifact.getArtifactId());
                        docletArtifact.setVersion(artifact.getVersion());
                    }
                }
            }
            f.set(mojo, docletArtifact);

            
            f = AbstractJavadocMojo.class.getDeclaredField("mavenProjectBuilder");
            f.setAccessible(true);
            f.set(mojo, this.mavenProjectBuilder);
            
            f = AbstractJavadocMojo.class.getDeclaredField("resourceResolver");
            f.setAccessible(true);
            f.set(mojo, this.resourceResolver);
            
            f = AbstractJavadocMojo.class.getDeclaredField("session");
            System.out.println("========>" + session.getProjects());
            f.setAccessible(true);
            f.set(mojo, this.session);
            
            f = AbstractJavadocMojo.class.getDeclaredField("dependencyResolver");
            f.setAccessible(true);
            f.set(mojo, this.dependencyResolver);

            f = AbstractJavadocMojo.class.getDeclaredField("artifactResolver");
            f.setAccessible(true);
            f.set(mojo, this.artifactResolver);

            f = AbstractJavadocMojo.class.getDeclaredField("archiverManager");
            f.setAccessible(true);
            f.set(mojo, this.archiverManager);

            f = AbstractJavadocMojo.class.getDeclaredField("toolchainManager");
            f.setAccessible(true);
            f.set(mojo, this.toolchainManager);

            f = AbstractJavadocMojo.class.getDeclaredField("localRepository");
            f.setAccessible(true);
            f.set(mojo, this.localRepository);

            f = AbstractJavadocMojo.class.getDeclaredField("applyJavadocSecurityFix");
            f.setAccessible(true);
            f.set(mojo, false);
            
            f = AbstractJavadocMojo.class.getDeclaredField("additionalOptions");
            f.setAccessible(true);
            f.set(mojo, new String[] {"-dumpJavaDocFile " + this.dumpFileOutputDirectory.getAbsolutePath()
                + File.separator + "dumpFile.properties"});

            f = AbstractJavadocMojo.class.getDeclaredField("useStandardDocletOptions");
            f.setAccessible(true);
            f.set(mojo, false);

            f = AbstractJavadocMojo.class.getDeclaredField("project");
            f.setAccessible(true);
            f.set(mojo, mavenProject);

            if (dumpFileOutputDirectory != null) {
                f = AbstractJavadocMojo.class.getDeclaredField("outputDirectory");
                f.setAccessible(true);
                f.set(mojo, dumpFileOutputDirectory);
            }

            Method m = AbstractJavadocMojo.class.getDeclaredMethod("executeReport", Locale.class);
            m.setAccessible(true);
            m.invoke(mojo, locale);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate javadoc", e);
        }
    }
}
