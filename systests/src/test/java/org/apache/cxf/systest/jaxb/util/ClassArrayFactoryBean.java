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

package org.apache.cxf.systest.jaxb.util;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class ClassArrayFactoryBean implements FactoryBean {

    private List<String> classNames;

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.beans.factory.FactoryBean#getObject()
     */
    public Object getObject() throws Exception {

        final Class<?>[] classes = new Class<?>[classNames.size()];
        for (int i = 0; i < classNames.size(); i++) {
            classes[i] = Class.forName(classNames.get(i));
        }
        return classes;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.beans.factory.FactoryBean#getObjectType()
     */
    public Class<?> getObjectType() {
        return Class[].class;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.beans.factory.FactoryBean#isSingleton()
     */
    public boolean isSingleton() {
        return true;
    }

    /**
     * @return the classNames
     */
    public List<String> getClassNames() {
        return classNames;
    }

    /**
     * @param classNames the classNames to set
     */
    public void setClassNames(List<String> classNames) {
        this.classNames = classNames;
    }

}
