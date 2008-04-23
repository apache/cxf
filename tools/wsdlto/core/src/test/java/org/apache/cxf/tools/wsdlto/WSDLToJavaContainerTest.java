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

package org.apache.cxf.tools.wsdlto;

import java.net.URISyntaxException;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.junit.Assert;
import org.junit.Test;

public class WSDLToJavaContainerTest extends Assert {
    
    @Test
    public void testNoPlugin() throws Exception {
        WSDLToJavaContainer container = new WSDLToJavaContainer("dummy", null);

        ToolContext context = new ToolContext();
        context.put(ToolConstants.CFG_WSDLURL, getLocation("hello_world.wsdl"));
        container.setContext(context);
        
        try {
            container.execute();
        } catch (ToolException te) {
            assertEquals(getLogMessage("FOUND_NO_FRONTEND"), te.getMessage());
        } catch (Exception e) {
            fail("Should not throw any exception but ToolException.");
        }
    }

    @Test
    public void testValidateorSuppressWarningsIsOn() throws Exception {
        WSDLToJavaContainer container = new WSDLToJavaContainer("dummy", null);

        ToolContext context = new ToolContext();
        context.put(ToolConstants.CFG_WSDLURL, getLocation("hello_world.wsdl"));
        container.setContext(context);

        try {
            container.execute();
        } catch (ToolException te) {
            assertEquals(getLogMessage("FOUND_NO_FRONTEND"), te.getMessage());
        } catch (Exception e) {
            fail("Should not throw any exception but ToolException.");
        }

        assertTrue(context.optionSet(ToolConstants.CFG_SUPPRESS_WARNINGS));
    }

    private String getLocation(String wsdlFile) throws URISyntaxException {
        return this.getClass().getResource(wsdlFile).toURI().getPath();
    }

    protected String getLogMessage(String key, Object...params) {
        return new Message(key, WSDLToJavaContainer.LOG, params).toString();
    }
}
