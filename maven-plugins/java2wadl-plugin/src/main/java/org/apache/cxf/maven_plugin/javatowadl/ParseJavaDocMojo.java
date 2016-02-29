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
import java.util.List;
import java.util.Locale;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.javadoc.AbstractJavadocMojo;
import org.apache.maven.plugin.javadoc.options.DocletArtifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.reporting.MavenReportException;

import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

/**
 * @goal parsejavadoc
 * @description CXF Java To WADL Tool
 * @requiresDependencyResolution compile
 * @threadSafe
 */
public class ParseJavaDocMojo extends AbstractJavadocMojo {

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
    private ArtifactFactory mavenArtifactFactory;

    /**
     * @component
     */
    private ArtifactResolver artifactResolver;

    /**
     * @component
     */
    private MavenProjectBuilder mavenProjectBuilder;

    /**
     * @component
     */
    private ArtifactMetadataSource artifactMetadataSource;

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
     * The remote repositories where artifacts are located.
     * 
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    private List<ArtifactRepository> remoteRepositories;
    
    
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
        if (skip) {
            getLog().info("Skipping parse javadoc");
            return;
        }

        try {
            Locale locale = Locale.getDefault();
            Field f = AbstractJavadocMojo.class.getDeclaredField("doclet");
            f.setAccessible(true);
            f.set(this, "org.apache.cxf.maven_plugin.javatowadl.DumpJavaDoc");

            f = AbstractJavadocMojo.class.getDeclaredField("encoding");
            f.setAccessible(true);
            f.set(this, encoding);
            
            f = AbstractJavadocMojo.class.getDeclaredField("stylesheet");
            f.setAccessible(true);
            f.set(this, "stylesheet");
            
            f = AbstractJavadocMojo.class.getDeclaredField("javadocOptionsDir");
            f.setAccessible(true);
            f.set(this, javadocOptionsDir);

            f = AbstractJavadocMojo.class.getDeclaredField("docletArtifact");
            f.setAccessible(true);
            DocletArtifact docletArtifact = new DocletArtifact();
            for (Object o : this.mavenProject.getPluginArtifacts()) {
                if (o instanceof Artifact) {
                    Artifact artifact = (Artifact)o;
                    if (artifact.getArtifactId().equals("cxf-java2wadl-plugin")) {
                        docletArtifact.setGroupId(artifact.getGroupId());
                        docletArtifact.setArtifactId(artifact.getArtifactId());
                        docletArtifact.setVersion(artifact.getVersion());
                    }
                }
            }
            f.set(this, docletArtifact);

            f = AbstractJavadocMojo.class.getDeclaredField("factory");
            f.setAccessible(true);
            f.set(this, this.mavenArtifactFactory);

            f = AbstractJavadocMojo.class.getDeclaredField("mavenProjectBuilder");
            f.setAccessible(true);
            f.set(this, this.mavenProjectBuilder);

            f = AbstractJavadocMojo.class.getDeclaredField("resolver");
            f.setAccessible(true);
            f.set(this, this.artifactResolver);

            f = AbstractJavadocMojo.class.getDeclaredField("archiverManager");
            f.setAccessible(true);
            f.set(this, this.archiverManager);

            f = AbstractJavadocMojo.class.getDeclaredField("artifactMetadataSource");
            f.setAccessible(true);
            f.set(this, this.artifactMetadataSource);

            f = AbstractJavadocMojo.class.getDeclaredField("toolchainManager");
            f.setAccessible(true);
            f.set(this, this.toolchainManager);

            f = AbstractJavadocMojo.class.getDeclaredField("localRepository");
            f.setAccessible(true);
            f.set(this, this.localRepository);

            f = AbstractJavadocMojo.class.getDeclaredField("remoteRepositories");
            f.setAccessible(true);
            f.set(this, this.remoteRepositories);

            f = AbstractJavadocMojo.class.getDeclaredField("applyJavadocSecurityFix");
            f.setAccessible(true);
            f.set(this, false);
            
            f = AbstractJavadocMojo.class.getDeclaredField("additionalparam");
            f.setAccessible(true);
            f.set(this, "-dumpJavaDocFile " + this.dumpFileOutputDirectory.getAbsolutePath() 
                      + File.separator + "dumpFile.properties");

            useStandardDocletOptions = false;
            this.project = mavenProject;
            generate(locale);
        } catch (Exception e) {
            failOnError("An error has occurred in parsing javadoc", e);
        }

    }

    private void generate(Locale locale) throws MavenReportException {
        try {
            outputDirectory = getReportOutputDirectory();
            executeReport(locale);
        } catch (MavenReportException e) {
            if (failOnError) {
                throw e;
            }
            getLog().error("Error while creating javadoc report: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            if (failOnError) {
                throw e;
            }
            getLog().error("Error while creating javadoc report: " + e.getMessage(), e);
        }
    }

    private File getReportOutputDirectory() {
        if (dumpFileOutputDirectory == null) {
            return outputDirectory;
        }

        return dumpFileOutputDirectory;
    }

}
