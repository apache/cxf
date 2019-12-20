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
package org.apache.cxf.systest.jaxrs.cdi;

import org.jboss.weld.environment.jetty.JettyContainer;
import org.jboss.weld.environment.servlet.Container;
import org.jboss.weld.environment.servlet.ContainerContext;
import org.jboss.weld.environment.tomcat.TomcatContainer;
import org.jboss.weld.resources.spi.ResourceLoader;

/**
 * Because we are mixing several Servlet Containers in the same project, JBoss Weld
 * needs some help to figure out the correct one we are running right now.
 */
public class WeldDiscoverableContainer implements Container {
    private final Container delegate;

    public WeldDiscoverableContainer() {
        if (JettyContainer.class.getName().equals(System.getProperty(Container.class.getName()))) {
            delegate = JettyContainer.INSTANCE;
        } else {
            delegate = TomcatContainer.INSTANCE;
        }
    }

    @Override
    public boolean touch(ResourceLoader loader, ContainerContext context) throws Exception {
        return delegate.touch(loader, context);
    }

    @Override
    public void initialize(ContainerContext context) {
        delegate.initialize(context);
    }

    @Override
    public void destroy(ContainerContext context) {
        delegate.destroy(context);
    }
}
