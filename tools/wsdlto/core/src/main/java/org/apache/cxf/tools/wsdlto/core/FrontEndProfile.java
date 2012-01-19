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

package org.apache.cxf.tools.wsdlto.core;

import java.util.ArrayList;
import java.util.List;
import org.apache.cxf.tools.common.FrontEndGenerator;
import org.apache.cxf.tools.common.Processor;
import org.apache.cxf.tools.common.toolspec.ToolContainer;

public class FrontEndProfile {
    List<FrontEndGenerator> generators = new ArrayList<FrontEndGenerator>();
    Processor processor;
    Class<? extends ToolContainer> containerClz;
    String toolspec;
    AbstractWSDLBuilder<? extends Object> builder;

    public void setContainerClass(Class<? extends ToolContainer> c) {
        this.containerClz = c;
    }

    public Class<? extends ToolContainer> getContainerClass() {
        return this.containerClz;
    }

    public void setToolspec(String ts) {
        this.toolspec = ts;
    }

    public String getToolspec() {
        return this.toolspec;
    }
    
    public List<FrontEndGenerator> getGenerators() {
        return generators;
    }
    
    public void registerGenerator(FrontEndGenerator generator) {
        generators.add(generator);
    }

    public Processor getProcessor() {
        return this.processor;
    }

    public void setProcessor(Processor prs) {
        this.processor = prs;
    }

    public void setWSDLBuilder(AbstractWSDLBuilder<? extends Object> b) {
        this.builder = b;
    }

    public AbstractWSDLBuilder getWSDLBuilder() {
        return builder;
    }

}
