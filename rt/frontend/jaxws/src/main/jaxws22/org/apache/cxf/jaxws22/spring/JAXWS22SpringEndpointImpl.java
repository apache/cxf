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
package org.apache.cxf.jaxws22.spring;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.BusWiringBeanFactoryPostProcessor;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@NoJSR250Annotations
public class JAXWS22SpringEndpointImpl extends org.apache.cxf.jaxws22.EndpointImpl
    implements ApplicationContextAware {

    boolean checkBlockConstruct;

    public JAXWS22SpringEndpointImpl(Object o) {
        super(o instanceof Bus ? (Bus)o : null,
              o instanceof Bus ? null : o);
    }

    public JAXWS22SpringEndpointImpl(Bus bus, Object implementor) {
        super(bus, implementor);
    }
    
    public void setCheckBlockConstruct(Boolean b) {
        checkBlockConstruct = b;
    }
    
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        if (checkBlockConstruct) {
            try {
                Class<?> cls = Class
                    .forName("org.springframework.context.annotation.CommonAnnotationBeanPostProcessor");
                if (ctx.getBeanNamesForType(cls, true, false).length != 0) {
                    //Spring will handle the postconstruct, but won't inject the 
                    // WebServiceContext so we do need to do that.
                    super.getServerFactory().setBlockPostConstruct(true);
                } else {
                    super.getServerFactory().setBlockInjection(true);
                }
            } catch (ClassNotFoundException e) {
                //ignore
            }
        }
        if (getBus() == null) {
            setBus(BusWiringBeanFactoryPostProcessor.addDefaultBus(ctx));
        }
    }
}