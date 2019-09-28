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
import java.util.Collections;
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
        if (arr == null || arr.length == 0) {
            return "";
        }
        return String.join(",", arr);
    }

    public static List<File> getWsdlFiles(File dir, String[] includes, String[] excludes)
        throws MojoExecutionException {

        List<String> exList = new ArrayList<>();
        if (excludes != null) {
            Collections.addAll(exList, excludes);
        }
        Collections.addAll(exList, org.codehaus.plexus.util.FileUtils.getDefaultExcludes());

        String inc = joinWithComma(includes);
        String ex = joinWithComma(exList.toArray(new String[0]));

        try {
            List<?> newfiles = org.codehaus.plexus.util.FileUtils.getFiles(dir, inc, ex);
            return CastUtils.cast(newfiles);
        } catch (IOException exc) {
            throw new MojoExecutionException(exc.getMessage(), exc);
        }
    }

}
