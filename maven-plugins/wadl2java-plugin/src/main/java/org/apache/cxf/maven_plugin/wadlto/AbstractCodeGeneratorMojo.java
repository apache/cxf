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

package org.apache.cxf.maven_plugin.wadlto;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.SystemUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.CollectionUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.maven_plugin.common.DocumentArtifact;
import org.apache.cxf.maven_plugin.common.ForkOnceCodeGenerator;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.wadlto.WADLToJava;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.Manifest.Attribute;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

public abstract class AbstractCodeGeneratorMojo extends AbstractMojo {

    /**
     * Source Root
     */
    @Parameter(property = "cxf.testSourceRoot")
    File testSourceRoot;

    /**
     * Path where the generated sources should be placed
     */
    @Parameter(required = true,
            property = "cxf.sourceRoot",
            defaultValue = "${project.build.directory}/generated-sources/cxf")
    File sourceRoot;

    @Parameter(required = true, property = "project.build.outputDirectory")
    String classesDirectory;

    @Parameter(required = true, property = "project")
    MavenProject project;

    /**
     * Default options to be used when a wadl has not had it's options explicitly specified.
     */
    @Parameter
    Option defaultOptions = new Option();

    /**
     * Directory in which the "DONE" markers are saved that
     */
    @Parameter(property = "cxf.markerDirectory", defaultValue = "${project.build.directory}/cxf-codegen-plugin-markers")
    File markerDirectory;

    /**
     * Use the compile classpath rather than the test classpath for execution useful if the test dependencies
     * clash with those of wadl2java
     */
    @Parameter(property = "cxf.useCompileClasspath", defaultValue = "false")
    boolean useCompileClasspath;


    /**
     * Disables the scanning of the wadlRoot/testWadlRoot directories configured above.
     * By default, we scan for *.wadl (see include/exclude params as well) in the wadlRoot
     * directories and run wadl2java on all the wadl's we find.    This disables that scan
     * and requires an explicit wadlOption to be set for each wadl that needs to be processed.
     */
    @Parameter(property = "cxf.disableDirectoryScan", defaultValue = "false")
    boolean disableDirectoryScan;

    /**
     * By default all maven dependencies of type "wadl" are added to the effective wadlOptions. Setting this
     * parameter to true disables this functionality
     */
    @Parameter(property = "cxf.disableDependencyScan", defaultValue = "false")
    boolean disableDependencyScan;

    /**
     * A list of wadl files to include. Can contain ant-style wildcards and double wildcards. Defaults to
     * *.wadl
     */
    @Parameter
    String[] includes;

    /**
     * A list of wadl files to exclude. Can contain ant-style wildcards and double wildcards.
     */
    @Parameter
    String[] excludes;

    /**
     * Allows running the JavaToWs in a separate process.
     * Valid values are "false", "always", and "once"
     * The value of "true" is equal to "once"
     */
    @Parameter(defaultValue = "false")
    String fork;
    
    /**
     * Sets the JVM arguments (i.e. <code>-Xms128m -Xmx128m</code>) if fork is set to <code>true</code>.
     */
    @Parameter
    String additionalJvmArgs;

    /**
     * The Maven session.
     */
    @Parameter(readonly = true, required = true, property = "session")
    private MavenSession mavenSession;

    /**
     * The plugin dependencies, needed for the fork mode.
     */
    @Parameter(readonly = true, required = true, property = "plugin.artifacts")
    private List<Artifact> pluginArtifacts;


    /**
     * Sets the Java executable to use when fork parameter is <code>true</code>.
     */
    @Parameter(defaultValue = "${java.home}/bin/java")
    private String javaExecutable;

    
    @Component
    private RepositorySystem repositorySystem;


    private ClassLoader resourceClassLoader;


    private Artifact resolveRemoteWadlArtifact(Artifact artifact)
        throws MojoExecutionException {

        /**
         * First try to find the artifact in the reactor projects of the maven session.
         * So an artifact that is not yet built can be resolved
         */
        List<MavenProject> rProjects = mavenSession.getProjects();
        for (MavenProject rProject : rProjects) {
            if (artifact.getGroupId().equals(rProject.getGroupId())
                && artifact.getArtifactId().equals(rProject.getArtifactId())
                && artifact.getVersion().equals(rProject.getVersion())) {
                Set<Artifact> artifacts = rProject.getArtifacts();
                for (Artifact pArtifact : artifacts) {
                    if ("wadl".equals(pArtifact.getType())) {
                        return pArtifact;
                    }
                }
            }
        }

        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        request.setResolveRoot(true).setResolveTransitively(false);
        request.setServers(mavenSession.getRequest().getServers());
        request.setMirrors(mavenSession.getRequest().getMirrors());
        request.setProxies(mavenSession.getRequest().getProxies());
        request.setLocalRepository(mavenSession.getLocalRepository());
        request.setRemoteRepositories(mavenSession.getRequest().getRemoteRepositories());
        ArtifactResolutionResult result = repositorySystem.resolve(request);
        Artifact resolvedArtifact = result.getOriginatingArtifact();
        if (resolvedArtifact == null && !CollectionUtils.isEmpty(result.getArtifacts())) {
            resolvedArtifact = result.getArtifacts().iterator().next();
        }
        return resolvedArtifact;
    }

