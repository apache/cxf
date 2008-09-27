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
package org.apache.cxf.jca.core.resourceadapter;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.ResourceException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jca.core.logging.LoggerHelper;



public class ResourceBean implements Serializable {

    public static final String DEFAULT_VALUE_STRING = "DEFAULT";
    public static final String LOG_LEVEL = "log.level";
    
    @Deprecated
    public static final String CONFIG_DOMAIN = "cxf";
    @Deprecated
    public static final String CONFIG_SCOPE = "j2ee";
    
    public static final String DEFAULT_MONITOR_POLL_INTERVAL = "120";    
    public static final String EJB_SERVICE_PROPERTIES_URL = "ejb.service.properties.url";
    public static final String MONITOR_EJB_SERVICE_PROPERTIES = "monitor.ejb.service.properties";
    public static final String MONITOR_POLL_INTERVAL = "monitor.poll.interval"; 
    public static final String EJB_SERVANT_BASE_URL = "ejb.servant.base.url";
    

    static {
        // first use of log, default init if necessary
        LoggerHelper.init();
    }

    private static final Logger LOG = LogUtils.getL7dLogger(ResourceBean.class);

    private Properties pluginProps;

    public ResourceBean() {
        pluginProps = new Properties();
    }

    public ResourceBean(Properties props) {
        pluginProps = props;
    }

    public void setDisableConsoleLogging(boolean disable) {
        if (disable) {
            LoggerHelper.disableConsoleLogging();
        }
    }    
   
    public Properties getPluginProps() {
        return pluginProps;
    }

    public void setProperty(String propName, String propValue) {
        if (!DEFAULT_VALUE_STRING.equals(propValue)) {
            LOG.log(Level.FINE, "SETTING_PROPERTY", new Object[] {propName, propValue});
            getPluginProps().setProperty(propName, propValue);
        }
        if (LOG_LEVEL.equals(propName)) {
            LoggerHelper.setLogLevel(propValue);
        }
    }

    
    protected URL getPropsURL(String propsUrl) throws ResourceException {
        URL ret = null;
        if (propsUrl != null) {
            ret = createURL(propsUrl, "Unable to construct URL from URL string, value=" + propsUrl);
        }
        return ret;
    }

    protected URL createURL(String spec, String msg) throws ResourceAdapterInternalException {
        try {
            return new URL(spec);
        } catch (MalformedURLException mue) {
            throw new ResourceAdapterInternalException(msg, mue);
        }
    }

    public void validateURLString(String spec, String msg) throws ResourceAdapterInternalException {
        URL url = null;
        try {
            url = createURL(spec, msg);
            url.openStream();
            LOG.fine("Validated url=" + url);
        } catch (IOException ioe) {
            throw new ResourceAdapterInternalException(msg, ioe);
        }
    }
    

}
