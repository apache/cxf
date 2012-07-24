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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.SystemUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.wsdlto.WSDLToJava;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectUtils;
import org.apache.maven.settings.Proxy;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.Manifest.Attribute;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * @goal wsdl2java
 * @phase generate-sources
 * @description CXF WSDL To Java Tool
 * @requiresDependencyResolution test
 * @threadSafe
 */
public class WSDL2JavaMojo extends AbstractMojo {

    /**
     * @parameter expression="${cxf.testSourceRoot}"
     */
    File testSourceRoot;

    /**
     * Path where the generated sources should be placed
     * 
     * @parameter expression="${cxf.sourceRoot}"
     *            default-value="${project.build.directory}/generated-sources/cxf"
     * @required
     */
    File sourceRoot;

    /**
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    String classesDirectory;

    /**
     * @parameter expression="${project}"
     * @required
     */
    MavenProject project;

    /**
     * Default options to be used when a wsdl has not had it's options explicitly specified.
     * 
     * @parameter
     */
    Option defaultOptions = new Option();

    /**
     * @parameter
     */
    WsdlOption wsdlOptions[];

    /**
     * @parameter expression="${cxf.wsdlRoot}" default-value="${basedir}/src/main/resources/wsdl"
     */
    File wsdlRoot;

    /**
     * @parameter expression="${cxf.testWsdlRoot}" default-value="${basedir}/src/test/resources/wsdl"
     */
    File testWsdlRoot;

    /**
     * Directory in which the "DONE" markers are saved that
     * 
     * @parameter expression="${cxf.markerDirectory}"
     *            default-value="${project.build.directory}/cxf-codegen-plugin-markers"
     */
    File markerDirectory;

    /**
     * Use the compile classpath rather than the test classpath for execution useful if the test dependencies
     * clash with those of wsdl2java
     * 
     * @parameter expression="${cxf.useCompileClasspath}" default-value="false"
     */
    boolean useCompileClasspath;
    
    
    /**
     * Disables the scanning of the wsdlRoot/testWsdlRoot directories configured above.
     * By default, we scan for *.wsdl (see include/exclude params as well) in the wsdlRoot
     * directories and run wsdl2java on all the wsdl's we find.    This disables that scan
     * and requires an explicit wsdlOption to be set for each wsdl that needs to be processed.
     * @parameter expression="${cxf.disableDirectoryScan}" default-value="false"
     */
    boolean disableDirectoryScan;

    /**
     * By default all maven dependencies of type "wsdl" are added to the effective wsdlOptions. Setting this
     * parameter to true disables this functionality
     * 
     * @parameter expression="${cxf.disableDependencyScan}" default-value="false"
     */
    boolean disableDependencyScan;

    /**
     * A list of wsdl files to include. Can contain ant-style wildcards and double wildcards. Defaults to
     * *.wsdl
     * 
     * @parameter
     */
    String includes[];

    /**
     * A list of wsdl files to exclude. Can contain ant-style wildcards and double wildcards.
     * 
     * @parameter
     */
    String excludes[];

    /**
     * The local repository taken from Maven's runtime. Typically $HOME/.m2/repository.
     * 
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    private ArtifactRepository localRepository;

    /**
     * Artifact factory, needed to create artifacts.
     * 
     * @component
     * @readonly
     * @required
     */
    private ArtifactFactory artifactFactory;

    /**
     * The remote repositories used as specified in your POM.
     * 
     * @parameter expression="${project.repositories}"
     * @readonly
     * @required
     */
    private List repositories;

    /**
     * Artifact repository factory component.
     * 
     * @component
     * @readonly
     * @required
     */
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    /**
     * The Maven session.
     * 
     * @parameter expression="${session}"
     * @readonly
     * @required
     */
    private MavenSession mavenSession;

    /**
     * @component
     * @readonly
     * @required
     */
    private ArtifactResolver artifactResolver;

    /**
     * The plugin dependencies, needed for the fork mode.
     *
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    private List<Artifact> pluginArtifacts;
    /**
     * Allows running the JavaToWs in a separate process.
     * Valid values are "false", "always", and "once"
     * The value of "true" is equal to "once"
     *
     * @parameter default-value="false"
     * @since 2.4
     */
    private String fork;

    /**
     * Sets the Java executable to use when fork parameter is <code>true</code>.
     *
     * @parameter default-value="${java.home}/bin/java"
     * @since 2.4
     */
    private String javaExecutable;