    protected void downloadRemoteDocs(List<WadlOption> effectiveOptions) throws MojoExecutionException {

        for (WadlOption option : effectiveOptions) {
            DocumentArtifact wadlA = option.getWadlArtifact();
            if (wadlA == null) {
                return;
            }
            Artifact wadlArtifact = repositorySystem.createArtifact(wadlA.getGroupId(),
                                                                    wadlA.getArtifactId(),
                                                                    wadlA.getVersion(),
                                                                    wadlA.getType());

            wadlArtifact = resolveRemoteWadlArtifact(wadlArtifact);
            if (wadlArtifact != null) {
                String path = wadlArtifact.getFile().getAbsolutePath();
                getLog().info("Resolved WADL artifact to file " + path);
                option.setWadl(path);
            }
        }
    }



    private void addPluginArtifact(Set<URI> artifactsPath) {
        //for Maven 2.x, the actual artifact isn't in the list....  need to try and find it
        URL url = getClass().getResource(getClass().getSimpleName() + ".class");

        try {
            if ("jar".equals(url.getProtocol())) {
                String s = url.getPath();
                if (s.contains("!")) {
                    s = s.substring(0, s.indexOf('!'));
                    url = new URL(s);
                }
            }
            URI uri = new URI(url.getProtocol(), null, url.getPath(), null, null);
            if (uri.getSchemeSpecificPart().endsWith(".class")) {
                String s = uri.toString();
                s = s.substring(0, s.length() - 6 - getClass().getName().length());
                uri = new URI(s);
            }
            File file = new File(uri);
            if (file.exists()) {
                artifactsPath.add(file.toURI());
            }
        } catch (Exception ex) {
            //ex.printStackTrace();
        }

    }

    protected void forkOnce(Set<URI> classPath, List<WadlOption> effectiveOptions)
        throws MojoExecutionException {
        List<WadlOption> toDo = new LinkedList<>();
        List<List<String>> wargs = new LinkedList<>();
        for (WadlOption option : effectiveOptions) {
            File outputDirFile = option.getOutputDir();
            outputDirFile.mkdirs();
            URI basedir = project.getBasedir().toURI();
            for (URI wadlURI : option.getWadlURIs(basedir, getResourceLoader())) {
                File doneFile = getDoneFile(basedir, wadlURI);

                if (!shouldRun(option, doneFile, wadlURI)) {
                    continue;
                }
                doneFile.delete();

                toDo.add(option);

                wargs.add(option.generateCommandLine(outputDirFile, basedir, wadlURI, getLog()
                                                                   .isDebugEnabled()));
            }
        }
        if (wargs.isEmpty()) {
            return;
        }

        Set<URI> artifactsPath = new LinkedHashSet<>();
        for (Artifact a : pluginArtifacts) {
            File file = a.getFile();
            if (file == null) {
                throw new MojoExecutionException("Unable to find file for artifact "
                                                 + a.getGroupId() + ":" + a.getArtifactId()
                                                 + ":" + a.getVersion());
            }
            artifactsPath.add(file.toURI());
        }
        addPluginArtifact(artifactsPath);
        artifactsPath.addAll(classPath);

        String[] args = createForkOnceArgs(wargs);
        runForked(artifactsPath, ForkOnceCodeGenerator.class, args);

        for (WadlOption option : toDo) {
            File[] dirs = option.getDeleteDirs();
            if (dirs != null) {
                for (int idx = 0; idx < dirs.length; ++idx) {
                    deleteDir(dirs[idx]);
                }
            }
            URI basedir = project.getBasedir().toURI();
            for (URI wadlURI : option.getWadlURIs(basedir, getResourceLoader())) {
                File doneFile = getDoneFile(basedir, wadlURI);
                try {
                    doneFile.createNewFile();
                } catch (Throwable e) {
                    getLog().warn("Could not create marker file " + doneFile.getAbsolutePath());
                    getLog().debug(e);
                }
            }
        }
    }

