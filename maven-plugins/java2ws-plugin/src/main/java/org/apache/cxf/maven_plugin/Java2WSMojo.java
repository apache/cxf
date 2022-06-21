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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang3.SystemUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.tools.common.CommandInterfaceUtils;
import org.apache.cxf.tools.java2ws.JavaToWS;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * @goal java2ws
 * @description CXF Java To Webservice Tool
 * @requiresDependencyResolution test
 * @threadSafe
*/
public class Java2WSMojo extends AbstractMojo {
    /**
     * @parameter
     * @required
     */
    private String className;

    /**
     * @parameter expression="${project.build.outputDirectory}"
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
     * @parameter
     */
    private String address;


    /**
     * @parameter
     */
    private String classifier;

    /**
     * @parameter expression="${project.compileClasspathElements}"
     * @required
     */
    private List<?> classpathElements;

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
     * @parameter
     */
    private String portName;


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

    /**
     * The plugin dependencies, needed for the fork mode.
     *
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    private List<Artifact> pluginArtifacts;

    /**
     * Specifies whether the JavaToWs execution should be skipped.
     *
     * @parameter default-value="false"
     * @since 2.4
     */
    private Boolean skip;

    /**
     * Allows running the JavaToWs in a separate process.
     *
     * @parameter default-value="false"
     * @since 2.4
     */
    private Boolean fork;

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
     * Determines how the classpath is applied to the executed command,
     * either as a command line argument, or as an environment variable
     * for the executing process. Only applicable when fork is set to <code>true</code>
     * (which is always the case for JDK 9+).
     * <p>
     * Primarily useful on Windows because of length limitations for command arguments,
     * which is why the default is <code>true</code> in that case.
     *
     * @parameter default-value="false"
     * @since 3.3
     */
    private Boolean classpathAsEnvVar;

    /**
     * Disable garbage collection at the end of the execution.
     *
     * @parameter expression="${cxf.skipGarbageCollection}" default-value="false"
     * @since 3.3.1
     */
    private boolean skipGarbageCollection;


    public void execute() throws MojoExecutionException {
        boolean requiresModules = JavaUtils.isJava9Compatible();
        if (requiresModules) {
            // Since JEP 261 ("Jigsaw"), access to some packages must be granted
            fork = true;
            additionalJvmArgs = "--add-exports=jdk.xml.dom/org.w3c.dom.html=ALL-UNNAMED "
                    + "--add-exports=java.xml/com.sun.org.apache.xerces.internal.impl.xs=ALL-UNNAMED "
                    + "--add-opens java.base/java.security=ALL-UNNAMED "
                    + "--add-opens java.base/java.net=ALL-UNNAMED "
                    + "--add-opens java.base/java.lang=ALL-UNNAMED "
                    + "--add-opens java.base/java.util=ALL-UNNAMED "
                    + "--add-opens java.base/java.util.concurrent=ALL-UNNAMED " 
                    + (additionalJvmArgs == null ? "" : additionalJvmArgs); 
        }
        if (fork && SystemUtils.IS_OS_WINDOWS) {
            // Windows does not allow for very long command lines,
            // so by default CLASSPATH environment variable is used.
            classpathAsEnvVar = true;
        }
        System.setProperty("org.apache.cxf.JDKBugHacks.defaultUsesCaches", "true");
        if (skip) {
            getLog().info("Skipping Java2WS execution");
            return;
        }

        ClassLoaderSwitcher classLoaderSwitcher = new ClassLoaderSwitcher(getLog());

        try {
            String cp = classLoaderSwitcher.switchClassLoader(project, false,
                                                              classpath, classpathElements);
            if (fork) {
                List<String> artifactsPath = new ArrayList<>(pluginArtifacts.size());
                for (Artifact a : pluginArtifacts) {
                    File file = a.getFile();
                    if (file == null) {
                        throw new MojoExecutionException("Unable to find file for artifact "
                                                         + a.getGroupId() + ":" + a.getArtifactId()
                                                         + ":" + a.getVersion());
                    }
                    artifactsPath.add(file.getPath());
                }
                cp = StringUtils.join(artifactsPath.iterator(), File.pathSeparator) + File.pathSeparator + cp;
            }

            List<String> args = initArgs(classpathAsEnvVar ? null : cp);
            processJavaClass(args, classpathAsEnvVar ? cp : null);
        } finally {
            classLoaderSwitcher.restoreClassLoader();
        }

        if (!skipGarbageCollection) {
            System.gc();
        }
    }

