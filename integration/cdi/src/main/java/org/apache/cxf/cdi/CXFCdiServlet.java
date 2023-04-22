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

import java.util.Set;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;

/**
 * Apache CXF servlet with CDI 1.1 integration support
 */
public class CXFCdiServlet extends CXFNonSpringServlet {
    private static final long serialVersionUID = -2890970731778523861L;
    private boolean busCreated;

    @Override @Inject
    public void setBus(final Bus bus) {
        super.setBus(bus);
    }

    @Override
    protected void loadBus(ServletConfig servletConfig) {
        Bus bus = null;

        final BeanManager beanManager = CDI.current().getBeanManager();
        if (beanManager != null) {
            final Set< Bean< ? > > candidates = beanManager.getBeans(CdiBusBean.CXF);

            if (!candidates.isEmpty()) {
                final Bean< ? > candidate = beanManager.resolve(candidates);

                bus = (Bus)beanManager.getReference(candidate, Bus.class,
                    beanManager.createCreationalContext(candidate));
            }
        }

        if (bus != null) {
            setBus(bus);
        } else {
            busCreated = true;
            setBus(BusFactory.newInstance().createBus());
        }
    }

    @Override
    public void destroyBus() {
        if (busCreated) {
            //if we created the Bus, we need to destroy it.  Otherwise, spring will handle it.
            getBus().shutdown(true);
            setBus(null);
        }
    }
}
