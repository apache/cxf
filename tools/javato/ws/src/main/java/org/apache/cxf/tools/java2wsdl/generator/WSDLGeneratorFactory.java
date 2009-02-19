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

package org.apache.cxf.tools.java2wsdl.generator;

import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.tools.common.ToolException;

public final class WSDLGeneratorFactory {
    private WSDLConstants.WSDLVersion wsdlVersion;
    
    public  WSDLGeneratorFactory() {
    }


    public void setWSDLVersion(WSDLConstants.WSDLVersion v) {
        this.wsdlVersion = v;
    }

    protected String getGeneratorClassName() {
        String pkgName = PackageUtils.getPackageName(getClass());
        return pkgName + "." + wsdlVersion.toString().toLowerCase() + "." + wsdlVersion + "Generator";
    }

    public AbstractGenerator newGenerator() {
        AbstractGenerator generator = null;
        String clzName = getGeneratorClassName();
        try {
            generator = (AbstractGenerator) Class.forName(clzName).newInstance();
        } catch (Exception e) {
            throw new ToolException("Can not find the Generator for: " + clzName, e);
        }
        return generator;
    }
}


