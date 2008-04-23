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

package org.apache.cxf.tools.corba.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;

public abstract class WSDLCorbaFactory {
    private static final String PROPERTY_NAME = "javax.wsdl.factory.WSDLCorbaFactory";
    private static final String PROPERTY_FILE_NAME = "wsdl.properties";
    private static final String DEFAULT_FACTORY_IMPL_NAME =
        "org.apache.cxf.tools.corba.processors.wsdl.WSDLCorbaFactoryImpl";

    private static String fullPropertyFileName;

    /**
     * Get a new instance of a WSDLFactory. This method
     * follows (almost) the same basic sequence of steps that JAXP
     * follows to determine the fully-qualified class name of the
     * class which implements WSDLFactory. The steps (in order)
     * are:
     *<pre>
     *  Check the javax.wsdl.factory.WSDLFactory system property.
     *  Check the lib/wsdl.properties file in the JRE directory. The key
     * will have the same name as the above system property.
     *  Use the default value.
     *</pre>
     * Once an instance of a WSDLFactory is obtained, invoke
     * newDefinition(), newWSDLReader(), or newWSDLWriter(), to create
     * the desired instances.
     */
    public static WSDLCorbaFactory newInstance() throws WSDLException {
        String factoryImplName = findFactoryImplName();

        return newInstance(factoryImplName);
    }

    /**
     * Get a new instance of a WSDLFactory. This method
     * returns an instance of the class factoryImplName.
     * Once an instance of a WSDLFactory is obtained, invoke
     * newDefinition(), newWSDLReader(), or newWSDLWriter(), to create
     * the desired instances.
     *
     * @param factoryImplName the fully-qualified class name of the
     * class which provides a concrete implementation of the abstract
     * class WSDLFactory.
     */
    public static WSDLCorbaFactory newInstance(String factoryImplName) throws WSDLException {
        if (factoryImplName != null) {
            try {
                // get the appropriate class for the loading.
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Class cl = loader.loadClass(factoryImplName);

                return (WSDLCorbaFactory)cl.newInstance();
            } catch (Exception e) {
                /*
                 Catches:
                 ClassNotFoundException
                 InstantiationException
                 IllegalAccessException
                 */
                throw new WSDLException(WSDLException.CONFIGURATION_ERROR, "Problem instantiating factory "
                                                                           + "implementation.", e);
            }
        } else {
            throw new WSDLException(WSDLException.CONFIGURATION_ERROR, "Unable to find name of factory "
                                                                       + "implementation.");
        }
    }

    /**
     * Create a new instance of a Definition.
     */
    public abstract Definition newDefinition();

    /**
     * Create a new instance of a WSDLReader.
     */
    public abstract WSDLReader newWSDLReader();

    /**
     * Create a new instance of a WSDLWriter.
     */
    public abstract WSDLWriter newWSDLWriter();

    /**
     * Create a new instance of an ExtensionRegistry with pre-registered
     * serializers/deserializers for the SOAP, HTTP and MIME
     * extensions. Java extensionTypes are also mapped for all
     * the SOAP, HTTP and MIME extensions.
     */
    public abstract ExtensionRegistry newPopulatedExtensionRegistry();

    private static String findFactoryImplName() {
        String factoryImplName = null;

        // First, check the system property.
        try {
            factoryImplName = System.getProperty(PROPERTY_NAME);

            if (factoryImplName != null) {
                return factoryImplName;
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        // Second, check the properties file.
        String propFileName = getFullPropertyFileName();

        if (propFileName != null) {
            try {
                Properties properties = new Properties();
                File propFile = new File(propFileName);
                FileInputStream fis = new FileInputStream(propFile);

                properties.load(fis);
                fis.close();

                factoryImplName = properties.getProperty(PROPERTY_NAME);

                if (factoryImplName != null) {
                    return factoryImplName;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Third, return the default.
        return DEFAULT_FACTORY_IMPL_NAME;
    }

    private static String getFullPropertyFileName() {
        if (fullPropertyFileName == null) {
            try {
                String javaHome = System.getProperty("java.home");

                fullPropertyFileName = javaHome + File.separator + "lib" + File.separator
                                       + PROPERTY_FILE_NAME;
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        return fullPropertyFileName;
    }
}
