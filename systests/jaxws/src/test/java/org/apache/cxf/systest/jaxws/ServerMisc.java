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

package org.apache.cxf.systest.jaxws;

import javax.xml.ws.Endpoint;

import org.apache.cxf.anonymous_complex_type.AnonymousComplexTypeImpl;
import org.apache.cxf.jaxb_element_test.JaxbElementTestImpl;
import org.apache.cxf.jaxws.JAXWSMethodInvoker;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.ordered_param_holder.OrderedParamHolderImpl;
import org.apache.cxf.service.factory.AbstractServiceConfiguration;
import org.apache.cxf.service.invoker.Factory;
import org.apache.cxf.service.invoker.PerRequestFactory;
import org.apache.cxf.service.invoker.PooledFactory;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;


public class ServerMisc extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(ServerMisc.class);

    
    public static final String DOCLIT_CODEFIRST_URL = 
        "http://localhost:" + PORT + "/DocLitWrappedCodeFirstService/";
    public static final String RPCLIT_CODEFIRST_URL = 
        "http://localhost:" + PORT + "/RpcLitCodeFirstService/";
    public static final String DOCLIT_CODEFIRST_BASE_URL =
        "http://localhost:" + PORT + "/DocLitWrappedCodeFirstServiceBaseService/";
    public static final String DOCLITBARE_CODEFIRST_URL = 
        "http://localhost:" + PORT + "/DocLitBareCodeFirstService/";
    public static final String DOCLIT_CODEFIRST_SETTINGS_URL = 
        "http://localhost:" + PORT + "/DocLitWrappedCodeFirstServiceSettings/";
    
    protected void run() {
        
        Factory factory = new PerRequestFactory(DocLitWrappedCodeFirstServiceImpl.class);
        factory = new PooledFactory(factory, 4);
        
        JAXWSMethodInvoker invoker = new JAXWSMethodInvoker(factory);
        JaxWsServerFactoryBean factoryBean;
        
        factoryBean = new JaxWsServerFactoryBean();
        factoryBean.setAddress(DOCLIT_CODEFIRST_URL);
        factoryBean.setServiceClass(DocLitWrappedCodeFirstServiceImpl.class);
        factoryBean.setInvoker(invoker);
        factoryBean.create();
        
        factoryBean = new JaxWsServerFactoryBean();
        factoryBean.setAddress(DOCLIT_CODEFIRST_SETTINGS_URL);
        factoryBean.setServiceClass(DocLitWrappedCodeFirstServiceImpl.class);
        factoryBean.setInvoker(invoker);
        factoryBean.getServiceFactory().setAnonymousWrapperTypes(true);
        factoryBean.getServiceFactory().getServiceConfigurations().add(0, new AbstractServiceConfiguration() {
            public Boolean isWrapperPartNillable(MessagePartInfo mpi) {
                return Boolean.TRUE;
            }
            public Long getWrapperPartMinOccurs(MessagePartInfo mpi) {
                return Long.valueOf(1L);
            }
        });
        factoryBean.create();
         
        //Object implementor4 = new DocLitWrappedCodeFirstServiceImpl();
        //Endpoint.publish(DOCLIT_CODEFIRST_URL, implementor4);
        
        Object implementor7 = new DocLitBareCodeFirstServiceImpl();
        Endpoint.publish(DOCLITBARE_CODEFIRST_URL, implementor7);
        
        Object implementor6 = new InterfaceInheritTestImpl();
        Endpoint.publish(DOCLIT_CODEFIRST_BASE_URL, implementor6);
        
        Object implementor1 = new AnonymousComplexTypeImpl();
        String address = "http://localhost:" + PORT + "/anonymous_complex_typeSOAP";
        Endpoint.publish(address, implementor1);

        Object implementor2 = new JaxbElementTestImpl();
        address = "http://localhost:" + PORT + "/jaxb_element_test";
        Endpoint.publish(address, implementor2);

        Object implementor3 = new OrderedParamHolderImpl();
        address = "http://localhost:" + PORT + "/ordered_param_holder/";
        Endpoint.publish(address, implementor3);
        
        //Object implementor4 = new DocLitWrappedCodeFirstServiceImpl();
        //Endpoint.publish(DOCLIT_CODEFIRST_URL, implementor4);
        
        Object implementor5 = new RpcLitCodeFirstServiceImpl();
        Endpoint.publish(RPCLIT_CODEFIRST_URL, implementor5);
        
        Endpoint.publish("http://localhost:" + PORT + "/InheritContext/InheritPort",
                         new InheritImpl());
    }

    public static void main(String[] args) {
        try {
            ServerMisc s = new ServerMisc();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
