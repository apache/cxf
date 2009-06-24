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
package org.apache.cxf.transport.servlet;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;



public abstract class AbstractCXFServlet extends HttpServlet {
    
    static final Map<String, WeakReference<Bus>> BUS_MAP = new Hashtable<String, WeakReference<Bus>>();
    static final Logger LOG = getLogger();
    
    /**
     * List of well-known HTTP 1.1 verbs, with POST and GET being the most used verbs at the top 
     */
    private static final List<String> KNOWN_HTTP_VERBS = 
        Arrays.asList(new String[]{"POST", "GET", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE"});
    
    protected Bus bus;
    protected ServletTransportFactory servletTransportFactory;
    protected ServletController controller;
    
    public static Logger getLogger() {
        return LogUtils.getL7dLogger(AbstractCXFServlet.class);
    }
    
    public ServletController createServletController(ServletConfig servletConfig) {
        ServletController newController =
            new ServletController(servletTransportFactory,
                                  servletConfig,
                                  this.getServletContext(), 
                                  bus);
        
        return newController;
    }
    
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        try {
            BusFactory.setThreadDefaultBus(null);
    
            String busid = servletConfig.getInitParameter("bus.id");
            if (null != busid) {
                WeakReference<Bus> ref = BUS_MAP.get(busid);
                if (null != ref) {
                    bus = ref.get();
                    BusFactory.setThreadDefaultBus(bus);
                }
            }
            
            loadBus(servletConfig);
                
            if (null != busid) {
                BUS_MAP.put(busid, new WeakReference<Bus>(bus));
            }
        } finally {
            BusFactory.setThreadDefaultBus(null);
        }
    }
    
    public abstract void loadBus(ServletConfig servletConfig) throws ServletException;    
   
    protected DestinationFactory createServletTransportFactory() {
        if (servletTransportFactory == null) {
            servletTransportFactory = new ServletTransportFactory(bus);
        }
        return servletTransportFactory;
    }

    private void registerTransport(DestinationFactory factory, String namespace) {
        bus.getExtension(DestinationFactoryManager.class).registerDestinationFactory(namespace, factory);
    }

    protected void replaceDestinationFactory() {
       
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class); 
        try {
            DestinationFactory df = dfm
                .getDestinationFactory("http://cxf.apache.org/transports/http/configuration");
            if (df instanceof ServletTransportFactory) {
                servletTransportFactory = (ServletTransportFactory)df;
                LOG.info("DESTIONFACTORY_ALREADY_REGISTERED");
                return;
            }
        } catch (BusException e) {
            // why are we throwing a busexception if the DF isn't found?
        }

        
        DestinationFactory factory = createServletTransportFactory();

        for (String s : factory.getTransportIds()) {
            registerTransport(factory, s);
        }
        LOG.info("REPLACED_HTTP_DESTIONFACTORY");
    }

    public ServletController getController() {
        return controller;
    }
    
    public Bus getBus() {
        return bus;
    }
    
    public void destroy() {
        String s = bus.getId();
        BUS_MAP.remove(s);
        bus.shutdown(true);
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException {
        invoke(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException {
        invoke(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        invoke(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
        invoke(request, response);
    }
    
    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
        invoke(request, response);
    }
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
        invoke(request, response);
    }
    
    
    
    /**
     * {@inheritDoc}
     * 
     * javax.http.servlet.HttpServlet does not let to override the code which deals with
     * unrecognized HTTP verbs such as PATCH (being standardized), WebDav ones, etc.
     * Thus we let CXF servlets process unrecognized HTTP verbs directly, otherwise we delegate
     * to HttpService  
     */
    @Override
    public void service(ServletRequest req, ServletResponse res)
        throws ServletException, IOException {
        
        HttpServletRequest      request;
        HttpServletResponse     response;
        
        try {
            request = (HttpServletRequest) req;
            response = (HttpServletResponse) res;
        } catch (ClassCastException e) {
            throw new ServletException("Unrecognized HTTP request or response object");
        }
        
        String method = request.getMethod();
        if (KNOWN_HTTP_VERBS.contains(method)) {
            super.service(request, response);
        } else {
            invoke(request, response);
        }
    }
    
    protected void invoke(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            BusFactory.setThreadDefaultBus(getBus());
            controller.invoke(request, response);
        } finally {
            BusFactory.setThreadDefaultBus(null);
        }
    }

}
