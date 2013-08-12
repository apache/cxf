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

package org.apache.cxf.configuration;

/**
 * The configurer's interface
 * 
 * A class that implements this interface will perform a 
 * bean's configuration work
 */
public interface Configurer {    
    
    String DEFAULT_USER_CFG_FILE = "cxf.xml";

    String USER_CFG_FILE_PROPERTY_NAME = "cxf.config.file";
    
    String USER_CFG_FILE_PROPERTY_URL = "cxf.config.file.url";

    /**
     * set up the Bean's value by using Dependency Injection from the application context
     * @param beanInstance the instance of the bean which needs to be configured
     */
    void configureBean(Object beanInstance);
    
    /**
     * set up the Bean's value by using Dependency Injection from the application context
     * with a proper name. You can use * as the prefix of wildcard name.
     * @param name the name of the bean which needs to be configured
     * @param beanInstance the instance of bean which need to be configured
     */
    void configureBean(String name, Object beanInstance);

}
