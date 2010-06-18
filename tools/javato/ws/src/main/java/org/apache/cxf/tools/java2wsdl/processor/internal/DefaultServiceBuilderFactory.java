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

package org.apache.cxf.tools.java2wsdl.processor.internal;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.frontend.AbstractServiceFactory;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxwsServiceBuilder;
import org.apache.cxf.service.ServiceBuilder;
import org.apache.cxf.simple.SimpleServiceBuilder;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.java2wsdl.processor.FrontendFactory;
import org.apache.cxf.tools.java2wsdl.processor.FrontendFactory.Style;

/**
 * This class constructs ServiceBuilder objects. These objects are used to access the services and the data
 * bindings to generate the wsdl.
 */
public final class DefaultServiceBuilderFactory extends ServiceBuilderFactory {

    @Override
    public ServiceBuilder newBuilder(FrontendFactory.Style s) {
        DataBinding dataBinding;
        final String dbn = getDatabindingName();
        if (ToolConstants.JAXB_DATABINDING.equals(dbn)) {
            dataBinding = new JAXBDataBinding();
        } else if (ToolConstants.AEGIS_DATABINDING.equals(dbn)) {
            dataBinding = new AegisDatabinding();
        } else {
            throw new ToolException("Unsupported databinding: " + s);
        }
        AbstractServiceFactory builder = null;
        if (Style.Jaxws.equals(s)) {
            builder = new JaxwsServiceBuilder();
        } else if (Style.Simple.equals(s)) {
            builder = new SimpleServiceBuilder();
        } else {
            throw new ToolException("Unsupported frontend style: " + s);
        }
        builder.setDataBinding(dataBinding);
        builder.setServiceClass(serviceClass);
        return builder;
    }
}
