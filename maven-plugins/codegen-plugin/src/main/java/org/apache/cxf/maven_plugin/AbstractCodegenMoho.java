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
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.SystemUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.common.util.URIParserUtil;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectUtils;
import org.apache.maven.settings.Proxy;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.Manifest.Attribute;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.sonatype.plexus.build.incremental.BuildContext;

public abstract class AbstractCodegenMoho extends AbstractMojo {

    /**
     * JVM/System property name holding the hostname of the http proxy.
     */
    private static final String HTTP_PROXY_HOST = "http.proxyHost";

    /**
     * JVM/System property name holding the port of the http proxy.
     */
    private static final String HTTP_PROXY_PORT = "http.proxyPort";

    /**
     * JVM/System property name holding the list of hosts/patterns that
     * should not use the proxy configuration.
     */
    private static final String HTTP_NON_PROXY_HOSTS = "http.nonProxyHosts";

    /**
     * JVM/System property name holding the username of the http proxy.
     */
    private static final String HTTP_PROXY_USER = "http.proxyUser";

    /**
     * JVM/System property name holding the password of the http proxy.
     */
    private static final String HTTP_PROXY_PASSWORD = "http.proxyPassword";


    /**
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    protected String classesDirectory;

    /**
     * By default all maven dependencies of type "wsdl" are added to the effective wsdlOptions. Setting this
     * parameter to true disables this functionality
     *
     * @parameter expression="${cxf.disableDependencyScan}" default-value="false"
     */
    protected boolean disableDependencyScan;
    /**
     * Disables the scanning of the wsdlRoot/testWsdlRoot directories.
     * By default, we scan for *.wsdl (see include/exclude params as well) in the wsdlRoot
     * directories and run the tool on all the wsdls we find. This disables that scan
     * and requires an explicit wsdlOption to be set for each wsdl that needs to be processed.
     * @parameter expression="${cxf.disableDirectoryScan}" default-value="false"
     */
    protected boolean disableDirectoryScan;
    /**
     * Allows running the JavaToWs in a separate process. Valid values are "false", "always", and "once" The
     * value of "true" is equal to "once"
     *
     * @parameter default-value="false"
     * @since 2.4
     */
    protected String fork;
    /**
     * A list of wsdl files to include. Can contain ant-style wildcards and double wildcards. Defaults to
     * *.wsdl
     *
     * @parameter
     */
    protected String includes[];
    /**
     * Directory in which the "DONE" markers are saved that
     *
     * @parameter expression="${cxf.markerDirectory}"
     *            default-value="${project.build.directory}/cxf-codegen-plugin-markers"
     */
    protected File markerDirectory;
    /**
     * The plugin dependencies, needed for the fork mode.
     *
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    protected List<Artifact> pluginArtifacts;
    /**
     * @parameter expression="${project}"
     * @required
     */
    protected MavenProject project;
    /**
     * Use the compile classpath rather than the test classpath for execution useful if the test dependencies
     * clash with those of wsdl2java
     *
     * @parameter expression="${cxf.useCompileClasspath}" default-value="false"
     */
    protected boolean useCompileClasspath;
    /**
     * A list of wsdl files to exclude. Can contain ant-style wildcards and double wildcards.
     *
     * @parameter
     */
    protected String excludes[];
    /**
     * @parameter expression="${cxf.testWsdlRoot}" default-value="${basedir}/src/test/resources/wsdl"
     */
    protected File testWsdlRoot;

    /**
     * @parameter expression="${cxf.wsdlRoot}" default-value="${basedir}/src/main/resources/wsdl"
     */
    protected File wsdlRoot;

    /** @component */
    protected BuildContext buildContext;


    /**
     * Sets the JVM arguments (i.e. <code>-Xms128m -Xmx128m</code>) if fork is set to <code>true</code>.
     *
     * @parameter expression="${cxf.codegen.jvmArgs}"
     * @since 2.4
     */
    private String additionalJvmArgs;
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
     * Sets the Java executable to use when fork parameter is <code>true</code>.
     *
     * @parameter
     * @readonly
     * @since 2.4
     */
    private String javaExecutable;
    /**
     * The remote repositories used as specified in your POM.
     *
     * @parameter expression="${project.repositories}"
     * @readonly
     * @required
     */
    private List<Repository> repositories;
    /**
     * Artifact repository factory component.
     *
     * @component
     * @readonly
     * @required
     */
    private ArtifactRepositoryFactory artifactRepositoryFactory;
    /**
     * The toolchain manager.
     * 
     * @component
     * @readonly
     */
    private ToolchainManager toolchainManager;

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
    private ProjectDependenciesResolver projectDependencyResolver;
     /**
      * @component
      * @readonly
      * @required
      */
    private ArtifactResolver artifactResolver;


