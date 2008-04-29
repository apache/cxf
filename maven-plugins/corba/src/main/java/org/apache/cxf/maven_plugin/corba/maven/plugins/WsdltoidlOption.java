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

package org.apache.cxf.maven_plugin.corba.maven.plugins;

import java.util.List;

public class WsdltoidlOption {

    String wsdl;
    boolean corbabinding;
    boolean idl;

    List<String> extraargs;
    
    public String getWSDL() {
        return wsdl;
    }

    public void setWSDL(String wsdlFile) {
        wsdl = wsdlFile;
    }

    public boolean isCorbaEnabled() {
        return corbabinding;
    }

    public void setCorbabinding(boolean flag) {
        corbabinding = flag;
    }

    public boolean isIdlEnabled() {
        return idl;
    }

    public void setIdl(boolean flag) {
        idl = flag;
    }

    public List<String> getExtraargs() {
        return extraargs;
    }

    public void setExtraargs(List<String> args) {
        extraargs = args;
    }
}
