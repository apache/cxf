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
package org.apache.cxf.transport.servlet.blueprint;

import jakarta.servlet.ServletConfig;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.service.blueprint.container.BlueprintContainer;

public class CXFBlueprintServlet extends CXFNonSpringServlet {
    private static final String CONTAINER_ATTRIBUTE = "org.apache.aries.blueprint.container";

    private static final long serialVersionUID = -5922443981969455305L;
    private boolean busCreated;
    public CXFBlueprintServlet() {
    }

    @Override
    protected void loadBus(ServletConfig servletConfig) {
        BlueprintContainer container =
            (BlueprintContainer)servletConfig.getServletContext().getAttribute(CONTAINER_ATTRIBUTE);

        if (container != null) {
            Object busComponent = container.getComponentInstance("cxf");
            setBus((Bus)busComponent);
        } else {
            busCreated = true;
            setBus(BusFactory.newInstance().createBus());
        }
    }

    public void destroyBus() {
        if (busCreated) {
            //if we created the Bus, we need to destroy it.  Otherwise, spring will handle it.
            getBus().shutdown(true);
            setBus(null);
        }
    }

}