    public AbstractCodegenMoho() {
        super();
    }

    public void execute() throws MojoExecutionException {
        // add the generated source into compile source
        // do this step first to ensure the source folder will be added to the Eclipse classpath
        if (project != null && getGeneratedSourceRoot() != null) {
            project.addCompileSourceRoot(getGeneratedSourceRoot().getAbsolutePath());
        }
        if (project != null && getGeneratedTestRoot() != null) {
            project.addTestCompileSourceRoot(getGeneratedTestRoot().getAbsolutePath());
        }

        // if this is an m2e configuration build then return immediately without doing any work
        if (project != null && buildContext.isIncremental() && !buildContext.hasDelta(project.getBasedir())) {
            return;
        }

        File classesDir = new File(classesDirectory);
        /*
         * This shouldn't be needed, but it's harmless.
         */
        classesDir.mkdirs();

        if (includes == null) {
            includes = new String[] {
                "*.wsdl"
            };
        }

        markerDirectory.mkdirs();

        String originalProxyHost = SystemPropertyAction.getProperty(HTTP_PROXY_HOST);
        String originalProxyPort = SystemPropertyAction.getProperty(HTTP_PROXY_PORT);
        String originalNonProxyHosts = SystemPropertyAction.getProperty(HTTP_NON_PROXY_HOSTS);
        String originalProxyUser = SystemPropertyAction.getProperty(HTTP_PROXY_USER);
        String originalProxyPassword = SystemPropertyAction.getProperty(HTTP_PROXY_PASSWORD);

        configureProxyServerSettings();

        List<GenericWsdlOption> effectiveWsdlOptions = createWsdlOptionsFromScansAndExplicitWsdlOptions();

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
                for (GenericWsdlOption o : effectiveWsdlOptions) {
                    bus = generate(o, bus, cp);

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
            restoreProxySetting(originalProxyHost, originalProxyPort, originalNonProxyHosts,
                                originalProxyUser, originalProxyPassword);
        }

        // refresh the generated sources
        if (project != null && getGeneratedSourceRoot() != null && getGeneratedSourceRoot().exists()) {
            buildContext.refresh(getGeneratedSourceRoot().getAbsoluteFile());
        }
        if (project != null && getGeneratedTestRoot() != null && getGeneratedTestRoot().exists()) {
            buildContext.refresh(getGeneratedTestRoot().getAbsoluteFile());
        }
        System.gc();
    }

    private void restoreProxySetting(String originalProxyHost, String originalProxyPort,
                                     String originalNonProxyHosts,
                                     String originalProxyUser,
                                     String originalProxyPassword) {
        if (originalProxyHost != null) {
            System.setProperty(HTTP_PROXY_HOST, originalProxyHost);
        } else {
            System.getProperties().remove(HTTP_PROXY_HOST);
        }
        if (originalProxyPort != null) {
            System.setProperty(HTTP_PROXY_PORT, originalProxyPort);
        } else {
            System.getProperties().remove(HTTP_PROXY_PORT);
        }
        if (originalNonProxyHosts != null) {
            System.setProperty(HTTP_NON_PROXY_HOSTS, originalNonProxyHosts);
        } else {
            System.getProperties().remove(HTTP_NON_PROXY_HOSTS);
        }
        if (originalProxyUser != null) {
            System.setProperty(HTTP_PROXY_USER, originalProxyUser);
        } else {
            System.getProperties().remove(HTTP_PROXY_USER);
        }
        if (originalProxyPassword != null) {
            System.setProperty(HTTP_PROXY_PASSWORD, originalProxyPassword);
        } else {
            System.getProperties().remove(HTTP_PROXY_PASSWORD);
        }
        Proxy proxy = mavenSession.getSettings().getActiveProxy();
        if (proxy != null && !StringUtils.isEmpty(proxy.getUsername())
            && !StringUtils.isEmpty(proxy.getPassword())) {
            Authenticator.setDefault(null);
        }
    }

    protected abstract Bus generate(GenericWsdlOption o,
                                    Bus bus, Set<URI> cp) throws MojoExecutionException;

