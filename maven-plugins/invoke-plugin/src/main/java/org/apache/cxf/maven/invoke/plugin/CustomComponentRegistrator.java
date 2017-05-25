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

import java.io.IOException;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

import org.w3c.dom.Node;

import org.xml.sax.SAXException;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

/**
 * Custom component configuration that registers {@link ConfigurationToNodeConverter} to convert
 * {@link PlexusConfiguration} to {@link Node} so that it can be injected into {@link org.apache.maven.plugin.Mojo}s.
 */
@Component(role = ComponentConfigurator.class, hint = "basic")
public final class CustomComponentRegistrator extends BasicComponentConfigurator implements Initializable {

    /**
     * {@link ConfigurationConverter} that converts {@link PlexusConfiguration} to {@link Node}. It does this by
     * converting the {@link PlexusConfiguration} to {@link String} and then parses this {@link String} to {@link Node}.
     */
    static final class ConfigurationToNodeConverter implements ConfigurationConverter {

        /**
         * Given {@link PlexusConfiguration} walk it's attributes and children and construct a XML String, verbatim as
         * its placed in the POM XML.
         *
         * @param configuration
         *            configuration property
         * @return XML representation of the given configuration property
         */
        static String asString(final PlexusConfiguration configuration) {
            final StringBuilder value = new StringBuilder();

            value.append('<');
            value.append(configuration.getName());

            final String attributes = stream(configuration.getAttributeNames())
                    .map(a -> a + "=\"" + configuration.getAttribute(a) + "\"").collect(Collectors.joining(" "));

            if (!attributes.isEmpty()) {
                value.append(' ');
                value.append(attributes);
            }

            value.append('>');

            for (final PlexusConfiguration child : configuration.getChildren()) {
                value.append(asString(child));
            }

            final String configurationValue = configuration.getValue();
            if ((configurationValue != null) && !configurationValue.isEmpty()) {
                value.append(configurationValue);
            }

            value.append("</");
            value.append(configuration.getName());
            value.append('>');

            return value.toString();
        }

        /**
         * Performs the common conversion of {@link PlexusConfiguration} to {@link Node} mid-step it processes any
         * expressions and re-evaluates them.
         *
         * @param configuration
         *            configuration property
         * @param expressionEvaluator
         *            evaluator for expressions
         * @return serialized, re-evaluated and parsed as XML
         * @throws ComponentConfigurationException
         *             if unable to evaluate expression or parse the XML
         */
        static Node fromConfiguration(final PlexusConfiguration configuration,
                final ExpressionEvaluator expressionEvaluator) throws ComponentConfigurationException {
            final String xmlStringValue = asString(configuration);

            final String literalStringValue;
            try {
                // we need to reevaluate expressions for any properties defined in runtime and not set by Maven
                literalStringValue = String.valueOf(expressionEvaluator.evaluate(xmlStringValue));
            } catch (final ExpressionEvaluationException e) {
                throw new ComponentConfigurationException(configuration, "Unable to evaluate expression", e);
            }

            try {
                return XmlUtil.parse(literalStringValue);
            } catch (SAXException | IOException e) {
                throw new ComponentConfigurationException(configuration, e);
            }
        }

        /**
         * Supports {@link Node} objects.
         *
         * {@inheritDoc}
         */
        @Override
        public boolean canConvert(@SuppressWarnings("rawtypes") final Class type) {
            return Node.class.isAssignableFrom(type);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object fromConfiguration(final ConverterLookup converterLookup, final PlexusConfiguration configuration,
                @SuppressWarnings("rawtypes") final Class type, @SuppressWarnings("rawtypes") final Class baseType,
                final ClassLoader classLoader, final ExpressionEvaluator expressionEvaluator)
                throws ComponentConfigurationException {
            return fromConfiguration(configuration, expressionEvaluator);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object fromConfiguration(final ConverterLookup converterLookup, final PlexusConfiguration configuration,
                @SuppressWarnings("rawtypes") final Class type, @SuppressWarnings("rawtypes") final Class baseType,
                final ClassLoader classLoader, final ExpressionEvaluator expressionEvaluator,
                final ConfigurationListener listener) throws ComponentConfigurationException {
            return fromConfiguration(configuration, expressionEvaluator);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() throws InitializationException {
        converterLookup.registerConverter(new ConfigurationToNodeConverter());
    }

    /**
     * Provide access to {@link ConverterLookup} for Unit tests.
     *
     * @return the converter lookup used
     */
    ConverterLookup getConverterLookup() {
        return converterLookup;
    }
}
