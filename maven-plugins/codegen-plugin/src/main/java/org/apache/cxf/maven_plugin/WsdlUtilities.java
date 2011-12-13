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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

public final class WsdlUtilities {

    public static final String WSDL_TYPE = "wsdl";

    private WsdlUtilities() {
    }
    
    public static boolean fillWsdlOptionFromArtifact(GenericWsdlOption option, 
                                                        Artifact artifact, 
                                                        File outputDir) {
        if (!WSDL_TYPE.equals(artifact.getType())) {
            return false;
        }
        WsdlArtifact wsdlArtifact = new WsdlArtifact();
        wsdlArtifact.setArtifactId(artifact.getArtifactId());
        wsdlArtifact.setGroupId(artifact.getGroupId());
        wsdlArtifact.setType(artifact.getType());
        wsdlArtifact.setVersion(artifact.getVersion());
        option.setArtifact(wsdlArtifact);
        option.setOutputDir(outputDir);
        return true;
    }

    public static String joinWithComma(String[] arr) {
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

    public static List<File> getWsdlFiles(File dir, String includes[], String excludes[])
        throws MojoExecutionException {

        List<String> exList = new ArrayList<String>();
        if (excludes != null) {
            exList.addAll(Arrays.asList(excludes));
        }
        exList.addAll(Arrays.asList(org.codehaus.plexus.util.FileUtils.getDefaultExcludes()));

        String inc = joinWithComma(includes);
        String ex = joinWithComma(exList.toArray(new String[exList.size()]));

        try {
            List newfiles = org.codehaus.plexus.util.FileUtils.getFiles(dir, inc, ex);
            return CastUtils.cast(newfiles);
        } catch (IOException exc) {
            throw new MojoExecutionException(exc.getMessage(), exc);
        }
    }

}