    protected void addPluginArtifact(Set<URI> artifactsPath) {
        // for Maven 2.x, the actual artifact isn't in the list.... need to try and find it
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
            // ex.printStackTrace();
        }

    }

    protected void configureProxyServerSettings() throws MojoExecutionException {

        Proxy proxy = mavenSession.getSettings().getActiveProxy();

        if (proxy != null) {

            getLog().info("Using proxy server configured in maven.");

            if (proxy.getHost() == null) {
                throw new MojoExecutionException("Proxy in settings.xml has no host");
            } else {
                if (proxy.getHost() != null) {
                    System.setProperty(HTTP_PROXY_HOST, proxy.getHost());
                }
                if (String.valueOf(proxy.getPort()) != null) {
                    System.setProperty(HTTP_PROXY_PORT, String.valueOf(proxy.getPort()));
                }
                if (proxy.getNonProxyHosts() != null) {
                    System.setProperty(HTTP_NON_PROXY_HOSTS, proxy.getNonProxyHosts());
                }
                if (!StringUtils.isEmpty(proxy.getUsername())
                    && !StringUtils.isEmpty(proxy.getPassword())) {
                    final String authUser = proxy.getUsername();
                    final String authPassword = proxy.getPassword();
                    Authenticator.setDefault(new Authenticator() {
                        public PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(authUser, authPassword.toCharArray());
                        }
                    });

                    System.setProperty(HTTP_PROXY_USER, authUser);
                    System.setProperty(HTTP_PROXY_PORT, authPassword);
                }
            }

        }
    }

    protected abstract List<GenericWsdlOption> createWsdlOptionsFromScansAndExplicitWsdlOptions()
        throws MojoExecutionException;

    /**
     * Recursively delete the given directory
     *
     * @param f
     * @return
     */
    protected boolean deleteDir(File f) {
        if (f.isDirectory()) {
            File files[] = f.listFiles();
            for (int idx = 0; idx < files.length; ++idx) {
                deleteDir(files[idx]);
            }
        }

        if (f.exists()) {
            return f.delete();
        }
        buildContext.refresh(f.getParentFile());

        return true;
    }

    protected abstract String getMarkerSuffix();

    protected List<String> generateCommandLine(GenericWsdlOption wsdlOption)
        throws MojoExecutionException {

        File outputDirFile = wsdlOption.getOutputDir();
        outputDirFile.mkdirs();
        URI basedir = project.getBasedir().toURI();
        URI wsdlURI = getWsdlURI(wsdlOption, basedir);
        return wsdlOption.generateCommandLine(outputDirFile, basedir, wsdlURI,
                                              getLog().isDebugEnabled());
    }

    protected void forkOnce(Set<URI> classPath, List<GenericWsdlOption> effectiveWsdlOptions)
        throws MojoExecutionException {
        List<GenericWsdlOption> toDo = new LinkedList<GenericWsdlOption>();
        List<List<String>> wargs = new LinkedList<List<String>>();
        for (GenericWsdlOption wsdlOption : effectiveWsdlOptions) {
            File outputDirFile = wsdlOption.getOutputDir();
            outputDirFile.mkdirs();
            URI basedir = project.getBasedir().toURI();
            URI wsdlURI = getWsdlURI(wsdlOption, basedir);
            File doneFile = getDoneFile(basedir, wsdlURI, getMarkerSuffix());

            if (!shouldRun(wsdlOption, doneFile, wsdlURI)) {
                continue;
            }
            doneFile.delete();

            toDo.add(wsdlOption);

            wargs.add(generateCommandLine(wsdlOption));
        }
        if (wargs.isEmpty()) {
            return;
        }

        Set<URI> artifactsPath = new LinkedHashSet<URI>();
        for (Artifact a : pluginArtifacts) {
            File file = a.getFile();
            if (file == null) {
                throw new MojoExecutionException("Unable to find " + file + " for artifact " + a.getGroupId()
                                                 + ":" + a.getArtifactId() + ":" + a.getVersion());
            }
            artifactsPath.add(file.toURI());
        }
        addPluginArtifact(artifactsPath);
        artifactsPath.addAll(classPath);

        String args[] = createForkOnceArgs(wargs);
        runForked(artifactsPath, getForkClass().getName(), args);

        for (GenericWsdlOption wsdlOption : toDo) {
            File dirs[] = wsdlOption.getDeleteDirs();
            if (dirs != null) {
                for (int idx = 0; idx < dirs.length; ++idx) {
                    deleteDir(dirs[idx]);
                }
            }
            URI basedir = project.getBasedir().toURI();
            URI wsdlURI = getWsdlURI(wsdlOption, basedir);
            File doneFile = getDoneFile(basedir, wsdlURI, getMarkerSuffix());
            try {
                createMarkerFile(wsdlOption, doneFile, wsdlURI);
            } catch (Throwable e) {
                getLog().warn("Could not create marker file " + doneFile.getAbsolutePath());
                getLog().debug(e);
            }
        }
    }

    protected abstract Class<?> getForkClass();

    protected File getDoneFile(URI basedir, URI wsdlURI, String mojo) {
        String doneFileName = wsdlURI.toString();

        // Strip the basedir from the doneFileName
        if (doneFileName.startsWith(basedir.toString())) {
            doneFileName = doneFileName.substring(basedir.toString().length());
        }

        // If URL to WSDL, replace ? and & since they're invalid chars for file names
        // Not to mention slashes.
        doneFileName = doneFileName.replace('?', '_').replace('&', '_').replace('/', '_').replace('\\', '_')
            .replace(':', '_');

        return new File(markerDirectory, "." + doneFileName + "." + mojo + ".DONE");
    }

    protected abstract File getGeneratedSourceRoot();

    protected abstract File getGeneratedTestRoot();

    protected void runForked(Set<URI> classPath,
                             String mainClassName,
                             String[] args) throws MojoExecutionException {
        getLog().info("Running code generation in fork mode...");
        getLog().debug("Running code generation in fork mode with args " + Arrays.asList(args));

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
            // file = new File("/tmp/test.jar");
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
            attr.setValue(mainClassName);
            manifest.getMainSection().addConfiguredAttribute(attr);

            jar.addConfiguredManifest(manifest);
            jar.createArchive();

            cmd.createArg().setValue("-jar");

            String tmpFilePath = file.getAbsolutePath();
            if (tmpFilePath.contains(" ")) {
                //ensure the path is in double quotation marks if the path contain space
                tmpFilePath = "\"" + tmpFilePath + "\"";
            }
            cmd.createArg().setValue(tmpFilePath);

        } catch (Exception e1) {
            throw new MojoExecutionException("Could not create runtime jar", e1);
        }
        cmd.addArguments(args);

        StreamConsumer out = new StreamConsumer() {
            public void consumeLine(String line) {
                getLog().info(line);
            }
        };
        final StringBuilder b = new StringBuilder();
        StreamConsumer err = new StreamConsumer() {
            public void consumeLine(String line) {
                b.append(line);
                b.append("\n");
                getLog().warn(line);
            }
        };
        int exitCode;
        try {
            exitCode = CommandLineUtils.executeCommandLine(cmd, out, err);
        } catch (CommandLineException e) {
            getLog().debug(e);
            throw new MojoExecutionException(e.getMessage(), e);
        }


        String cmdLine = CommandLineUtils.toString(cmd.getCommandline());

        if (exitCode != 0) {
            StringBuffer msg = new StringBuffer("\nExit code: ");
            msg.append(exitCode);
            msg.append('\n');
            msg.append("Command line was: ").append(cmdLine).append('\n').append('\n');

            throw new MojoExecutionException(msg.toString());
        }

        file.delete();
        if (b.toString().contains("WSDL2Java Error")) {
            StringBuffer msg = new StringBuffer();
            msg.append(b.toString());
            msg.append('\n');
            msg.append("Command line was: ").append(cmdLine).append('\n').append('\n');
            throw new MojoExecutionException(msg.toString());
        }

    }

    /**
     * Determine if code should be generated from the given wsdl
     *
     * @param wsdlOption
     * @param doneFile
     * @param wsdlURI
     * @return
     */
    protected abstract boolean shouldRun(GenericWsdlOption wsdlOption, File doneFile, URI wsdlURI);

    protected void createMarkerFile(GenericWsdlOption wsdlOption, File doneFile, URI wsdlURI) throws IOException {
        doneFile.createNewFile();
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
            return new String[] {
                f.getAbsolutePath()
            };
        } catch (IOException ex) {
            throw new MojoExecutionException("Could not create argument file", ex);
        }
    }

    /**
     * Try to find a file matching the wsdl path (either absolutely, relatively to the current dir or to
     * the project base dir)
     *
     * @return wsdl file
     */
    public File getWsdlFile(GenericWsdlOption option, File baseDir) {
        if (option.getUri() == null) {
            return null;
        }
        File file = null;
        try {
            URI uri = new URI(option.getUri());
            if (uri.isAbsolute()) {
                file = new File(uri);
            }
        } catch (Exception e) {
            // ignore
        }
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
            Artifact wsdlArtifact = artifactFactory.createArtifactWithClassifier(wsdlA.getGroupId(),
                                                                        wsdlA.getArtifactId(),
                                                                        wsdlA.getVersion(),
                                                                        wsdlA.getType(),
                                                                        wsdlA.getClassifier());
            wsdlArtifact = resolveRemoteWsdlArtifact(wsdlArtifact);
            if (wsdlArtifact != null) {
                File supposedFile = wsdlArtifact.getFile();
                if (!supposedFile.exists() || !supposedFile.isFile()) {
                    getLog().info("Apparent Maven bug: wsdl artifact 'resolved' to "
                                      + supposedFile.getAbsolutePath() + " for " + wsdlArtifact.toString());
                    continue;
                }
                String path = supposedFile.getAbsolutePath();
                getLog().info("Resolved WSDL artifact to file " + path);
                wsdlOption.setUri(path);
            }
        }
    }

    private File getJavaExecutable() throws IOException {
        if (javaExecutable != null) {
            getLog().debug("Plugin configuration set the 'javaExecutable' parameter to " + javaExecutable);
        } else {
            Toolchain tc = toolchainManager.getToolchainFromBuildContext("jdk", mavenSession);
            if (tc != null) {
                getLog().info("Using toolchain " + tc + " to find the java executable");
                javaExecutable = tc.findTool("java");
            } else {
                getLog().debug("The java executable is set to default value");
                javaExecutable = SystemUtils.getJavaHome() + File.separator + "bin" + File.separator + "java";
            }
        }
        String exe = SystemUtils.IS_OS_WINDOWS && !javaExecutable.endsWith(".exe") ? ".exe" : "";
        File javaExe = new File(javaExecutable + exe);
        if (!javaExe.isFile()) {
            throw new IOException(
                                  "The java executable '"
                                      + javaExe
                                      + "' doesn't exist or is not a file."
                                      + "Verify the <javaExecutable/> parameter or toolchain configuration.");
        }
        getLog().info("The java executable is " + javaExe.getAbsolutePath());
        return javaExe;
    }

    private Artifact resolveRemoteWsdlArtifact(Artifact artifact) throws MojoExecutionException {
        Artifact remoteWsdl = resolveDependentWsdl(artifact);
        if (remoteWsdl == null) {
            remoteWsdl = resolveAttachedWsdl(artifact);
        }
        if (remoteWsdl == null) {
            remoteWsdl = resolveArbitraryWsdl(artifact);
        }
        if (remoteWsdl != null && remoteWsdl.isResolved()) {
            return remoteWsdl;
        }
        throw new MojoExecutionException(String.format("Failed to resolve WSDL artifact %s",
                                                       artifact.toString()));
    }

    private Artifact resolveDependentWsdl(Artifact artifact) {
        Collection<String> scopes = new ArrayList<String>();
        scopes.add(Artifact.SCOPE_RUNTIME);
        Set<Artifact> artifactSet = null;
        try {
            artifactSet = projectDependencyResolver.resolve(project, scopes, mavenSession);
        } catch (AbstractArtifactResolutionException e) {
            getLog().info("Error resolving dependent wsdl artifact.", e);
        }
        return findWsdlArtifact(artifact, artifactSet);
    }

    private Artifact resolveAttachedWsdl(Artifact artifact) {
        List<MavenProject> rProjects = mavenSession.getSortedProjects();
        List<Artifact> artifactList = new ArrayList<Artifact>();
        for (MavenProject rProject : rProjects) {
            List<Artifact> list = CastUtils.cast(rProject.getAttachedArtifacts());
            if (list != null) {
                artifactList.addAll(list);
            }
        }
        return findWsdlArtifact(artifact, artifactList);
    }

    private Artifact resolveArbitraryWsdl(Artifact artifact) {
        try {
            @SuppressWarnings("rawtypes")
            List remoteRepos = ProjectUtils.buildArtifactRepositories(repositories,
                                                                      artifactRepositoryFactory,
                                                                 mavenSession.getContainer());
            artifactResolver.resolve(artifact, remoteRepos, localRepository);
        } catch (InvalidRepositoryException e) {
            getLog().info("Error build repositories for remote wsdls.", e);
        } catch (AbstractArtifactResolutionException e) {
            getLog().info("Error resolving arbitrary wsdl artifact.", e);
        }
        return artifact;
    }

    private Artifact findWsdlArtifact(Artifact targetArtifact, Collection<Artifact> artifactSet) {
        if (artifactSet != null && !artifactSet.isEmpty()) {
            for (Artifact pArtifact : artifactSet) {
                if (targetArtifact.getGroupId().equals(pArtifact.getGroupId())
                    && targetArtifact.getArtifactId().equals(pArtifact.getArtifactId())
                    && targetArtifact.getVersion().equals(pArtifact.getVersion())
                    && "wsdl".equals(pArtifact.getType())) {
                    getLog().info(String.format("%s resolved to %s", pArtifact.toString(), pArtifact
                                      .getFile().getAbsolutePath()));
                    return pArtifact;
                }
            }
        }
        return null;
    }

}
