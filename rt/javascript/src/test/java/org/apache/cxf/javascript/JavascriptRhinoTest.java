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

package org.apache.cxf.javascript;

import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.test.AbstractCXFSpringTest;

public abstract class JavascriptRhinoTest extends AbstractCXFSpringTest {
    
    protected JavascriptTestUtilities testUtilities;
    protected JaxWsProxyFactoryBean clientProxyFactory;
    protected ServiceInfo serviceInfo;
    protected ServerFactoryBean serverFactoryBean;
    protected Object rawImplementor;
    private Endpoint endpoint;
    
    public JavascriptRhinoTest() throws Exception {
        super();
        testUtilities = new JavascriptTestUtilities(getClass());
        testUtilities.addDefaultNamespaces();
    }

    public void setupRhino(String serviceEndpointBean, 
                           String testsJavascript,
                           boolean validation) throws Exception {
        testUtilities.setBus(getBean(Bus.class, "cxf"));
        testUtilities.initializeRhino();
        serverFactoryBean = getBean(ServerFactoryBean.class, serviceEndpointBean);
        endpoint = serverFactoryBean.getServer().getEndpoint();
        // we need to find the implementor.
        rawImplementor = serverFactoryBean.getServiceBean();

        testUtilities.readResourceIntoRhino("/org/apache/cxf/javascript/cxf-utils.js");
        List<ServiceInfo> serviceInfos = endpoint.getService().getServiceInfos();
        // there can only be one.
        assertEquals(1, serviceInfos.size());
        serviceInfo = serviceInfos.get(0);
        testUtilities.loadJavascriptForService(serviceInfo);
        testUtilities.readResourceIntoRhino(testsJavascript);
        if (validation) {
            endpoint.getService().put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.TRUE);
        }
    }
    
    protected String getAddress() {
        return endpoint.getEndpointInfo().getAddress();
    }

}
