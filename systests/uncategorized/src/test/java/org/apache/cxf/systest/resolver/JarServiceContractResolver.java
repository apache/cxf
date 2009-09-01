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

package org.apache.cxf.systest.resolver;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.ServiceContractResolver;

public class JarServiceContractResolver implements ServiceContractResolver {

    private URI uri;
    
    public URI getContractLocation(QName qname) {
        URI ns = null;
        try {
            ns = new URI(qname.getNamespaceURI()).normalize();
        } catch (URISyntaxException e2) {
            return null;
        }

        String[] hostStrings = ns.getHost().split("\\.");
        String[] pathStrings = ns.getPath().split("\\/");
        String local = qname.getLocalPart();
        StringBuffer path = new StringBuffer();
        for (int i = hostStrings.length - 1; i >= 0; i--) {
            path.append(hostStrings[i]).append("/");
        }
        for (int i = 1; i < pathStrings.length; i++) {
            path.append(pathStrings[i]).append("/");
        }
        path.append(local).append(".wsdl");
        try {
            URL jarURL = this.getClass().getResource("/wsdl_systest/hello_world_resolver.jar");
            JarFile jar = new JarFile(new File(jarURL.toURI()));
            JarEntry jarEntry = jar.getJarEntry(path.toString());
            uri = new URI("jar:" + jarURL + "!/" + jarEntry.toString());
            return uri;
        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }


}
