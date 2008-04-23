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

package org.apache.cxf.tools.wsdlto.frontend.jaxws;

import java.util.Map;

import org.apache.cxf.tools.common.Processor;
import org.apache.cxf.tools.plugin.FrontEnd;
import org.apache.cxf.tools.plugin.Generator;
import org.apache.cxf.tools.plugin.Plugin;
import org.apache.cxf.tools.wsdlto.core.AbstractWSDLBuilder;
import org.apache.cxf.tools.wsdlto.core.FrontEndProfile;
import org.apache.cxf.tools.wsdlto.core.PluginLoader;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.WSDLToJavaProcessor;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.wsdl11.JAXWSDefinitionBuilder;
import org.junit.Assert;
import org.junit.Test;

public class JAXWSProfileTest extends Assert {
    
    @Test
    public void testLoadPlugins() {
        PluginLoader loader = PluginLoader.getInstance();
        assertNotNull(loader);

        loader.loadPlugin("/org/apache/cxf/tools/wsdlto/frontend/jaxws/jaxws-plugin.xml");
        
        assertEquals(2, loader.getPlugins().size());
        Plugin plugin = getPlugin(loader, 1);
        assertEquals("tools-jaxws-frontend", plugin.getName());
        assertEquals("2.0", plugin.getVersion());
        assertEquals("apache cxf", plugin.getProvider());

        Map<String, FrontEnd> frontends = loader.getFrontEnds();
        assertNotNull(frontends);
        assertEquals(1, frontends.size());

        FrontEnd frontend = getFrontEnd(frontends, 0);
        assertEquals("jaxws", frontend.getName());
        assertEquals("org.apache.cxf.tools.wsdlto.frontend.jaxws", frontend.getPackage());
        assertEquals("JAXWSProfile", frontend.getProfile());
        assertNotNull(frontend.getGenerators());
        assertNotNull(frontend.getGenerators().getGenerator());
        assertEquals(2, frontend.getGenerators().getGenerator().size());
        assertEquals("AntGenerator", getGenerator(frontend, 0).getName());
        assertEquals("ImplGenerator", getGenerator(frontend, 1).getName());

        FrontEndProfile profile = loader.getFrontEndProfile("jaxws");
        assertNotNull(profile);
        //TODO: After generator completed ,umcomment these linses
        /*List<FrontEndGenerator> generators = profile.getGenerators();
        assertNotNull(generators);
        assertEquals(2, generators.size());
        assertTrue(generators.get(0) instanceof AntGenerator);
        assertTrue(generators.get(1) instanceof ImplGenerator);
        */
        Processor processor = profile.getProcessor();
        assertNotNull(processor);
        assertTrue(processor instanceof WSDLToJavaProcessor);

        AbstractWSDLBuilder builder = profile.getWSDLBuilder();
        assertNotNull(builder);
        assertTrue(builder instanceof JAXWSDefinitionBuilder);

        Class container = profile.getContainerClass();
        assertEquals(container, JAXWSContainer.class);
        assertEquals("/org/apache/cxf/tools/wsdlto/frontend/jaxws/jaxws-toolspec.xml", profile.getToolspec());
    }

    protected Generator getGenerator(FrontEnd frontend, int index) {
        return frontend.getGenerators().getGenerator().get(index);
    }
    
    protected FrontEnd getFrontEnd(Map<String, FrontEnd> frontends, int index) {
        int size = frontends.size();
        return frontends.values().toArray(new FrontEnd[size])[index];
    }

    protected Plugin getPlugin(PluginLoader loader, int index) {
        int size = loader.getPlugins().size();
        return loader.getPlugins().values().toArray(new Plugin[size])[index];
    }
}