    private List<String> initArgs(String cp) {
        List<String> args = new ArrayList<>();

        if (fork) {
            if (!StringUtils.isEmpty(additionalJvmArgs)) {
                String[] split = additionalJvmArgs.split("\\s+");
                args.addAll(Arrays.asList(split));
            }
            // @see JavaToWS#isExitOnFinish()
            args.add("-DexitOnFinish=true");
        }

        if (!StringUtils.isEmpty(cp)) {
            // classpath arg
            args.add("-cp");
            args.add(cp);
        }

        if (fork) {
            args.add(JavaToWS.class.getCanonicalName());
        }

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

        if (!StringUtils.isEmpty(frontend)) {
            args.add("-frontend");
            args.add(frontend);
        }

        if (!StringUtils.isEmpty(databinding)) {
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

        // soap12 arg
        if (soap12 != null && soap12.booleanValue()) {
            args.add("-soap12");
        }

        // target namespace arg
        if (!StringUtils.isEmpty(targetNamespace)) {
            args.add("-t");
            args.add(targetNamespace);
        }

        // servicename arg
        if (!StringUtils.isEmpty(serviceName)) {
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

        // address arg
        if (!StringUtils.isEmpty(address)) {
            args.add("-address");
            args.add(address);
        }

        // portname arg
        if (!StringUtils.isEmpty(portName)) {
            args.add("-portname");
            args.add(portName);
        }

        if (argline != null) {
            StringTokenizer stoken = new StringTokenizer(argline, " ");
            while (stoken.hasMoreTokens()) {
                args.add(stoken.nextToken());
            }
        }

        // classname arg
        args.add(className);

        return args;
    }

    private void processJavaClass(List<String> args, String cp) throws MojoExecutionException {
        if (!fork) {
            try {
                CommandInterfaceUtils.commandCommonMain();
                JavaToWS j2w = new JavaToWS(args.toArray(new String[0]));
                j2w.run();
            } catch (OutOfMemoryError e) {
                getLog().debug(e);

                StringBuilder msg = new StringBuilder(128);
                msg.append(e.getMessage()).append('\n');
                msg.append("Try to run this goal using the <fork>true</fork> and "
                        + "<additionalJvmArgs>-Xms128m -Xmx128m</additionalJvmArgs> parameters.");
                throw new MojoExecutionException(msg.toString(), e);
            } catch (Throwable e) {
                getLog().debug(e);
                throw new MojoExecutionException(e.getMessage(), e);
            }
        } else {
            getLog().info("Running java2ws in fork mode...");

            Commandline cmd = new Commandline();
            cmd.getShell().setQuotedArgumentsEnabled(true); // for JVM args
            cmd.setWorkingDirectory(project.getBuild().getDirectory());
            try {
                cmd.setExecutable(getJavaExecutable().getAbsolutePath());
            } catch (IOException e) {
                getLog().debug(e);
                throw new MojoExecutionException(e.getMessage(), e);
            }

            cmd.addArguments(args.toArray(new String[0]));
            if (classpathAsEnvVar && !StringUtils.isEmpty(cp)) {
                cmd.addEnvironment("CLASSPATH", cp);
            }

            CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
            CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();

            String cmdLine = CommandLineUtils.toString(cmd.getCommandline());

            int exitCode;
            try {
                exitCode = CommandLineUtils.executeCommandLine(cmd, out, err);
            } catch (CommandLineException e) {
                getLog().debug(e);
                StringBuilder msg = new StringBuilder(e.getMessage());
                if (!(fork && classpathAsEnvVar)) {
                    msg.append('\n');
                    msg.append("Try to run this goal using <fork>true</fork> and "
                            + "<classpathAsEnvVar>true</classpathAsEnvVar>.");
                }
                msg.append('\n');
                msg.append("Command line was: ").append(cmdLine).append('\n');
                if (classpathAsEnvVar && !StringUtils.isEmpty(cp)) {
                    msg.append("   CLASSPATH env: ").append(cp).append('\n');
                }
                msg.append('\n');
                throw new MojoExecutionException(msg.toString(), e);
            }

            String output = StringUtils.isEmpty(out.getOutput()) ? null : '\n' + out.getOutput().trim();

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
                msg.append("Command line was: ").append(cmdLine).append('\n');
                if (classpathAsEnvVar && !StringUtils.isEmpty(cp)) {
                    msg.append("   CLASSPATH env: ").append(cp).append('\n');
                }
                msg.append('\n');

                throw new MojoExecutionException(msg.toString());
            }

            if (StringUtils.isNotEmpty(err.getOutput()) && err.getOutput().contains("JavaToWS Error")) {
                StringBuilder msg = new StringBuilder();
                msg.append(err.getOutput());
                msg.append('\n');
                msg.append("Command line was: ").append(cmdLine).append('\n');
                if (classpathAsEnvVar && !StringUtils.isEmpty(cp)) {
                    msg.append("   CLASSPATH env: ").append(cp).append('\n');
                }
                msg.append('\n');
                throw new MojoExecutionException(msg.toString());
            }
        }

        // Attach the generated wsdl file to the artifacts that get deployed
        // with the enclosing project
        if (attachWsdl && outputFile != null) {
            File wsdlFile = new File(outputFile);
            if (wsdlFile.exists()) {
                
                
                boolean hasWsdlAttached = false;
                for (Artifact a : project.getAttachedArtifacts()) {
                    if ("wsdl".equals(a.getType()) && classifier != null && classifier.equals(a.getClassifier())) {
                        hasWsdlAttached = true;
                    }
                }
                if (!hasWsdlAttached) {
                    if (classifier != null) {
                        projectHelper.attachArtifact(project, wsdlFile.getName(), classifier, wsdlFile);
                    } else {
                        projectHelper.attachArtifact(project, wsdlFile.getName(), wsdlFile);
                    }
                }
            }
        }
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
}
