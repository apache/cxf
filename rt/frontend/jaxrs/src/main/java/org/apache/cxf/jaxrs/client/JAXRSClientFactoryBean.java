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
package org.apache.cxf.jaxrs.client;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.util.ProxyHelper;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.UpfrontConduitSelector;
import org.apache.cxf.jaxrs.AbstractJAXRSFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.service.Service;

public class JAXRSClientFactoryBean extends AbstractJAXRSFactoryBean {
    
    private boolean inheritHeaders; 
    private MultivaluedMap<String, String> headers;
    
    public JAXRSClientFactoryBean() {
        this(new JAXRSServiceFactoryBean());
    }
    
    public JAXRSClientFactoryBean(JAXRSServiceFactoryBean serviceFactory) {
        super(serviceFactory);
        serviceFactory.setEnableStaticResolution(true);
        
    }
    
    public void setInheritHeaders(boolean ih) {
        inheritHeaders = ih;
    }
    
    public void setResourceClass(Class cls) {
        setServiceClass(cls);
    }
    
    public void setServiceClass(Class cls) {
        serviceFactory.setResourceClass(cls);
    }
    
    public void setHeaders(Map<String, String> map) {
        headers = new MetadataMap<String, String>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String[] values = entry.getValue().split(",");
            for (String v : values) {
                if (v.length() != 0) {
                    headers.add(entry.getKey(), v);
                }
            }
        }
    }
    
    public WebClient createWebClient() {
        
        Service service = new JAXRSServiceImpl(getAddress());
        getServiceFactory().setService(service);
        
        try {
            Endpoint ep = createEndpoint();
            WebClient client = new WebClient(getAddress());
            initClient(client, ep);
            
            return client;
        } catch (Exception ex) {
            throw new WebApplicationException();
        }
    }
    
    public <T> T create(Class<T> cls) {
        return cls.cast(create());
    }
    
    public Client create() {
        List<ClassResourceInfo> list = serviceFactory.getClassResourceInfo();
        if (list.isEmpty()) {
            throw new WebApplicationException();
        }
        
        try {
            Endpoint ep = createEndpoint();
            URI baseURI = URI.create(getAddress());
            ClassResourceInfo cri = list.get(0);
            
            ClientProxyImpl proxyImpl = new ClientProxyImpl(baseURI, baseURI, cri, inheritHeaders);
            initClient(proxyImpl, ep);    
            
            return (Client)ProxyHelper.getProxy(cri.getServiceClass().getClassLoader(),
                                        new Class[]{cri.getServiceClass(), Client.class}, 
                                        proxyImpl);
        } catch (Exception ex) {
            throw new WebApplicationException();
        }
        
        
    }
    
    protected ConduitSelector getConduitSelector(Endpoint ep) {
        ConduitSelector cs = getConduitSelector();
        cs = cs == null ? new UpfrontConduitSelector() : cs;
        cs.setEndpoint(ep);
        return cs;
    }
    
    protected void initClient(AbstractClient client, Endpoint ep) {
        client.setConduitSelector(getConduitSelector(ep));
        client.setBus(getBus());
        client.setOutInterceptors(getOutInterceptors());
        client.setInInterceptors(getInInterceptors());
        if (headers != null) {
            client.headers(headers);
        }
        if (entityProviders != null) {
            ProviderFactory.getInstance().setUserProviders(entityProviders); 
        }
        if (schemaLocations != null) {
            ProviderFactory.getInstance().setSchemaLocations(schemaLocations);
        }
    }
} 
