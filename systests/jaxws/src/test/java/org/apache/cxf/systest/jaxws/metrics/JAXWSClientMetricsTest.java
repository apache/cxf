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

package org.apache.cxf.systest.jaxws.metrics;

import java.util.Arrays;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.common.i18n.UncheckedException;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxws.JaxWsClientFactoryBean;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.metrics.MetricsContext;
import org.apache.cxf.metrics.MetricsFeature;
import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.test.AbstractCXFSpringTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class JAXWSClientMetricsTest extends AbstractCXFSpringTest {
    @Rule public ExpectedException expectedException = ExpectedException.none();
    
    private MetricsProvider provider;
    private MetricsContext operationContext;
    private MetricsContext resourceContext;
    private MetricsContext endpointContext;
    
    @Before
    public void setUp() {
        endpointContext = Mockito.mock(MetricsContext.class);
        operationContext = Mockito.mock(MetricsContext.class);
        resourceContext = Mockito.mock(MetricsContext.class);

        provider = new MetricsProvider() {
            public MetricsContext createEndpointContext(Endpoint endpoint, boolean asClient, String cid) {
                return endpointContext;
            }

            public MetricsContext createOperationContext(Endpoint endpoint, BindingOperationInfo boi, 
                    boolean asClient, String cid) {
                return operationContext;
            }

            public MetricsContext createResourceContext(Endpoint endpoint, String resourceName, 
                    boolean asClient, String cid) {
                return resourceContext;
            }
        };
    }

    @Test
    public void usingClientProxyStopIsCalledWhenServerReturnsResponse() throws Exception {
        final JaxWsClientFactoryBean factory = new JaxWsClientFactoryBean();
        factory.setAddress("local://services/Book");
        factory.setServiceClass(IBookWebService.class);
        factory.setFeatures(Arrays.asList(new MetricsFeature(provider)));
        
        try {
            final Client client = factory.create();
            String response = (String)client.invoke("getBook", 10)[0];
            assertEquals("All your bases belong to us.", response);
        } finally {
            Mockito.verify(operationContext, times(1)).start(any(Exchange.class));
            Mockito.verify(operationContext, times(1)).stop(anyLong(), anyLong(), anyLong(), any(Exchange.class));
            Mockito.verify(endpointContext, times(1)).start(any(Exchange.class));
            Mockito.verify(endpointContext, times(1)).stop(anyLong(), anyLong(), anyLong(), any(Exchange.class));
            Mockito.verifyNoInteractions(resourceContext);
        }
    }
   
    @Test
    public void usingClientProxyStopIsCalledWhenServerReturnsFault() throws Exception {
        final JaxWsClientFactoryBean factory = new JaxWsClientFactoryBean();
        factory.setAddress("local://services/Book");
        factory.setServiceClass(IBookWebService.class);
        factory.setFeatures(Arrays.asList(new MetricsFeature(provider)));
        
        try {
            final Client client = factory.create();
            expectedException.expect(SoapFault.class);
            client.invoke("getBook", 11);
        } finally {
            Mockito.verify(operationContext, times(1)).start(any(Exchange.class));
            Mockito.verify(operationContext, times(1)).stop(anyLong(), anyLong(), anyLong(), any(Exchange.class));
            Mockito.verify(endpointContext, times(1)).start(any(Exchange.class));
            Mockito.verify(endpointContext, times(1)).stop(anyLong(), anyLong(), anyLong(), any(Exchange.class));
            Mockito.verifyNoInteractions(resourceContext);
        }
    }

    @Test
    public void usingClientProxyStopIsCalledForUnsupportedOperation() throws Exception {
        final JaxWsClientFactoryBean factory = new JaxWsClientFactoryBean();
        factory.setAddress("local://services/Book");
        factory.setServiceClass(IBookWebService.class);
        factory.setFeatures(Arrays.asList(new MetricsFeature(provider)));
        
        try {
            final Client client = factory.create();
            expectedException.expect(UncheckedException.class);
            client.invoke("getBooks");
        } finally {
            Mockito.verifyNoInteractions(endpointContext);
            Mockito.verifyNoInteractions(operationContext);
            Mockito.verifyNoInteractions(resourceContext);
        }
    }

    @Override
    protected String[] getConfigLocations() {
        return new String[] {"/org/apache/cxf/systest/jaxws/metrics/context.xml" };
    }
}