    /**
     * Sets the JVM arguments (i.e. <code>-Xms128m -Xmx128m</code>) if fork is set to <code>true</code>.
     *
     * @parameter
     * @since 2.4
     */
    private String additionalJvmArgs;
    
    /**
     * Merge WsdlOptions that point to the same file by adding the extraargs to the first option and deleting
     * the second from the options list
     * 
     * @param options
     */
    private void mergeOptions(List<WsdlOption> effectiveWsdlOptions) {
        if (wsdlOptions == null) {
            return;
        }
        File outputDirFile = testSourceRoot == null ? sourceRoot : testSourceRoot;
        for (WsdlOption o : wsdlOptions) {
            if (defaultOptions != null) {
                o.merge(defaultOptions);
            }
            if (o.getOutputDir() == null) {
                o.setOutputDir(outputDirFile);
            }

            File file = o.getWsdlFile(project.getBasedir());
            if (file != null && file.exists()) {
                file = file.getAbsoluteFile();
                for (WsdlOption o2 : effectiveWsdlOptions) {
                    File file2 = o2.getWsdlFile(project.getBasedir());
                    if (file2 != null && file2.exists() && file2.getAbsoluteFile().equals(file)) {
                        o.getExtraargs().addAll(0, o2.getExtraargs());
                        effectiveWsdlOptions.remove(o2);
                        break;
                    }
                }
            }
            effectiveWsdlOptions.add(o);
        }
    }

    /**
     * @return effective WsdlOptions
     * @throws MojoExecutionException
     */
    private List<WsdlOption> createWsdlOptionsFromScansAndExplicitWsdlOptions() 
        throws MojoExecutionException {
        List<WsdlOption> effectiveWsdlOptions = new ArrayList<WsdlOption>();
        List<WsdlOption> temp;
        if (wsdlRoot != null && wsdlRoot.exists() && !disableDirectoryScan) {
            temp = WsdlOptionLoader.loadWsdlOptionsFromFiles(wsdlRoot, includes, excludes, defaultOptions,
                                                             sourceRoot);
            effectiveWsdlOptions.addAll(temp);
        }
        if (testWsdlRoot != null && testWsdlRoot.exists() && !disableDirectoryScan) {
            temp = WsdlOptionLoader.loadWsdlOptionsFromFiles(testWsdlRoot, includes, excludes,
                                                             defaultOptions, testSourceRoot);
            effectiveWsdlOptions.addAll(temp);
        }
        if (!disableDependencyScan) {
            temp = WsdlOptionLoader.loadWsdlOptionsFromDependencies(project, defaultOptions, sourceRoot);
            effectiveWsdlOptions.addAll(temp);
        }
        mergeOptions(effectiveWsdlOptions);
        downloadRemoteWsdls(effectiveWsdlOptions);
//        String buildDir = project.getBuild().getDirectory();
//        File tempBindingDir = new File(buildDir, TEMPBINDINGS_DIR);
//        for (WsdlOption o : effectiveWsdlOptions) {
//            BindingFileHelper.setWsdlLocationInBindingsIfNotSet(project.getBasedir(), tempBindingDir, o,
//                                                                getLog());
//        }
        return effectiveWsdlOptions;
    }
    
    @SuppressWarnings("unchecked")
    private Artifact resolveRemoteWsdlArtifact(List remoteRepos, Artifact artifact)
        throws MojoExecutionException {
        
        /**
         * First try to find the artifact in the reactor projects of the maven session.
         * So an artifact that is not yet built can be resolved
         */
        List<MavenProject> rProjects = mavenSession.getSortedProjects();
        for (MavenProject rProject : rProjects) {
            if (artifact.getGroupId().equals(rProject.getGroupId())
                && artifact.getArtifactId().equals(rProject.getArtifactId()) 
                && artifact.getVersion().equals(rProject.getVersion())) {
                Set<Artifact> artifacts = rProject.getArtifacts();
                for (Artifact pArtifact : artifacts) {
                    if ("wsdl".equals(pArtifact.getType())) {
                        return pArtifact;
                    }
                }
            }
        }
        
        /**
         * If this did not work resolve the artifact using the artifactResolver
         */
        try {
            artifactResolver.resolve(artifact, remoteRepos, localRepository);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("Error downloading wsdl artifact.", e);
        } catch (ArtifactNotFoundException e) {
            throw new MojoExecutionException("Resource can not be found.", e);
        }
        
        return artifact;
    }

