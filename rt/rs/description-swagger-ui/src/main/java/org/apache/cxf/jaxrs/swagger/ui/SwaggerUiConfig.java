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

package org.apache.cxf.jaxrs.swagger.ui;

import java.util.Map;
import java.util.TreeMap;

import org.apache.cxf.common.util.StringUtils;

/**
 * Please refer to https://github.com/swagger-api/swagger-ui/blob/master/docs/usage/configuration.md
 * to get the idea what each parameter does.
 */
public class SwaggerUiConfig {
    // URL to fetch external configuration document from.
    private String configUrl;
    // The url pointing to API definition (normally 
    // swagger.json/swagger.yaml/openapi.json/openapi.yaml).
    private String url;
    // If set, enables filtering. The top bar will show an edit box that 
    // could be used to filter the tagged operations that are shown.
    private String filter;
    
    // Enables or disables deep linking for tags and operations.
    private Boolean deepLinking;
    //  Controls the display of operationId in operations list. 
    private Boolean displayOperationId;
    // The default expansion depth for models (set to -1 completely hide the models).
    private Integer defaultModelsExpandDepth;
    // The default expansion depth for the model on the model-example section.
    private Integer defaultModelExpandDepth;
    
    // Controls how the model is shown when the API is first rendered.
    private String defaultModelRendering;
    // Controls the display of the request duration (in milliseconds) for Try-It-Out requests.
    private Boolean displayRequestDuration;
    // Controls the default expansion setting for the operations and tags. 
    private String docExpansion;
    //  If set, limits the number of tagged operations displayed to at most this many. 
    private Integer maxDisplayedTags;
    // Controls the display of vendor extension (x-) fields and values.
    private Boolean showExtensions;
    // Controls the display of extensions
    private Boolean showCommonExtensions;
    // Set a different validator URL, for example for locally deployed validators
    private String validatorUrl;
    
    public String getConfigUrl() {
        return configUrl;
    }
    
    public void setConfigUrl(final String configUrl) {
        this.configUrl = configUrl;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(final String url) {
        this.url = url;
    }
    
    public String getFilter() {
        return filter;
    }
    
    public void setFilter(final String filter) {
        this.filter = filter;
    }
    
    public Boolean getShowCommonExtensions() {
        return showCommonExtensions;
    }

    public void setShowCommonExtensions(Boolean showCommonExtensions) {
        this.showCommonExtensions = showCommonExtensions;
    }
    
    public Boolean getShowExtensions() {
        return showExtensions;
    }

    public Integer getMaxDisplayedTags() {
        return maxDisplayedTags;
    }

    public void setMaxDisplayedTags(Integer maxDisplayedTags) {
        this.maxDisplayedTags = maxDisplayedTags;
    }

    public SwaggerUiConfig maxDisplayedTags(Integer value) {
        setMaxDisplayedTags(value);
        return this;
    }

    public void setShowExtensions(Boolean showExtensions) {
        this.showExtensions = showExtensions;
    }
    
    public String getDocExpansion() {
        return docExpansion;
    }

    public void setDocExpansion(String docExpansion) {
        this.docExpansion = docExpansion;
    }
    
    public Boolean getDisplayRequestDuration() {
        return displayRequestDuration;
    }

    public void setDisplayRequestDuration(Boolean displayRequestDuration) {
        this.displayRequestDuration = displayRequestDuration;
    }
    
    public String getDefaultModelRendering() {
        return defaultModelRendering;
    }

    public void setDefaultModelRendering(String defaultModelRendering) {
        this.defaultModelRendering = defaultModelRendering;
    }
    
    public Integer getDefaultModelExpandDepth() {
        return defaultModelExpandDepth;
    }

    public void setDefaultModelExpandDepth(Integer defaultModelExpandDepth) {
        this.defaultModelExpandDepth = defaultModelExpandDepth;
    }
    
    public Integer getDefaultModelsExpandDepth() {
        return defaultModelsExpandDepth;
    }

    public void setDefaultModelsExpandDepth(Integer defaultModelsExpandDepth) {
        this.defaultModelsExpandDepth = defaultModelsExpandDepth;
    }
    
    public Boolean getDisplayOperationId() {
        return displayOperationId;
    }

    public void setDisplayOperationId(Boolean displayOperationId) {
        this.displayOperationId = displayOperationId;
    }
    
    public Boolean getDeepLinking() {
        return deepLinking;
    }

    public void setDeepLinking(Boolean deepLinking) {
        this.deepLinking = deepLinking;
    }
    

    public String getValidatorUrl() {
        return validatorUrl;
    }

    public void setValidatorUrl(String validatorUrl) {
        this.validatorUrl = validatorUrl;
    }
    
    public SwaggerUiConfig validatorUrl(String value) {
        setValidatorUrl(value);
        return this;
    }

    public SwaggerUiConfig deepLinking(Boolean value) {
        setDeepLinking(value);
        return this;
    }

    public SwaggerUiConfig displayOperationId(Boolean value) {
        setDisplayOperationId(value);
        return this;
    }

    public SwaggerUiConfig defaultModelsExpandDepth(Integer value) {
        setDefaultModelsExpandDepth(value);
        return this;
    }

    public SwaggerUiConfig defaultModelExpandDepth(Integer value) {
        setDefaultModelExpandDepth(value);
        return this;
    }

    public SwaggerUiConfig defaultModelRendering(String value) {
        setDefaultModelRendering(value);
        return this;
    }
    
    public SwaggerUiConfig displayRequestDuration(Boolean value) {
        setDisplayRequestDuration(value);
        return this;
    }

    public SwaggerUiConfig docExpansion(String value) {
        setDocExpansion(value);
        return this;
    }

    public SwaggerUiConfig showExtensions(Boolean value) {
        setShowExtensions(value);
        return this;
    }
    
    public SwaggerUiConfig showCommonExtensions(Boolean value) {
        setShowCommonExtensions(value);
        return this;
    }
    
    public SwaggerUiConfig url(final String u) {
        setUrl(u);
        return this;
    }
    
    public SwaggerUiConfig configUrl(final String cu) {
        setConfigUrl(cu);
        return this;
    }

    public SwaggerUiConfig filter(final String f) {
        setFilter(f);
        return this;
    }

    public Map<String, String> getConfigParameters() {
        final Map<String, String> params = new TreeMap<>();
        
        put("url", getUrl(), params);
        put("configUrl", getConfigUrl(), params);
        put("filter", getFilter(), params);
        put("deepLinking", getDeepLinking(), params);
        put("displayOperationId", getDisplayOperationId(), params);
        put("defaultModelsExpandDepth", getDefaultModelsExpandDepth(), params);
        put("defaultModelExpandDepth", getDefaultModelExpandDepth(), params);
        put("defaultModelRendering", getDefaultModelRendering(), params);
        put("displayRequestDuration", getDisplayRequestDuration(), params);
        put("docExpansion", getDocExpansion(), params);
        put("maxDisplayedTags", getMaxDisplayedTags(), params);
        put("showExtensions", getShowExtensions(), params);
        put("showCommonExtensions", getShowCommonExtensions(), params);
        put("validatorUrl", getValidatorUrl(), params);

        return params;
    }
    
    protected static void put(final String name, final Integer value, final Map<String, String> params) {
        if (value != null) {
            params.put(name, value.toString());
        }
    }

    protected static void put(final String name, final Boolean value, final Map<String, String> params) {
        if (value != null) {
            params.put(name, value.toString());
        }
    }
    
    protected static void put(final String name, final String value, final Map<String, String> params) {
        if (!StringUtils.isEmpty(value)) {
            params.put(name, value);
        }
    }
}
