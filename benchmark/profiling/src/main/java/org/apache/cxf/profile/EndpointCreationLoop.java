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

package org.apache.cxf.profile;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * 
 */
public final class EndpointCreationLoop {
    
    private GenericApplicationContext applicationContext;
    
    private EndpointCreationLoop() {
    }
    
    private void readBeans(Resource beanResource) {
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(
                        applicationContext);
        reader.loadBeanDefinitions(beanResource);
    }
    
    private void iteration() {
        applicationContext = new GenericApplicationContext();
        readBeans(new ClassPathResource("extrajaxbclass.xml"));
        applicationContext.refresh();
        applicationContext.close();
    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        EndpointCreationLoop ecl = new EndpointCreationLoop();
        int count = Integer.parseInt(args[0]);
        for (int x = 0; x < count; x++) {
            ecl.iteration();
        }
    }
}
