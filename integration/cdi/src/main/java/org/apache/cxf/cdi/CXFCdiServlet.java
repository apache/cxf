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
package org.apache.cxf.cdi;

import java.util.ArrayList;
import java.util.ServiceLoader;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;

/**
 * Apache CXF servlet with CDI 1.1 integration support 
 */
public class CXFCdiServlet extends CXFNonSpringServlet {
    private static final long serialVersionUID = -2890970731778523861L;
    
    @Override
    @SuppressWarnings("rawtypes")
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);        
        
        final BeanManager beanManager = CDI.current().getBeanManager();
        final JAXRSCdiResourceExtension extension = beanManager.getExtension(JAXRSCdiResourceExtension.class);
        if (extension != null) {    
            for (final Application application: extension.getApplications()) {
                final JAXRSServerFactoryBean bean = ResourceUtils.createApplication(application, false, false);
                
                bean.setServiceBeans(new ArrayList< Object >(extension.getServices()));
                bean.setProviders(extension.getProviders());
                bean.setBus(getBus());
                
                final ServiceLoader< MessageBodyWriter > writers = ServiceLoader.load(MessageBodyWriter.class);
                for (final MessageBodyWriter< ? > writer: writers) {
                    bean.setProvider(writer);
                }
                
                final ServiceLoader< MessageBodyReader > readers = ServiceLoader.load(MessageBodyReader.class);
                for (final MessageBodyReader< ? > reader: readers) {
                    bean.setProvider(reader);
                }
                
                bean.create();
            }        
        }
    }
}
