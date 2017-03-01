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
package org.apache.cxf.jaxrs.ext.search.client;

import java.util.Collections;
import java.util.Map;

/**
 * Builder of client-side search condition string using `fluent interface' style. It helps build create part
 * of URL that will be parsed by server-side counterpart. It is factory of different implementations e.g. for
 * {@link FiqlSearchConditionBuilder}, that has {@link org.apache.cxf.jaxrs.ext.search.FiqlParser FiqlParser}
 * on server-side, one can use <tt>SearchConditionBuilder.instance("FIQL")</tt>.
 * <p>
 * See {@link FiqlSearchConditionBuilder} for examples of usage.
 */
public abstract class SearchConditionBuilder implements PartialCondition {

    public static final String DEFAULT_LANGUAGE = "FIQL";
    public static final String FIQL = DEFAULT_LANGUAGE;
    /**
     * Creates instance of builder.
     *
     * @return default implementation of builder.
     */
    public static SearchConditionBuilder instance() {
        return instance(DEFAULT_LANGUAGE);
    }

    /**
     * Creates instance of builder with provided properties
     * @param properties
     * @return default implementation of builder.
     */
    public static SearchConditionBuilder instance(Map<String, String> properties) {
        return instance(DEFAULT_LANGUAGE, properties);
    }

    /**
     * Creates instance of builder for specific language.
     *
     * @param language alias of language, case insensitive. If alias is unknown, default FIQL implementation
     *            is returned.
     * @return implementation of expected or default builder.
     */
    public static SearchConditionBuilder instance(String language) {
        return instance(language, Collections.<String, String>emptyMap());
    }

    public static SearchConditionBuilder instance(String language, Map<String, String> properties) {
        if (!DEFAULT_LANGUAGE.equalsIgnoreCase(language)) {
            throw new IllegalArgumentException("Unsupported query language: " + language);
        }
        return new FiqlSearchConditionBuilder(properties);
    }

    /** Finalize condition construction and build search condition query. */
    public abstract String query();
}