    private void downloadRemoteWsdls(List<WsdlOption> effectiveWsdlOptions) throws MojoExecutionException {
        List remoteRepos;
        try {
            remoteRepos = ProjectUtils.buildArtifactRepositories(repositories, artifactRepositoryFactory,
                                                                 mavenSession.getContainer());
        } catch (InvalidRepositoryException e) {
            throw new MojoExecutionException("Error build repositories for remote wsdls", e);
        }
        
        for (WsdlOption wsdlOption : effectiveWsdlOptions) {
            WsdlArtifact wsdlA = wsdlOption.getWsdlArtifact();
            if (wsdlA == null) {
                return;
            }
            Artifact wsdlArtifact = artifactFactory.createArtifact(wsdlA.getGroupId(), wsdlA.getArtifactId(),
                                                                   wsdlA.getVersion(),
                                                                   Artifact.SCOPE_COMPILE, wsdlA.getType());
            wsdlArtifact = resolveRemoteWsdlArtifact(remoteRepos, wsdlArtifact);
            if (wsdlArtifact != null) {
                File supposedFile = wsdlArtifact.getFile();
                if (!supposedFile.exists() || !supposedFile.isFile()) {
                    getLog().info("Apparent Maven bug: wsdl artifact 'resolved' to " 
                        + supposedFile.getAbsolutePath() + " for " + wsdlArtifact.toString());
                    continue;
                }
                String path = supposedFile.getAbsolutePath();
                getLog().info("Resolved WSDL artifact to file " + path);
                wsdlOption.setWsdl(path);
            }
        }
    }

    private void configureProxyServerSettings() throws MojoExecutionException {

        Proxy proxy = mavenSession.getSettings().getActiveProxy();

        if (proxy != null) {

            getLog().info("Using proxy server configured in maven.");

            if (proxy.getHost() == null) {
                throw new MojoExecutionException("Proxy in settings.xml has no host");
            } else {
                System.setProperty("proxySet", "true");
                System.setProperty("proxyHost", proxy.getHost());
                System.setProperty("proxyPort", String.valueOf(proxy.getPort()));
            }


        }
    }

