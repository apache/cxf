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

package org.apache.cxf.jbi.se;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.cxf.common.classloader.FireWallClassLoader;
import org.apache.cxf.common.logging.LogUtils;

public class ComponentClassLoader extends URLClassLoader {

    private static final Logger LOG =  LogUtils.getL7dLogger(ComponentClassLoader.class);
    
    private static final String FILTERS_PROPS_FILE = "filters.properties";
    private static final String NEFILTERS_PROPS_FILE = "negativefilters.properties";

    
    public ComponentClassLoader(URL[] urls, ClassLoader p) throws IOException {
        
        super(urls, new FireWallClassLoader(p, 
                                      getFilterList(p, FILTERS_PROPS_FILE),
                                      getFilterList(p, NEFILTERS_PROPS_FILE)));
        
    }
    
    public void addResource(URL url) {
        addURL(url);
    }
    
        
    private static String[] getFilterList(ClassLoader parent, String propFile) throws IOException {
        Properties filtersProps = getProperties(parent, propFile);
        Iterator<Object> i = filtersProps.keySet().iterator();
        while (i.hasNext()) {            
            LOG.config("get Filter " + propFile + "::" + (String)i.next());            
        }    
        return (String[])filtersProps.keySet().toArray(new String[] {});
    }

    private static Properties getProperties(ClassLoader parent, String propsFileName) throws IOException {
        InputStream in = parent.getResourceAsStream(propsFileName);

        if (null == in) {
            in = ComponentClassLoader.class.getResourceAsStream(propsFileName);

            if (null == in) {
                String msg = "Internal component classloader failed to locate configuration resource: "
                        + propsFileName;
                IOException ioe = new IOException(msg);

                LOG.warning(ioe.toString());
                throw ioe;
            }
        }

        Properties props = new Properties();

        props.load(in);
        LOG.fine("Contents: " + propsFileName + props);

        return props;
    }
     
}
