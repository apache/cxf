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
import java.util.List;

/**
 * The abstract mojo processes in terms of a list of these items.
 */
public interface GenericWsdlOption {
    WsdlArtifact getArtifact();
    void setArtifact(WsdlArtifact artifact);
    /**
     * Return a string pointing to the WSDL. Might be a plain path, might be a full URI.
     * @return
     */
    String getUri();
    void setUri(String uri);
    File getOutputDir();
    void setOutputDir(File outputDir);
    File[] getDeleteDirs();
    List<String> generateCommandLine(File outputDirFile, URI basedir, URI wsdlURI, boolean debug);
}
