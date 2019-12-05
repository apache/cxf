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

package org.apache.cxf.maven_plugin.wsdl2java;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.maven_plugin.AbstractCodegenMojo;
import org.apache.cxf.maven_plugin.GenericWsdlOption;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolErrorListener;
import org.apache.cxf.tools.util.OutputStreamCreator;
import org.apache.cxf.tools.wsdlto.WSDLToJava;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "wsdl2java", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true,
      requiresDependencyResolution = ResolutionScope.TEST)
public class WSDL2JavaMojo extends AbstractCodegenMojo {

    final class MavenToolErrorListener extends ToolErrorListener {
        private final List<File> errorfiles;

        MavenToolErrorListener(List<File> errorfiles) {
            this.errorfiles = errorfiles;
        }

        public void addError(final String file, int line, int column, String message, Throwable t) {
            super.addError(file, line, column, message, t);

            File f = mapFile(file);

            if (f != null && !errorfiles.contains(f)) {
                buildContext.removeMessages(f);
                errorfiles.add(f);
            }
            if (f == null) {
                if (file == null) {
                    f = new File("null");
                } else {
                    f = new File(file) {
                        private static final long serialVersionUID = 1L;
                        public String getAbsolutePath() {
                            return file;
                        }
                    };
                }
            }
            buildContext.addMessage(f, line, column, message, BuildContext.SEVERITY_ERROR, t);
        }

        public void addWarning(final String file, int line, int column, String message, Throwable t) {
            File f = mapFile(file);
            if (f != null && !errorfiles.contains(f)) {
                buildContext.removeMessages(f);
                errorfiles.add(f);
            }
            if (f == null) {
                f = new File(file) {
                    private static final long serialVersionUID = 1L;
                    public String getAbsolutePath() {
                        return file;
                    }
                };
            }
            //don't send to super which just LOG.warns.   We'll let Maven do that to
            //not duplicate the error message.
            buildContext.addMessage(f, line, column, message, BuildContext.SEVERITY_WARNING, t);
        }

        private File mapFile(String s) {
            File file = null;
            if (s != null && s.startsWith("file:")) {
                if (s.contains("#")) {
                    s = s.substring(0, s.indexOf('#'));
                }
                try {
                    URI uri = new URI(s);
                    file = new File(uri);
                } catch (URISyntaxException e) {
                    //ignore
                }
            }
            return file;
        }
    }

    @Parameter(property = "cxf.testSourceRoot")
    File testSourceRoot;

    /**
     * Path where the generated sources should be placed
     *
     */
    @Parameter(required = true, defaultValue = "${project.build.directory}/generated-sources/cxf",
               property = "cxf.sourceRoot")
    File sourceRoot;

    /**
     * Options that specify WSDLs to process and/or control the processing of wsdls.
     * If you have enabled wsdl scanning, these elements attach options to particular wsdls.
     * If you have not enabled wsdl scanning, these options call out the wsdls to process.
     */
    @Parameter
    WsdlOption[] wsdlOptions;

    /**
     * Default options to be used when a wsdl has not had it's options explicitly specified.
     */
    @Parameter
    Option defaultOptions = new Option();

    /**
     * Encoding to use for generated sources
     */
    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    String encoding;

