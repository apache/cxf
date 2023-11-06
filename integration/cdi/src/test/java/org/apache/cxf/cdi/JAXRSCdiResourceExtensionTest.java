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

import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionTargetFactory;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.ws.rs.Path;
import org.apache.cxf.Bus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JAXRSCdiResourceExtensionTest {
    private final JAXRSCdiResourceExtension extension = new JAXRSCdiResourceExtension();
    @Mock
    private BeanManager beanManager;
    @Mock
    private AfterBeanDiscovery event;
    @Mock
    private Bean<Bus> busBean;
    @Mock
    private ProcessBean<Bus> processBean;
    @Mock
    private Annotated annotated;
    @Mock
    private InjectionTargetFactory<Object> factory;
    
    @Test
    public void shouldNotAddDefaultApplicationWhenNoResourcesDefined() {
        when(beanManager.getInjectionTargetFactory(any())).thenReturn(factory);
        extension.injectBus(event, beanManager);

        verify(event).addBean(any(CdiBusBean.class));
        verify(event, never()).addBean(any(DefaultApplicationBean.class));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void shouldNotAddBusBeanIfBeanAlreadySent() {
        when(processBean.getBean()).thenReturn(busBean);
        when(processBean.getAnnotated()).thenReturn(annotated);
        Class cls = Bus.class;
        when(busBean.getBeanClass()).thenReturn(cls);
        when(busBean.getName()).thenReturn(CdiBusBean.CXF);
        extension.collect(processBean, beanManager);

        extension.injectBus(event, beanManager);

        verify(event, never()).addBean(any(CdiBusBean.class));
    }

    @Test
    public void shouldAddApplicationBeanWhenResourcesProvided() {
        when(processBean.getBean()).thenReturn(busBean);
        when(processBean.getAnnotated()).thenReturn(annotated);
        when(annotated.isAnnotationPresent(Path.class)).thenReturn(true);
        extension.collect(processBean, beanManager);

        when(beanManager.getInjectionTargetFactory(any())).thenReturn(factory);
        extension.injectBus(event, beanManager);

        verify(event).addBean(any(DefaultApplicationBean.class));
    }
}