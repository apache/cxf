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
package org.apache.cxf.maven.invoke.plugin;

import org.w3c.dom.Node;

import org.apache.cxf.maven.invoke.plugin.CustomComponentRegistrator.ConfigurationToNodeConverter;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.junit.Rule;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CustomComponentRegistratorTest {

    @Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Mock
    private ExpressionEvaluator expressionEvaluator;

    @Test
    public void shouldConvertElaborateConfigurationToNode()
            throws ComponentConfigurationException, ExpressionEvaluationException {
        final PlexusConfiguration configuration = new DefaultPlexusConfiguration("test");
        configuration.setAttribute("attr", "attribute-value");
        final PlexusConfiguration child = new DefaultPlexusConfiguration("child");
        configuration.addChild(child);
        child.addChild("grandchild", "grandchild-value");

        final String xml = "<test attr=\"attribute-value\">"//
                + "<child>"//
                + "<grandchild>grandchild-value</grandchild>"//
                + "</child>"//
                + "</test>";
        expect(expressionEvaluator.evaluate(xml)).andReturn(xml);

        replay(expressionEvaluator);

        final Node node = ConfigurationToNodeConverter.fromConfiguration(configuration, expressionEvaluator);

        verify(expressionEvaluator);

        assertThat("Should convert trivial example", node, notNullValue());

        assertThat("Parsed is test xml element", node.getLocalName(), equalTo("test"));
        assertThat("Parsed test xml element has attribute", node.getAttributes().getNamedItem("attr").getNodeValue(),
                equalTo("attribute-value"));
        assertThat("Parsed test xml element has child xml element", node.getFirstChild().getLocalName(),
                equalTo("child"));
        assertThat("Parsed test xml element has grandchildchild xml element",
                node.getFirstChild().getFirstChild().getLocalName(), equalTo("grandchild"));
    }

    @Test
    public void shouldConvertTrivialConfigurationToNode()
            throws ComponentConfigurationException, ExpressionEvaluationException {
        final PlexusConfiguration configuration = new DefaultPlexusConfiguration("test");

        expect(expressionEvaluator.evaluate("<test></test>")).andReturn("<test></test>");

        replay(expressionEvaluator);

        final Node node = ConfigurationToNodeConverter.fromConfiguration(configuration, expressionEvaluator);

        verify(expressionEvaluator);

        assertThat("Should convert trivial example", node, notNullValue());

        assertThat("Parsed is test xml element", node.getLocalName(), equalTo("test"));
    }

    @Test
    public void shouldRegisterConverter() throws InitializationException, ComponentConfigurationException {
        final CustomComponentRegistrator customComponentRegistrator = new CustomComponentRegistrator();

        customComponentRegistrator.initialize();

        final ConverterLookup converterLookup = customComponentRegistrator.getConverterLookup();
        final ConfigurationConverter converter = converterLookup.lookupConverterForType(Node.class);

        assertThat("Registrator should register our converter", converter,
                instanceOf(ConfigurationToNodeConverter.class));
    }
}
