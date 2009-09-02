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
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.wsdlto.WSDLToJava;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * @goal wsdl2java
 * @phase generate-sources
 * @description CXF WSDL To Java Tool
 * @requiresDependencyResolution test
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
     *             default-value="${project.build.directory}/generated/src/main/java"
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
    Option defaultOptions;

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
     * Create WsdlOption objects for each wsdl file found in the root dir. includes, excludes filter which
     * files are considered. The defaultOptions will be applied.
     * 
     * @param root Base directory to search
     * @param output
     * @return
     * @throws MojoExecutionException
     */
    private List<WsdlOption> getWsdlOptionsFromDir(final File root, final File output)
        throws MojoExecutionException {
        List<WsdlOption> options = new ArrayList<WsdlOption>();
        for (WsdlOption o : new WsdlOptionLoader().load(root, includes, excludes, defaultOptions)) {
            if (o.getOutputDir() == null) {
                o.setOutputDir(output);
            }
            if (!options.contains(o)) {
                options.add(o);
            }
        }
        return options;
    }

    /**
     * Try to find a file matching the given wsdlPath (either absolutely, relatively to the current dir or to
     * the project base dir)
     * 
     * @param wsdlPath
     * @return wsdl file
     */
    private File getFileFromWsdlPath(String wsdlPath) {
        File file = null;
        try {
            URI uri = new URI(wsdlPath);
            if (uri.isAbsolute()) {
                file = new File(uri);
            }
        } catch (Exception e) {
            // ignore
        }
        if (file == null || !file.exists()) {
            file = new File(wsdlPath);
        }
        if (!file.exists()) {
            file = new File(project.getBasedir(), wsdlPath);
        }
        return file;
    }

    /**
     * Merge WsdlOptions that point to the same file by adding the extraargs to the first option and deleting
     * the second from the options list
     * 
     * @param options
     */
    private void mergeOptions(List<WsdlOption> effectiveWsdlOptions, WsdlOption[] explicitWsdlOptions) {
        File outputDirFile = testSourceRoot == null ? sourceRoot : testSourceRoot;
        for (WsdlOption o : explicitWsdlOptions) {
            if (o.getOutputDir() == null) {
                o.setOutputDir(outputDirFile);
            }

            File file = getFileFromWsdlPath(o.getWsdl());
            if (file.exists()) {
                file = file.getAbsoluteFile();
                for (WsdlOption o2 : effectiveWsdlOptions) {
                    File file2 = getFileFromWsdlPath(o2.getWsdl());
                    if (file2.exists() && file2.getAbsoluteFile().equals(file)) {
                        o.getExtraargs().addAll(0, o2.getExtraargs());
                        effectiveWsdlOptions.remove(o2);
                        break;
                    }
                }
            }
            effectiveWsdlOptions.add(0, o);
        }
    }

    /**
     * @return effective WsdlOptions
     * @throws MojoExecutionException
     */
    private WsdlOption[] createWsdlOptionsFromWsdlFilesAndExplicitWsdlOptions() 
        throws MojoExecutionException {
        List<WsdlOption> effectiveWsdlOptions = new ArrayList<WsdlOption>();
        if (wsdlRoot != null && wsdlRoot.exists()) {
            effectiveWsdlOptions.addAll(getWsdlOptionsFromDir(wsdlRoot, sourceRoot));
        }
        if (testWsdlRoot != null && testWsdlRoot.exists()) {
            effectiveWsdlOptions.addAll(getWsdlOptionsFromDir(testWsdlRoot, testSourceRoot));
        }

        if (wsdlOptions != null) {
            mergeOptions(effectiveWsdlOptions, wsdlOptions);
        }
        return effectiveWsdlOptions.toArray(new WsdlOption[effectiveWsdlOptions.size()]);
    }

    public void execute() throws MojoExecutionException {
        if (includes == null) {
            includes = new String[] {
                "*.wsdl"
            };
        }

        File classesDir = new File(classesDirectory);
        classesDir.mkdirs();
        markerDirectory.mkdirs();

        WsdlOption[] effectiveWsdlOptions = createWsdlOptionsFromWsdlFilesAndExplicitWsdlOptions();

        if (effectiveWsdlOptions.length == 0) {
            getLog().info("Nothing to generate");
            return;
        }

        ClassLoaderSwitcher classLoaderSwitcher = new ClassLoaderSwitcher(getLog());
        boolean result = true;

        try {
            classLoaderSwitcher.switchClassLoader(project, useCompileClasspath, classesDir);

            for (WsdlOption o : effectiveWsdlOptions) {
                processWsdl(o);

                File dirs[] = o.getDeleteDirs();
                if (dirs != null) {
                    for (int idx = 0; idx < dirs.length; ++idx) {
                        result = result && deleteDir(dirs[idx]);
                    }
                }
            }
        } finally {
            // cleanup as much as we can.
            Bus bus = BusFactory.getDefaultBus(false);
            if (bus != null) {
                bus.shutdown(true);
            }
            classLoaderSwitcher.restoreClassLoader();
            org.apache.cxf.tools.wsdlto.core.PluginLoader.unload();
        }
        if (project != null && sourceRoot != null && sourceRoot.exists()) {
            project.addCompileSourceRoot(sourceRoot.getAbsolutePath());
        }
        if (project != null && testSourceRoot != null && testSourceRoot.exists()) {
            project.addTestCompileSourceRoot(testSourceRoot.getAbsolutePath());
        }

        System.gc();
    }

    private void processWsdl(WsdlOption wsdlOption) throws MojoExecutionException {

        File outputDirFile = wsdlOption.getOutputDir();
        outputDirFile.mkdirs();

        String wsdlLocation = wsdlOption.getWsdl();
        File wsdlFile = new File(wsdlLocation);
        URI basedir = project.getBasedir().toURI();
        URI wsdlURI;
        if (wsdlFile.exists()) {
            wsdlURI = wsdlFile.toURI();
        } else {
            wsdlURI = basedir.resolve(wsdlLocation);
        }

        String doneFileName = wsdlURI.toString();
        if (doneFileName.startsWith(basedir.toString())) {
            doneFileName = doneFileName.substring(basedir.toString().length());
        }

        // If URL to WSDL, replace ? and & since they're invalid chars for file names
        // Not to mention slashes.

        doneFileName = doneFileName.replace('?', '_').replace('&', '_').replace('/', '_').replace('\\', '_')
            .replace(':', '_');

        File doneFile = new File(markerDirectory, "." + doneFileName + ".DONE");

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

        boolean doWork = false;
        if (!doneFile.exists()) {
            doWork = true;
        } else if (timestamp > doneFile.lastModified()) {
            doWork = true;
        } else if (isDefServiceName(wsdlOption)) {
            doWork = true;
        } else {
            File files[] = wsdlOption.getDependencies();
            if (files != null) {
                for (int z = 0; z < files.length; ++z) {
                    if (files[z].lastModified() > doneFile.lastModified()) {
                        doWork = true;
                    }
                }
            }
        }

        if (doWork) {
            doneFile.delete();

            List<String> list = generateCommandLine(wsdlOption, outputDirFile, basedir, wsdlURI);

            getLog().debug("Calling wsdl2java with args: " + list);
            try {
                new WSDLToJava((String[])list.toArray(new String[list.size()])).run(new ToolContext());
            } catch (Throwable e) {
                getLog().debug(e);
                throw new MojoExecutionException(e.getMessage(), e);
            }
            try {
                doneFile.createNewFile();
            } catch (Throwable e) {
                getLog().warn("Could not create marker file " + doneFile.getAbsolutePath());
                getLog().debug(e);
            }
        }
    }

    private List<String> generateCommandLine(WsdlOption wsdlOption, File outputDirFile, URI basedir,
                                             URI wsdlURI) {
        List<String> list = new ArrayList<String>();
        if (wsdlOption.getPackagenames() != null) {
            Iterator<String> it = wsdlOption.getPackagenames().iterator();
            while (it.hasNext()) {
                list.add("-p");
                list.add(it.next());
            }
        }
        if (wsdlOption.getNamespaceExcludes() != null) {
            Iterator<String> it = wsdlOption.getNamespaceExcludes().iterator();
            while (it.hasNext()) {
                list.add("-nexclude");
                list.add(it.next());
            }
        }

        // -d specify the dir for generated source code
        list.add("-d");
        list.add(outputDirFile.toString());

        for (String binding : wsdlOption.getBindingFiles()) {
            File bindingFile = new File(binding);
            URI bindingURI;
            if (bindingFile.exists()) {
                bindingURI = bindingFile.toURI();
            } else {
                bindingURI = basedir.resolve(binding);
            }
            list.add("-b");
            list.add(bindingURI.toString());
        }
        if (wsdlOption.getFrontEnd() != null) {
            list.add("-fe");
            list.add(wsdlOption.getFrontEnd());
        }
        if (wsdlOption.getDataBinding() != null) {
            list.add("-db");
            list.add(wsdlOption.getDataBinding());
        }
        if (wsdlOption.getWsdlVersion() != null) {
            list.add("-wv");
            list.add(wsdlOption.getWsdlVersion());
        }
        if (wsdlOption.getCatalog() != null) {
            list.add("-catalog");
            list.add(wsdlOption.getCatalog());
        }
        if (wsdlOption.isExtendedSoapHeaders()) {
            list.add("-exsh");
            list.add("true");
        }
        if (wsdlOption.isAllowElementRefs()) {
            list.add("-allowElementRefs");
        }
        if (wsdlOption.isValidateWsdl()) {
            list.add("-validate");
        }
        if (wsdlOption.getDefaultExcludesNamespace() != null) {
            list.add("-dex");
            list.add(wsdlOption.getDefaultExcludesNamespace().toString());
        }
        if (wsdlOption.getDefaultNamespacePackageMapping() != null) {
            list.add("-dns");
            list.add(wsdlOption.getDefaultNamespacePackageMapping().toString());
        }
        if (wsdlOption.getServiceName() != null) {
            list.add("-sn");
            list.add(wsdlOption.getServiceName());
        }
        if (wsdlOption.isAutoNameResolution()) {
            list.add("-autoNameResolution");
        }
        if (wsdlOption.isNoAddressBinding()) {
            list.add("-noAddressBinding");
        }
        if (wsdlOption.getExtraargs() != null) {
            Iterator<String> it = wsdlOption.getExtraargs().iterator();
            while (it.hasNext()) {
                Object value = it.next();
                if (value == null) {
                    value = ""; // Maven makes empty tags into null
                    // instead of empty strings.
                }
                list.add(value.toString());
            }
        }
        if (wsdlOption.isSetWsdlLocation()) {
            list.add("-wsdlLocation");
            list.add(wsdlOption.getWsdlLocation() == null ? "" : wsdlOption.getWsdlLocation());
        }
        if (wsdlOption.isWsdlList()) {
            list.add("-wsdlList");
        }
        if (getLog().isDebugEnabled() && !list.contains("-verbose")) {
            list.add("-verbose");
        }
        list.add(wsdlURI.toString());
        return list;
    }

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

    private boolean isDefServiceName(WsdlOption wsdlOption) {
        List<String> args = wsdlOption.extraargs;
        if (args == null) {
            return false;
        }
        for (int i = 0; i < args.size(); i++) {
            if ("-sn".equalsIgnoreCase(args.get(i))) {
                return true;
            }
        }
        return false;

    }

}
