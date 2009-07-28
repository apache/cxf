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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.wsdlto.WSDLToJava;
import org.apache.maven.artifact.Artifact;
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
     * @parameter expression="${cxf.markerDirectory}" 
     *            default-value="${project.build.directory}/cxf-codegen-plugin-markers"
     */
    File markerDirectory;

    /**
     * Use the compile classpath rather than the test classpath for execution
     * useful if the test dependencies clash with those of wsdl2java
     * @parameter expression="${cxf.useCompileClasspath}" default-value="false"
     */
    boolean useCompileClasspath;
    
    /**
     * A list of wsdl files to include. Can contain ant-style wildcards and double wildcards.  
     * Defaults to *.wsdl
     * @parameter
     */
    String includes[];
    /**
     * A list of wsdl files to exclude. Can contain ant-style wildcards and double wildcards.  
     * @parameter
     */
    String excludes[];
                    
    private List<WsdlOption> getWsdlOptionsFromDir(final File root,
                                                   final File output)
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
    
    private void mergeOptions(List<WsdlOption> options) {
        File outputDirFile = testSourceRoot == null ? sourceRoot : testSourceRoot;
        for (WsdlOption o : wsdlOptions) {
            if (o.getOutputDir() == null) {
                o.setOutputDir(outputDirFile);
            }
            
            File file = new File(o.getWsdl());
            if (!file.exists()) {
                file = new File(project.getBasedir(), o.getWsdl());
            }
            if (file.exists()) {
                file = file.getAbsoluteFile();
                for (WsdlOption o2 : options) {
                    File file2 = null;
                    try {
                        URI uri = new URI(o2.getWsdl());
                        if (uri.isAbsolute()) {
                            file2 = new File(uri);
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                    if (file2 == null || !file2.exists()) {
                        file2 = new File(o2.getWsdl());
                    }
                    if (file2 == null || !file2.exists()) {
                        file2 = new File(project.getBasedir(), o2.getWsdl());
                    }
                    if (file2.exists() 
                        && file2.getAbsoluteFile().equals(file)) {
                        o.getExtraargs().addAll(0, o2.getExtraargs());
                        options.remove(o2);
                        break;
                    }
                }
            }
            options.add(0, o);
        }        
    }
    
    public void execute() throws MojoExecutionException {
        if (includes == null) {
            includes = new String[] {"*.wsdl"};
        } 
       
        File classesDir = new File(classesDirectory);
        classesDir.mkdirs();
        markerDirectory.mkdirs();
        
        List<WsdlOption> options = new ArrayList<WsdlOption>();
        if (wsdlRoot != null && wsdlRoot.exists()) {
            options.addAll(getWsdlOptionsFromDir(wsdlRoot, sourceRoot));
        }
        if (testWsdlRoot != null && testWsdlRoot.exists()) {
            options.addAll(getWsdlOptionsFromDir(testWsdlRoot, testSourceRoot));
        }

        if (wsdlOptions != null) {
            mergeOptions(options);
        }
        wsdlOptions = options.toArray(new WsdlOption[options.size()]);

        if (wsdlOptions.length == 0) {
            getLog().info("Nothing to generate");
            return;
        }

        List<URL> urlList = new ArrayList<URL>();
        StringBuffer buf = new StringBuffer();

        try {
            urlList.add(classesDir.toURI().toURL());
            if (!useCompileClasspath) {
                urlList.add(new File(project.getBuild().getOutputDirectory()).toURI().toURL());
            }
        } catch (MalformedURLException e) {
            //ignore
        }

        buf.append(classesDir.getAbsolutePath());
        buf.append(File.pathSeparatorChar);
        if (!useCompileClasspath) {
            buf.append(project.getBuild().getOutputDirectory());
            buf.append(File.pathSeparatorChar);
        }
        List artifacts = useCompileClasspath ? project.getCompileArtifacts() : project.getTestArtifacts();
        for (Artifact a : CastUtils.cast(artifacts, Artifact.class)) {
            try {
                if (a.getFile() != null
                    && a.getFile().exists()) {
                    urlList.add(a.getFile().toURI().toURL());
                    buf.append(a.getFile().getAbsolutePath());
                    buf.append(File.pathSeparatorChar);
                    //System.out.println("     " + a.getFile().getAbsolutePath());
                }
            } catch (MalformedURLException e) {
                //ignore
            }
        }
        
        ClassLoader origContext = Thread.currentThread().getContextClassLoader();
        URLClassLoader loader = new URLClassLoader(urlList.toArray(new URL[urlList.size()]),
                                                   origContext);
        String newCp = buf.toString();
        
        getLog().debug("Classpath: " + urlList.toString());

        //with some VM's, creating an XML parser (which we will do to parse wsdls)
        //will set some system properties that then interferes with mavens 
        //dependency resolution.  (OSX is the major culprit here)
        //We'll save the props and then set them back later.
        Map<Object, Object> origProps = new HashMap<Object, Object>(System.getProperties());
        
        String cp = System.getProperty("java.class.path");
        boolean result = true;
        
        try {
            Thread.currentThread().setContextClassLoader(loader);
            System.setProperty("java.class.path", newCp);
            for (WsdlOption o : wsdlOptions) {
                processWsdl(o);

                File dirs[] = o.getDeleteDirs();
                if (dirs != null) {
                    for (int idx = 0; idx < dirs.length; ++idx) {
                        result = result && deleteDir(dirs[idx]);
                    }
                }                
            }
        } finally {
            //cleanup as much as we can.
            Bus bus = BusFactory.getDefaultBus(false);
            if (bus != null) {
                bus.shutdown(true);
            }
            Thread.currentThread().setContextClassLoader(origContext);
            System.setProperty("java.class.path", cp);
            
            Map<Object, Object> newProps = new HashMap<Object, Object>(System.getProperties());
            for (Object o : newProps.keySet()) {
                if (!origProps.containsKey(o)) {
                    System.clearProperty(o.toString());
                }
            }
            System.getProperties().putAll(origProps);
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
        
        doneFileName = doneFileName.replace('?', '_')
            .replace('&', '_').replace('/', '_').replace('\\', '_').replace(':', '_'); 
        
        File doneFile =
            new File(markerDirectory, "." + doneFileName + ".DONE");
        
        long timestamp = 0;
        if ("file".equals(wsdlURI.getScheme())) {
            timestamp = new File(wsdlURI).lastModified();
        } else {
            try {
                timestamp = wsdlURI.toURL().openConnection().getDate();
            } catch (Exception e) {
                //ignore
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
            Iterator it = wsdlOption.getPackagenames().iterator();
            while (it.hasNext()) {
                list.add("-p");
                list.add(it.next().toString());
            }
        }
        if (wsdlOption.getNamespaceExcludes() != null) {
            Iterator it = wsdlOption.getNamespaceExcludes().iterator();
            while (it.hasNext()) {
                list.add("-nexclude");
                list.add(it.next().toString());
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
        if (wsdlOption.getServiceName()  != null) {
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
            Iterator it = wsdlOption.getExtraargs().iterator();
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
        List args = wsdlOption.extraargs;
        if (args == null) {
            return false;
        }
        for (int i = 0; i < args.size(); i++) {
            if ("-sn".equalsIgnoreCase((String)args.get(i))) {
                return true;
            }
        }
        return false;

    }

}
