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

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;

public final class WsdlOptionLoader {
    private static final String WSDL_OPTIONS = "-options$";
    private static final String WSDL_BINDINGS = "-binding-?\\d*.xml$";

    
    private String getIncludeExcludeString(String[] arr) {
        if (arr == null) {
            return "";
        }
        StringBuilder str = new StringBuilder();

        if (arr != null) {
            for (String s : arr) {
                if (str.length() > 0) {
                    str.append(',');
                }
                str.append(s);
            }
        }
        return str.toString();
    }
    
    public List<WsdlOption> load(String wsdlRoot) throws MojoExecutionException {
        return load(new File(wsdlRoot), new String[] {"*.wsdl"}, null, null);
    }

    public List<WsdlOption> load(File wsdlBasedir,
                                 String includes[],
                                 String excludes[],
                                 Option defaultOptions)
        throws MojoExecutionException {
        
        if (wsdlBasedir == null) {
            return new ArrayList<WsdlOption>();
        }

        if (!wsdlBasedir.exists()) {
            throw new MojoExecutionException(wsdlBasedir + " does not exist");
        }

        return findJobs(wsdlBasedir, getWsdlFiles(wsdlBasedir, includes, excludes), defaultOptions);
    }

    private List<File> getWsdlFiles(File dir, String includes[], String excludes[])
        throws MojoExecutionException {
        
        List<String> exList = new ArrayList<String>();
        if (excludes != null) {
            exList.addAll(Arrays.asList(excludes));
        }
        exList.addAll(Arrays.asList(org.codehaus.plexus.util.FileUtils.getDefaultExcludes()));
        
        String inc = getIncludeExcludeString(includes);
        String ex = getIncludeExcludeString(exList.toArray(new String[exList.size()]));
        
        try {
            List newfiles = org.codehaus.plexus.util.FileUtils.getFiles(dir, inc, ex);
            return CastUtils.cast(newfiles);
        } catch (IOException exc) {
            throw new MojoExecutionException(exc.getMessage(), exc);
        }
    }

    private File getOptions(File dir, String pattern) {
        List<File> files = FileUtils.getFiles(dir, pattern);
        if (files.size() > 0) {
            return files.iterator().next();
        }
        return null;
    }

    private List<File> getBindingFiles(File dir, String pattern) {
        return FileUtils.getFiles(dir, pattern);
    }

    protected List<WsdlOption> findJobs(File dir, List<File> wsdlFiles, Option defaultOptions) {
        List<WsdlOption> jobs = new ArrayList<WsdlOption>();

        for (File wsdl : wsdlFiles) {
            if (wsdl == null || !wsdl.exists()) {
                continue;
            }

            String wsdlName = wsdl.getName();
            int idx = wsdlName.toLowerCase().lastIndexOf(".wsdl");
            if (idx == -1) {
                idx = wsdlName.lastIndexOf('.');
            }
            if (idx != -1) {
                wsdlName = wsdlName.substring(0, idx);
                File options = getOptions(dir, wsdlName + WSDL_OPTIONS);
                List<File> bindings = getBindingFiles(dir, wsdlName + WSDL_BINDINGS);
    
                jobs.add(generateWsdlOption(wsdl, bindings, options, defaultOptions));
            }
        }
        return jobs;
    }

    protected WsdlOption generateWsdlOption(final File wsdl, 
                                            final List<File> bindingFiles, 
                                            final File options,
                                            final Option defaultOptions) {
        WsdlOption wsdlOption = new WsdlOption();

        if (options != null && options.exists()) {
            try {
                List<String> lines = FileUtils.readLines(options);
                if (lines.size() > 0) {
                    wsdlOption.getExtraargs().addAll(Arrays.asList(lines.iterator().next().split(" ")));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (defaultOptions != null) {
            // no options specified use the defaults
            defaultOptions.copyOptions(wsdlOption);
        }
        
        if (bindingFiles != null) {
            for (File binding : bindingFiles) {
                wsdlOption.addBindingFile(binding);
            }
        }
        wsdlOption.setWsdl(wsdl.toURI().toString());
        
        return wsdlOption;
    }
}