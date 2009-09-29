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


public class WsdlOption extends Option {

    /**
     * The WSDL file to process.
     */
    String wsdl;
    
    /**
     * Alternatively to the wsdl string an artifact can be specified
     */
    WsdlArtifact wsdlArtifact;

    public String getWsdl() {
        return wsdl;
    }

    public void setWsdl(String w) {
        wsdl = w;
    }

    public WsdlArtifact getWsdlArtifact() {
        return wsdlArtifact;
    }

    public void setWsdlArtifact(WsdlArtifact wsdlArtifact) {
        this.wsdlArtifact = wsdlArtifact;
    }
    
    public int hashCode() {
        if (wsdl != null) {
            return wsdl.hashCode();
        }
        return -1;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof WsdlOption)) {
            return false;
        }
        
        WsdlOption t = (WsdlOption) obj;
        return t.getWsdl().equals(getWsdl());
    }
    
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WSDL: ").append(wsdl).append('\n');
        builder.append("OutputDir: ").append(outputDir).append('\n');
        builder.append("Extraargs: ").append(extraargs).append('\n');
        builder.append("Packagenames: ").append(packagenames).append('\n');
        builder.append('\n');
        return builder.toString();
    }
}