    public void execute() throws MojoExecutionException {
        if (includes == null) {
            includes = new String[] {
                "*.wsdl"
            };
        }
        defaultOptions.addDefaultBindingFileIfExists(project.getBasedir());
        File classesDir = new File(classesDirectory);
        classesDir.mkdirs();
        markerDirectory.mkdirs();

        configureProxyServerSettings();

        List<WsdlOption> effectiveWsdlOptions = createWsdlOptionsFromScansAndExplicitWsdlOptions();

        if (effectiveWsdlOptions.size() == 0) {
            getLog().info("Nothing to generate");
            return;
        }

        ClassLoaderSwitcher classLoaderSwitcher = new ClassLoaderSwitcher(getLog());
        boolean result = true;

        Bus bus = null;
        try {
            Set<URI> cp = classLoaderSwitcher.switchClassLoader(project, useCompileClasspath, classesDir);

            if ("once".equals(fork) || "true".equals(fork)) {
                forkOnce(cp, effectiveWsdlOptions);
            } else {
                for (WsdlOption o : effectiveWsdlOptions) {
                    bus = callWsdl2Java(o, bus, cp);
    
                    File dirs[] = o.getDeleteDirs();
                    if (dirs != null) {
                        for (int idx = 0; idx < dirs.length; ++idx) {
                            result = result && deleteDir(dirs[idx]);
                        }
                    }
                }
            }
        } finally {
            // cleanup as much as we can.
            if (bus != null) {
                bus.shutdown(true);
            }
            classLoaderSwitcher.restoreClassLoader();
        }
        if (project != null && sourceRoot != null && sourceRoot.exists()) {
            project.addCompileSourceRoot(sourceRoot.getAbsolutePath());
        }
        if (project != null && testSourceRoot != null && testSourceRoot.exists()) {
            project.addTestCompileSourceRoot(testSourceRoot.getAbsolutePath());
        }

        System.gc();
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

    private void forkOnce(Set<URI> classPath, List<WsdlOption> effectiveWsdlOptions) 
        throws MojoExecutionException {
        List<WsdlOption> toDo = new LinkedList<WsdlOption>();
        List<List<String>> wargs = new LinkedList<List<String>>();
        for (WsdlOption wsdlOption : effectiveWsdlOptions) {
            File outputDirFile = wsdlOption.getOutputDir();
            outputDirFile.mkdirs();
            URI basedir = project.getBasedir().toURI();
            URI wsdlURI = wsdlOption.getWsdlURI(basedir);
            File doneFile = getDoneFile(basedir, wsdlURI);

            if (!shouldRun(wsdlOption, doneFile, wsdlURI)) {
                continue;
            }
            doneFile.delete();
            
            toDo.add(wsdlOption);
            
            wargs.add(wsdlOption.generateCommandLine(outputDirFile, basedir, wsdlURI, getLog()
                                                               .isDebugEnabled()));
        }
        if (wargs.isEmpty()) {
            return;
        }
        
        Set<URI> artifactsPath = new LinkedHashSet<URI>();
        for (Artifact a : pluginArtifacts) {
            File file = a.getFile();
            if (file == null) {
                throw new MojoExecutionException("Unable to find " + file + " for artifact "
                                                 + a.getGroupId() + ":" + a.getArtifactId()
                                                 + ":" + a.getVersion());
            }
            artifactsPath.add(file.toURI());
        }
        addPluginArtifact(artifactsPath);
        artifactsPath.addAll(classPath);
        
        String args[] = createForkOnceArgs(wargs);
        runForked(artifactsPath, ForkOnceWSDL2Java.class, args);
        
        for (WsdlOption wsdlOption : toDo) {
            File dirs[] = wsdlOption.getDeleteDirs();
            if (dirs != null) {
                for (int idx = 0; idx < dirs.length; ++idx) {
                    deleteDir(dirs[idx]);
                }
            }
            URI basedir = project.getBasedir().toURI();
            URI wsdlURI = wsdlOption.getWsdlURI(basedir);
            File doneFile = getDoneFile(basedir, wsdlURI);
            try {
                doneFile.createNewFile();
            } catch (Throwable e) {
                getLog().warn("Could not create marker file " + doneFile.getAbsolutePath());
                getLog().debug(e);
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

    private Bus callWsdl2Java(WsdlOption wsdlOption, 
                              Bus bus,
                              Set<URI> classPath) throws MojoExecutionException {
        File outputDirFile = wsdlOption.getOutputDir();
        outputDirFile.mkdirs();
        URI basedir = project.getBasedir().toURI();
        URI wsdlURI = wsdlOption.getWsdlURI(basedir);
        File doneFile = getDoneFile(basedir, wsdlURI);

        if (!shouldRun(wsdlOption, doneFile, wsdlURI)) {
            return bus;
        }
        doneFile.delete();

        List<String> list = wsdlOption.generateCommandLine(outputDirFile, basedir, wsdlURI, getLog()
                                                           .isDebugEnabled());
        String[] args = (String[])list.toArray(new String[list.size()]);
        getLog().debug("Calling wsdl2java with args: " + Arrays.toString(args));
        
        if (!"false".equals(fork)) {
            Set<URI> artifactsPath = new LinkedHashSet<URI>();
            for (Artifact a : pluginArtifacts) {
                File file = a.getFile();
                if (file == null) {
                    throw new MojoExecutionException("Unable to find " + file + " for artifact "
                                                     + a.getGroupId() + ":" + a.getArtifactId()
                                                     + ":" + a.getVersion());
                }
                artifactsPath.add(file.toURI());
            }
            addPluginArtifact(artifactsPath);
            artifactsPath.addAll(classPath);
            
            runForked(artifactsPath, WSDLToJava.class, args);

        } else {
            if (bus == null) {
                bus = BusFactory.newInstance().createBus();
                BusFactory.setThreadDefaultBus(bus);
            }
            try {
                new WSDLToJava(args).run(new ToolContext());
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

    private void runForked(Set<URI> classPath, Class cls, String[] args) throws MojoExecutionException {
        getLog().info("Running wsdl2java in fork mode...");
        getLog().debug("Running wsdl2java in fork mode with args " + Arrays.asList(args));

        Commandline cmd = new Commandline();
        cmd.getShell().setQuotedArgumentsEnabled(false); // for JVM args
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

            StringBuffer msg = new StringBuffer("\nExit code: ");
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
        if (StringUtils.isNotEmpty(err.getOutput()) && err.getOutput().contains("WSDL2Java Error")) {
            StringBuffer msg = new StringBuffer();
            msg.append(err.getOutput());
            msg.append('\n');
            msg.append("Command line was: ").append(cmdLine).append('\n').append('\n');
            throw new MojoExecutionException(msg.toString());
        }

    }

    private File getDoneFile(URI basedir, URI wsdlURI) {
        String doneFileName = wsdlURI.toString();
        
        // Strip the basedir from the doneFileName
        if (doneFileName.startsWith(basedir.toString())) {
            doneFileName = doneFileName.substring(basedir.toString().length());
        }

        // If URL to WSDL, replace ? and & since they're invalid chars for file names
        // Not to mention slashes.
        doneFileName = doneFileName.replace('?', '_').replace('&', '_').replace('/', '_').replace('\\', '_')
            .replace(':', '_');

        return new File(markerDirectory, "." + doneFileName + ".DONE");
    }

    /**
     * Determine if code should be generated from the given wsdl
     * 
     * @param wsdlOption
     * @param doneFile
     * @param wsdlURI
     * @return
     */
    private boolean shouldRun(WsdlOption wsdlOption, File doneFile, URI wsdlURI) {
        long timestamp = 0;
        if ("file".equals(wsdlURI.getScheme())) {
            timestamp = new File(wsdlURI).lastModified();
        } else {
            try {
                timestamp = wsdlURI.toURL().openConnection().getDate();
            } catch (Exception e) {
                // ignore
            }
        }
<<<<<<< HEAD:maven-plugins/codegen-plugin/src/main/java/org/apache/cxf/maven_plugin/WSDL2JavaMojo.java
        boolean doWork = false;
        if (!doneFile.exists()) {
            doWork = true;
        } else if (timestamp > doneFile.lastModified()) {
            doWork = true;
        } else if (wsdlOption.isDefServiceName()) {
            doWork = true;
        } else {
            File files[] = wsdlOption.getDependencies();
            if (files != null) {
                for (int z = 0; z < files.length; ++z) {
                    if (files[z].lastModified() > doneFile.lastModified()) {
                        doWork = true;
                    }
=======
        if (file == null || !file.exists()) {
            file = new File(option.getUri());
        }
        if (!file.exists()) {
            file = new File(baseDir, option.getUri());
        }
        return file;
    }
    
    public URI getWsdlURI(GenericWsdlOption option, URI baseURI) throws MojoExecutionException {
        String wsdlLocation = option.getUri();
        if (wsdlLocation == null) {
            throw new MojoExecutionException("No wsdl available for base URI " + baseURI);
        }
        File wsdlFile = new File(wsdlLocation);
        return wsdlFile.exists() ? wsdlFile.toURI() 
            : baseURI.resolve(URIParserUtil.escapeChars(wsdlLocation));
    }

    protected void downloadRemoteWsdls(List<GenericWsdlOption> effectiveWsdlOptions) 
        throws MojoExecutionException {

        for (GenericWsdlOption wsdlOption : effectiveWsdlOptions) {
            WsdlArtifact wsdlA = wsdlOption.getArtifact();
            if (wsdlA == null) {
                continue;
            }
            Artifact wsdlArtifact = artifactFactory.createBuildArtifact(wsdlA.getGroupId(),
                                                                        wsdlA.getArtifactId(),
                                                                        wsdlA.getVersion(), wsdlA.getType());
            wsdlArtifact = resolveRemoteWsdlArtifact(wsdlArtifact);
            if (wsdlArtifact != null) {
                File supposedFile = wsdlArtifact.getFile();
                if (!supposedFile.exists() || !supposedFile.isFile()) {
                    getLog().info("Apparent Maven bug: wsdl artifact 'resolved' to "
                                      + supposedFile.getAbsolutePath() + " for " + wsdlArtifact.toString());
                    continue;
>>>>>>> 646689a... Merged revisions 1365176 via  git cherry-pick from:maven-plugins/codegen-plugin/src/main/java/org/apache/cxf/maven_plugin/AbstractCodegenMoho.java
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
    private boolean deleteDir(File f) {
        if (f.isDirectory()) {
            File files[] = f.listFiles();
            for (int idx = 0; idx < files.length; ++idx) {
                deleteDir(files[idx]);
            }
        }

        if (f.exists()) {
            return f.delete();
        }

        return true;
    }

}
