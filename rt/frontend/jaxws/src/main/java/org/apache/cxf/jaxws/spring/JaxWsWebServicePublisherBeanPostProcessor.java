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

package org.apache.cxf.jaxws.spring;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;
import org.springframework.web.servlet.mvc.Controller;

// All tests for this are in systests, since there's no place else to assemble all the necessary dependencies.

/**
 * Bean to scan context for potential web services. This scans the beans for classes that
 * are annotated with @WebService. Excepting those already declared via the JAX-WS Spring
 * schema, it launches each as an endpoint.
 * 
 * By default, it sets up a default JaxWsServiceFactory and JAX-B data binding,
 * and then creates a URL under /services/ based on the service name. Properties of the bean
 * permit you to configure this; if you set prototypeServiceFactoryBeanName, the code
 * will fetch that bean. It must be a prototype, since service factory object can't be used
 * for more than one endpoint. Similarly, prototypeDataBindingBeanName can be used to 
 * control the data binding.
 * 
 * Note that this class uses {@link org.apache.cxf.transport.servlet#CXFServlet} from the 
 * cxf-rt-transports-http-jetty library, which is not part of 
 * the standard dependencies of the JAX-WS front
 * end.
 * 
 * If you use this processor in an environment with no servlet, it will still launch the
 * endpoints using the embedded CXF server.
 * 
 */
public class JaxWsWebServicePublisherBeanPostProcessor 
             extends AbstractUrlHandlerMapping implements BeanPostProcessor,
    ServletConfigAware, BeanFactoryAware {
    
    private static final Logger LOG = LogUtils.getL7dLogger(JaxWsWebServicePublisherBeanPostProcessor.class);
    
    private static final String CXF_SERVLET_CLASS_NAME = "org.apache.cxf.transport.servlet.CXFServlet";
    private Class<?> servletClass;
    private Method servletGetBusMethod;

    private String urlPrefix = "/services/";
    private Servlet shadowCxfServlet;
    private String prototypeDataBindingBeanName;
    private String prototypeServerFactoryBeanName;
    private BeanFactory beanFactory;
    // for testing
    private boolean customizedServerFactory;
    private boolean customizedDataBinding;
    
    public JaxWsWebServicePublisherBeanPostProcessor() throws SecurityException, 
           NoSuchMethodException, ClassNotFoundException {
        try {
            servletClass = ClassLoaderUtils.loadClass(CXF_SERVLET_CLASS_NAME, getClass());
        } catch (ClassNotFoundException e) {
            Message message = new Message("SERVLET_CLASS_MISSING", LOG, CXF_SERVLET_CLASS_NAME);
            LOG.severe(message.toString());
            throw e;
        }
        servletGetBusMethod = servletClass.getMethod("getBus");
    }
    
    private Bus getServletBus() {
        try {
            if (shadowCxfServlet == null) {
                // no servlet going on. Just launch.
                return BusFactory.getDefaultBus(true);
            }
            return (Bus) servletGetBusMethod.invoke(shadowCxfServlet);
        } catch (Exception e) {
            // CXF internally inconsistent? 
            throw new RuntimeException(e);
        }
    }

  
    /**
     * Set the prefix for the generated endpoint URLs.
     * @param urlPrefix
     */
    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
    
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = ClassHelper.getRealClass(bean);

        if (clazz.isAnnotationPresent(WebService.class)) {
            WebService ws = (WebService)clazz.getAnnotation(WebService.class);
            String url = urlPrefix + ws.serviceName();
            Message message = new Message("SELECTED_SERVICE", LOG, beanName,
                                          clazz.getName(),
                                          url);
            LOG.info(message.toString());

            createAndPublishEndpoint(url, bean);
            registerHandler(url, new ServletAdapter(shadowCxfServlet));
        } else {
            if (logger.isDebugEnabled()) {
                Message message = new Message("REJECTED_NO_ANNOTATION", LOG, beanName,
                                              clazz.getName());
                LOG.fine(message.toString());
            }
        }

        return bean;
    }
    
    private void createAndPublishEndpoint(String url, Object implementor) {
        ServerFactoryBean serverFactory = null;
        if (prototypeServerFactoryBeanName != null) {
            if (!beanFactory.isPrototype(prototypeServerFactoryBeanName)) {
                throw 
                    new IllegalArgumentException(
                        "prototypeServerFactoryBeanName must indicate a scope='prototype' bean");
            }
            serverFactory = (ServerFactoryBean)
                             beanFactory.getBean(prototypeServerFactoryBeanName, 
                                                 ServerFactoryBean.class);
            customizedServerFactory = true;
        } else {
            serverFactory = new JaxWsServerFactoryBean();
        }

        serverFactory.setServiceBean(implementor);
        serverFactory.setServiceClass(ClassHelper.getRealClass(implementor));
        serverFactory.setAddress(url);
        
        DataBinding dataBinding = null;
        if (prototypeDataBindingBeanName != null) {
            if (!beanFactory.isPrototype(prototypeDataBindingBeanName)) {
                throw 
                    new IllegalArgumentException(
                        "prototypeDataBindingBeanName must indicate a scope='prototype' bean");
            }
            customizedDataBinding = true;
            dataBinding = (DataBinding)
                             beanFactory.getBean(prototypeDataBindingBeanName, 
                                                 DataBinding.class); 
        } else {
            dataBinding = new JAXBDataBinding();
        }
        
        serverFactory.setDataBinding(dataBinding);
        serverFactory.setBus(getServletBus());
        serverFactory.create();
    }

    public void setServletConfig(ServletConfig servletConfig) {
        try {
            shadowCxfServlet = (Servlet)servletClass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        try {
            shadowCxfServlet.init(servletConfig);
        } catch (ServletException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public static class ServletAdapter implements Controller {

        private Servlet controller;

        public ServletAdapter(Servlet controller) {
            this.controller = controller;
        }

        public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
            controller.service(request, response);
            return null;
        }
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory; 
        
    }
    
    public String getPrototypeServerFactoryBeanName() {
        return prototypeServerFactoryBeanName;
    }

    /**
     * Set the server factory for all services launched by this bean. This must be the name of a 
     * scope='prototype' bean that implements 
     * {@link org.apache.cxf.frontend#ServerFactoryBean}.
     * @param prototypeServerFactoryBeanName
     */
    public void setPrototypeServerFactoryBeanName(String prototypeServerFactoryBeanName) {
        this.prototypeServerFactoryBeanName = prototypeServerFactoryBeanName;
    }
    
    public String getPrototypeDataBindingBeanName() {
        return prototypeDataBindingBeanName;
    }

    /**
     * Set the data binding for all services launched by this bean. This must be the name of a 
     * scope='prototype' bean that implements {@link org.apache.cxf.databinding#DataBinding}.
     * @param prototypeDataBindingBeanName
     */
    public void setPrototypeDataBindingBeanName(String prototypeDataBindingBeanName) {
        this.prototypeDataBindingBeanName = prototypeDataBindingBeanName;
    }
    
    /**
     * For Unit Test.
     * @return
     */
    public boolean isCustomizedServerFactory() {
        return customizedServerFactory;
    }

    /**
     * For Unit Test.
     * @return
     */
    public boolean isCustomizedDataBinding() {
        return customizedDataBinding;
    }
}