    /**
     * Merge WsdlOptions that point to the same file by adding the extraargs to the first option and deleting
     * the second from the options list
     *
     * @param effectiveWsdlOptions
     */
    protected void mergeOptions(List<GenericWsdlOption> effectiveWsdlOptions) {

        File outputDirFile = getGeneratedTestRoot() == null
                             ? getGeneratedSourceRoot() : getGeneratedTestRoot();

        List<GenericWsdlOption> newList = new ArrayList<>();

        for (GenericWsdlOption go : effectiveWsdlOptions) {
            WsdlOption o = (WsdlOption) go;
            if (defaultOptions != null) {
                o.merge(defaultOptions);
            }
            /*
             * If not output dir at all, go for tests, and failing that, source.
             */
            if (o.getOutputDir() == null) {
                o.setOutputDir(outputDirFile);
            }

            File file = o.getWsdlFile(project.getBasedir());
            if (file != null && file.exists()) {
                file = file.getAbsoluteFile();
                boolean duplicate = false;
                for (GenericWsdlOption o2w : newList) {
                    WsdlOption o2 = (WsdlOption) o2w;
                    File file2 = o2.getWsdlFile(project.getBasedir());
                    if (file2 != null && file2.exists() && file2.getAbsoluteFile().equals(file)) {
                        o2.getExtraargs().addAll(0, o.getExtraargs());
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    newList.add(o);
                }
            } else {
                newList.add(o);
            }
        }
        effectiveWsdlOptions.clear();
        effectiveWsdlOptions.addAll(newList);
    }

    /**
     * Determine if code should be generated from the given wsdl
     *
     * @param genericWsdlOption
     * @param doneFile
     * @param wsdlURI
     * @return
     */
    protected boolean shouldRun(GenericWsdlOption genericWsdlOption,
                                File doneFile, URI wsdlURI) {
        WsdlOption wsdlOption = (WsdlOption) genericWsdlOption;
        long timestamp = getTimestamp(wsdlURI);
        boolean doWork = false;
        if (!doneFile.exists()) {
            doWork = true;
        } else if (timestamp > doneFile.lastModified()) {
            doWork = true;
        } else if (wsdlOption.isDefServiceName()) {
            doWork = true;
        } else {
            URI[] dependencies = wsdlOption.getDependencyURIs(project
                    .getBasedir().toURI());
            if (dependencies != null) {
                for (int z = 0; z < dependencies.length; ++z) {
                    long dependencyTimestamp = getTimestamp(dependencies[z]);
                    if (dependencyTimestamp > doneFile.lastModified()) {
                        doWork = true;
                        break;
                    }
                }
            }
        }
        if (!doWork) {
            URI basedir = project.getBasedir().toURI();
            String options = wsdlOption.generateCommandLine(null, basedir, wsdlURI, false).toString();
            try (DataInputStream reader = new DataInputStream(Files.newInputStream(doneFile.toPath()))) {
                String s = reader.readUTF();
                if (!options.equals(s)) {
                    doWork = true;
                }
            } catch (Exception ex) {
                //ignore
            }
        }
        return doWork;
    }

    protected void createMarkerFile(GenericWsdlOption wsdlOption, File doneFile, URI wsdlURI) throws IOException {
        doneFile.createNewFile();
        URI basedir = project.getBasedir().toURI();
        String options = wsdlOption.generateCommandLine(null, basedir, wsdlURI, false).toString();
        try (DataOutputStream writer = new DataOutputStream(Files.newOutputStream(doneFile.toPath()))) {
            writer.writeUTF(options);
            writer.flush();
        }
    }


    /**
     * Finds the timestamp for a given URI. Calls {@link #getBaseFileURI(URI)} prior to the timestamp
     * check in order to handle "classpath" and "jar" URIs.
     *
     * @param uri the URI to timestamp
     * @return a timestamp
     */
    protected long getTimestamp(URI uri) {
        long timestamp = 0;
        URI baseURI = getBaseFileURI(uri);
        if ("file".equals(baseURI.getScheme())) {
            timestamp = new File(baseURI).lastModified();
        } else {
            try {
                timestamp = baseURI.toURL().openConnection().getDate();
            } catch (Exception e) {
                // ignore
            }
        }
        return timestamp;
    }

    /**
     * Finds the base file URI that 'contains' the given URI. If the URI can not be resolved to a file URI
     * then the original URI is returned. This method currently attempts to resolve only "classpath" and
     * "jar" URIs.
     *
     * @param uri the URI to resolve
     * @return uri a file URI if the original URI is contained in a file, otherwise the original URI
     */
    protected URI getBaseFileURI(URI uri) {
        if ("classpath".equals(uri.getScheme())) {
            URL resource = ClassLoaderUtils.getResource(uri.toString().substring(10), getClass());
            if (resource != null) {
                try {
                    return getBaseFileURI(resource.toURI());
                } catch (URISyntaxException e) {
                    // ignore
                }
            }
        } else if ("jar".equals(uri.getScheme())) {
            String jarUrl = uri.toString();
            int embeddedUrlEndIndex = jarUrl.lastIndexOf("!/");
            if (embeddedUrlEndIndex != -1) {
                String embeddedUrl = jarUrl.substring(4, embeddedUrlEndIndex);
                try {
                    return getBaseFileURI(new URI(embeddedUrl));
                } catch (URISyntaxException e) {
                    // ignore
                }
            }
        }
        return uri;
    }

    protected List<String> generateCommandLine(GenericWsdlOption wsdlOption)
        throws MojoExecutionException {
        List<String> ret = super.generateCommandLine(wsdlOption);
        if (encoding != null) {
            ret.add(0, "-encoding");
            ret.add(1, encoding);
        }
        return ret;
    }

    @Override
    protected Bus generate(GenericWsdlOption genericWsdlOption,
                           Bus bus,
                           Set<URI> classPath) throws MojoExecutionException {
        WsdlOption wsdlOption = (WsdlOption) genericWsdlOption;
        File outputDirFile = wsdlOption.getOutputDir();
        outputDirFile.mkdirs();
        URI basedir = project.getBasedir().toURI();
        URI wsdlURI = wsdlOption.getWsdlURI(basedir);
        File doneFile = getDoneFile(basedir, wsdlURI, "java");

        if (!shouldRun(wsdlOption, doneFile, wsdlURI)) {
            return bus;
        }
        doneFile.delete();

        try {
            File file = new File(getBaseFileURI(wsdlURI));
            if (file.exists()) {
                buildContext.removeMessages(file);
            }
        } catch (Throwable t) {
            //ignore
        }
        if (wsdlOption.getDependencies() != null) {
            for (URI dependency : wsdlOption.getDependencyURIs(project
                    .getBasedir().toURI())) {
                URI baseDependency = getBaseFileURI(dependency);
                if ("file".equals(baseDependency.getScheme())) {
                    buildContext.removeMessages(new File(baseDependency));
                }
            }
        }

        List<String> list = wsdlOption.generateCommandLine(outputDirFile, basedir, wsdlURI,
                                                           getLog().isDebugEnabled());
        if (encoding != null) {
            list.add(0, "-encoding");
            list.add(1, encoding);
        }
        String[] args = list.toArray(new String[0]);
        getLog().debug("Calling wsdl2java with args: " + Arrays.toString(args));

        if (!"false".equals(fork)) {
            Set<URI> artifactsPath = new LinkedHashSet<>();
            for (Artifact a : pluginArtifacts) {
                File file = a.getFile();
                if (file == null) {
                    throw new MojoExecutionException("Unable to find (null) file for artifact "
                                                     + a.getGroupId() + ":" + a.getArtifactId()
                                                     + ":" + a.getVersion());
                }
                artifactsPath.add(file.toURI());
            }
            addPluginArtifact(artifactsPath);
            artifactsPath.addAll(classPath);

            runForked(artifactsPath, WSDLToJava.class.getName(), args);

        } else {
            if (bus == null) {
                bus = BusFactory.newInstance().createBus();
                BusFactory.setThreadDefaultBus(bus);
            }
            try {
                ToolContext ctx = new ToolContext();
                final List<File> files = new ArrayList<>();
                final List<File> errorfiles = new ArrayList<>();
                ctx.put(OutputStreamCreator.class, new OutputStreamCreator() {
                    public OutputStream createOutputStream(File file) throws IOException {
                        files.add(file);
                        return buildContext.newFileOutputStream(file);
                    }
                });
                ctx.setErrorListener(new MavenToolErrorListener(errorfiles));
                new WSDLToJava(args).run(ctx);

                List<File> oldFiles = CastUtils.cast((List<?>)buildContext
                                                     .getValue("cxf.file.list." + doneFile.getName()));
                if (oldFiles != null) {
                    for (File f : oldFiles) {
                        if (!files.contains(f)) {
                            f.delete();
                            buildContext.refresh(f);
                        }
                    }
                }

                buildContext.setValue("cxf.file.list." + doneFile.getName(), files);
            } catch (Throwable e) {
                buildContext.setValue("cxf.file.list." + doneFile.getName(), null);
                getLog().debug(e);
                if (e instanceof RuntimeException) {
                    throw (RuntimeException)e;
                }
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }


        try {
            createMarkerFile(wsdlOption, doneFile, wsdlURI);
            buildContext.refresh(doneFile);
        } catch (Throwable e) {
            getLog().warn("Could not create marker file " + doneFile.getAbsolutePath());
            getLog().debug(e);
            throw new MojoExecutionException("Failed to create marker file " + doneFile.getAbsolutePath());
        }
        if (project != null && getGeneratedSourceRoot() != null && getGeneratedSourceRoot().exists()) {
            project.addCompileSourceRoot(getGeneratedSourceRoot().getAbsolutePath());
            buildContext.refresh(getGeneratedSourceRoot().getAbsoluteFile());
        }
        if (project != null && getGeneratedTestRoot() != null && getGeneratedTestRoot().exists()) {
            project.addTestCompileSourceRoot(getGeneratedTestRoot().getAbsolutePath());
            buildContext.refresh(getGeneratedTestRoot().getAbsoluteFile());
        }
        return bus;
    }

    /**
     * @return effective WsdlOptions
     * @throws MojoExecutionException
     */
    protected List<GenericWsdlOption> createWsdlOptionsFromScansAndExplicitWsdlOptions()
        throws MojoExecutionException {
        List<GenericWsdlOption> effectiveWsdlOptions = new ArrayList<>();

        if (wsdlOptions != null) {
            for (WsdlOption wo : wsdlOptions) {
                effectiveWsdlOptions.add(wo);
            }
        }

        List<GenericWsdlOption> temp;
        if (wsdlRoot != null && wsdlRoot.exists() && !disableDirectoryScan) {
            temp = WsdlOptionLoader.loadWsdlOptionsFromFiles(wsdlRoot, includes, excludes,
                                                             getGeneratedSourceRoot());
            effectiveWsdlOptions.addAll(temp);
        }
        if (testWsdlRoot != null && testWsdlRoot.exists() && !disableDirectoryScan) {
            temp = WsdlOptionLoader.loadWsdlOptionsFromFiles(testWsdlRoot, includes, excludes,
                                                             getGeneratedTestRoot());
            effectiveWsdlOptions.addAll(temp);
        }
        if (!disableDependencyScan) {
            temp = WsdlOptionLoader.loadWsdlOptionsFromDependencies(project,
                                                                    getGeneratedSourceRoot());
            effectiveWsdlOptions.addAll(temp);
        }
        mergeOptions(effectiveWsdlOptions);
        downloadRemoteWsdls(effectiveWsdlOptions);
        return effectiveWsdlOptions;
    }

    @Override
    protected File getGeneratedSourceRoot() {
        return sourceRoot;
    }

    @Override
    protected File getGeneratedTestRoot() {
        return testSourceRoot;
    }

    @Override
    protected Class<?> getForkClass() {
        return ForkOnceWSDL2Java.class;
    }

    @Override
    public void execute() throws MojoExecutionException {
        defaultOptions.addDefaultBindingFileIfExists(project.getBasedir());
        super.execute();
    }

    @Override
    protected String getMarkerSuffix() {
        return "java";
    }

}
