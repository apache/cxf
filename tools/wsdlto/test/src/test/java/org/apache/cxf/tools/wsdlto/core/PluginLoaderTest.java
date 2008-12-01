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

package org.apache.cxf.tools.wsdlto.core;

import java.util.Map;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.tools.plugin.DataBinding;
import org.apache.cxf.tools.plugin.FrontEnd;
import org.apache.cxf.tools.plugin.Generator;
import org.apache.cxf.tools.plugin.Plugin;
import org.junit.Assert;
import org.junit.Test;

public class PluginLoaderTest extends Assert {
   
    @Test
    public void testLoadPlugins() throws Exception {
        PluginLoader loader = PluginLoader.getInstance();
        assertEquals(3, loader.getPlugins().size());

        Plugin plugin = getPlugin(loader, 0);
        assertNotNull(plugin.getName());
        
        Map<String, FrontEnd> frontends = loader.getFrontEnds();
        assertNotNull(frontends);
        assertEquals(1, frontends.size());

        FrontEnd frontend = getFrontEnd(frontends, 0);
        assertEquals("jaxws", frontend.getName());
        assertEquals("org.apache.cxf.tools.wsdlto.frontend.jaxws", frontend.getPackage());
        assertEquals("JAXWSProfile", frontend.getProfile());
        assertNotNull(frontend.getGenerators());
        assertNotNull(frontend.getGenerators().getGenerator());
        
        assertEquals("AntGenerator", getGenerator(frontend, 0).getName());
        
        assertEquals("JAXWSContainer", frontend.getContainer().getName());
        assertEquals("jaxws-toolspec.xml", frontend.getContainer().getToolspec());
        
        loader.getFrontEndProfile("jaxws");
                  
        Map<String, DataBinding> databindings = loader.getDataBindings();
        assertNotNull(databindings);
        assertEquals(2, databindings.size());
        
        DataBinding databinding = getDataBinding(databindings, 0);
        assertEquals("jaxb", databinding.getName());
        assertEquals("org.apache.cxf.tools.wsdlto.databinding.jaxb", databinding.getPackage());
        assertEquals("JAXBDataBinding", databinding.getProfile());
    }

    protected String getLogMessage(String key, Object...params) {
        return new Message(key, PluginLoader.LOG, params).toString();
    }

    protected Generator getGenerator(FrontEnd frontend, int index) {
        return frontend.getGenerators().getGenerator().get(index);
    }
    
    protected FrontEnd getFrontEnd(Map<String, FrontEnd> frontends, int index) {
        int size = frontends.size();
        return frontends.values().toArray(new FrontEnd[size])[index];
    }

    protected DataBinding getDataBinding(Map<String, DataBinding> databindings, int index) {
        int size = databindings.size();
        return databindings.values().toArray(new DataBinding[size])[index];
    }
    
    protected Plugin getPlugin(PluginLoader loader, int index) {
        int size = loader.getPlugins().size();
        return loader.getPlugins().values().toArray(new Plugin[size])[index];
    }
}