    private String[] createForkOnceArgs(List<List<String>> wargs) throws MojoExecutionException {
        try {
            File f = FileUtils.createTempFile("cxf-w2j", "args");
            PrintWriter fw = new PrintWriter(new FileWriter(f));
            for (List<String> args : wargs) {
                fw.println(Integer.toString(args.size()));
                for (String s : args) {
                    fw.println(s);
                }
            }
            fw.println("-1");
            fw.close();
            return new String[] {f.getAbsolutePath()};
        } catch (IOException ex) {
            throw new MojoExecutionException("Could not create argument file", ex);
        }
    }

    private ClassLoader getResourceLoader() throws MojoExecutionException {
        if (resourceClassLoader == null) {
            try {
                List<?> runtimeClasspathElements = project.getRuntimeClasspathElements();
                List<?> resources = project.getResources();
                List<?> testResources = project.getTestResources();
                URL[] runtimeUrls = new URL[runtimeClasspathElements.size() + resources.size() + testResources.size()];
                for (int i = 0; i < runtimeClasspathElements.size(); i++) {
                    String element = (String)runtimeClasspathElements.get(i);
                    runtimeUrls[i] = new File(element).toURI().toURL();
                }
                for (int i = 0, j = runtimeClasspathElements.size(); i < resources.size(); i++, j++) {
                    Resource r = (Resource)resources.get(i);
                    runtimeUrls[j] = new File(r.getDirectory()).toURI().toURL();
                }
                for (int i = 0, j = runtimeClasspathElements.size() + resources.size(); i < testResources.size();
                    i++, j++) {
                    Resource r = (Resource)testResources.get(i);
                    runtimeUrls[j] = new File(r.getDirectory()).toURI().toURL();
                }
                resourceClassLoader = new URLClassLoader(runtimeUrls, Thread.currentThread()
                    .getContextClassLoader());
            } catch (Exception e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        return resourceClassLoader;
    }

    protected Bus callCodeGenerator(WadlOption option,
                              Bus bus,
                              Set<URI> classPath) throws MojoExecutionException {
        File outputDirFile = option.getOutputDir();
        outputDirFile.mkdirs();
        URI basedir = project.getBasedir().toURI();

        for (URI wadlURI : option.getWadlURIs(basedir, getResourceLoader())) {
            File doneFile = getDoneFile(basedir, wadlURI);

            if (!shouldRun(option, doneFile, wadlURI)) {
                return bus;
            }
            doneFile.delete();

            List<String> list = option.generateCommandLine(outputDirFile, basedir, wadlURI, getLog()
                                                               .isDebugEnabled());
            String[] args = list.toArray(new String[0]);
            getLog().debug("Calling wadl2java with args: " + Arrays.toString(args));

            if (!"false".equals(fork)) {
                Set<URI> artifactsPath = new LinkedHashSet<>();
                for (Artifact a : pluginArtifacts) {
                    File file = a.getFile();
                    if (file == null) {
                        throw new MojoExecutionException("Unable to find file for artifact "
                                                         + a.getGroupId() + ":" + a.getArtifactId()
                                                         + ":" + a.getVersion());
                    }
                    artifactsPath.add(file.toURI());
                }
                addPluginArtifact(artifactsPath);
                artifactsPath.addAll(classPath);

                runForked(artifactsPath, WADLToJava.class, args);

            } else {
                if (bus == null) {
                    bus = BusFactory.newInstance().createBus();
                    BusFactory.setThreadDefaultBus(bus);
                }
                try {
                    new WADLToJava(args).run(new ToolContext());
                } catch (Throwable e) {
                    getLog().debug(e);
                    throw new MojoExecutionException(e.getMessage(), e);
                }
            }


            try {
                doneFile.createNewFile();
            } catch (Throwable e) {
                getLog().warn("Could not create marker file " + doneFile.getAbsolutePath());
                getLog().debug(e);
            }
        }
        return bus;
    }
    private File getJavaExecutable() throws IOException {
        String exe = (SystemUtils.IS_OS_WINDOWS && !javaExecutable.endsWith(".exe")) ? ".exe" : "";
        File javaExe = new File(javaExecutable + exe);

        if (!javaExe.isFile()) {
            throw new IOException("The java executable '" + javaExe
                + "' doesn't exist or is not a file. Verify the <javaExecutable/> parameter.");
        }

        return javaExe;
    }

    private void runForked(Set<URI> classPath, Class<?> cls, String[] args) throws MojoExecutionException {
        getLog().info("Running wadl2java in fork mode...");

        Commandline cmd = new Commandline();
        cmd.getShell().setQuotedArgumentsEnabled(true); // for JVM args
        cmd.setWorkingDirectory(project.getBuild().getDirectory());
        try {
            cmd.setExecutable(getJavaExecutable().getAbsolutePath());
        } catch (IOException e) {
            getLog().debug(e);
            throw new MojoExecutionException(e.getMessage(), e);
        }

        cmd.createArg().setLine(additionalJvmArgs);

        File file = null;
        try {
            //file = new File("/tmp/test.jar");
            file = FileUtils.createTempFile("cxf-codegen", ".jar");

            JarArchiver jar = new JarArchiver();
            jar.setDestFile(file.getAbsoluteFile());

            Manifest manifest = new Manifest();
            Attribute attr = new Attribute();
            attr.setName("Class-Path");
            StringBuilder b = new StringBuilder(8000);
            for (URI cp : classPath) {
                b.append(cp.toURL().toExternalForm()).append(' ');
            }
            attr.setValue(b.toString());
            manifest.getMainSection().addConfiguredAttribute(attr);

            attr = new Attribute();
            attr.setName("Main-Class");
            attr.setValue(cls.getName());
            manifest.getMainSection().addConfiguredAttribute(attr);

            jar.addConfiguredManifest(manifest);
            jar.createArchive();

            cmd.createArg().setValue("-jar");
            cmd.createArg().setValue(file.getAbsolutePath());


        } catch (Exception e1) {
            throw new MojoExecutionException("Could not create runtime jar", e1);
        }
        cmd.addArguments(args);


        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();

        int exitCode;
        try {
            exitCode = CommandLineUtils.executeCommandLine(cmd, out, err);
        } catch (CommandLineException e) {
            getLog().debug(e);
            throw new MojoExecutionException(e.getMessage(), e);
        }

        String output = StringUtils.isEmpty(out.getOutput()) ? null : '\n' + out.getOutput().trim();

        String cmdLine = CommandLineUtils.toString(cmd.getCommandline());

        if (exitCode != 0) {
            if (StringUtils.isNotEmpty(output)) {
                getLog().info(output);
            }

            StringBuilder msg = new StringBuilder("\nExit code: ");
            msg.append(exitCode);
            if (StringUtils.isNotEmpty(err.getOutput())) {
                msg.append(" - ").append(err.getOutput());
            }
            msg.append('\n');
            msg.append("Command line was: ").append(cmdLine).append('\n').append('\n');

            throw new MojoExecutionException(msg.toString());
        }

        if (file != null) {
            file.delete();
        }
        if (StringUtils.isNotEmpty(err.getOutput()) && err.getOutput().contains("WADL2Java Error")) {
            StringBuilder msg = new StringBuilder();
            msg.append(err.getOutput());
            msg.append('\n');
            msg.append("Command line was: ").append(cmdLine).append('\n').append('\n');
            throw new MojoExecutionException(msg.toString());
        }

    }

    private File getDoneFile(URI basedir, URI wadlURI) {
        String doneFileName = wadlURI.toString();

        // Strip the basedir from the doneFileName
        if (doneFileName.startsWith(basedir.toString())) {
            doneFileName = doneFileName.substring(basedir.toString().length());
        }

        // If URL to WADL, replace ? and & since they're invalid chars for file names
        // Not to mention slashes.
        doneFileName = doneFileName.replace('?', '_').replace('&', '_').replace('/', '_').replace('\\', '_')
            .replace(':', '_');

        return new File(markerDirectory, "." + doneFileName + ".DONE");
    }

    /**
     * Determine if code should be generated from the given wadl
     *
     * @param wadlOption
     * @param doneFile
     * @param wadlURI
     * @return
     */
    private boolean shouldRun(WadlOption wadlOption, File doneFile, URI wadlURI) {
        long timestamp = 0;
        if ("file".equals(wadlURI.getScheme())) {
            timestamp = new File(wadlURI).lastModified();
        } else {
            try {
                timestamp = wadlURI.toURL().openConnection().getDate();
            } catch (Exception e) {
                // ignore
            }
        }
        boolean doWork = false;
        if (!doneFile.exists()) {
            doWork = true;
        } else if (timestamp > doneFile.lastModified()) {
            doWork = true;
        } else {
            File[] files = wadlOption.getDependencies();
            if (files != null) {
                for (int z = 0; z < files.length; ++z) {
                    if (files[z].lastModified() > doneFile.lastModified()) {
                        doWork = true;
                    }
                }
            }
        }
        return doWork;
    }

    /**
     * Recursively delete the given directory
     *
     * @param f
     * @return
     */
    protected boolean deleteDir(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (int idx = 0; idx < files.length; ++idx) {
                    deleteDir(files[idx]);
                }
            }
        }

        if (f.exists()) {
            return f.delete();
        }

        return true;
    }

}
