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

import java.util.HashMap;
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

    private static Map<String, SearchConditionBuilder> lang2impl;
    private static SearchConditionBuilder defaultImpl;
    static {
        defaultImpl = new FiqlSearchConditionBuilder();
        lang2impl = new HashMap<String, SearchConditionBuilder>();
        lang2impl.put("fiql", defaultImpl);
    }

    /**
     * Creates instance of builder.
     * 
     * @return default implementation of builder.
     */
    public static SearchConditionBuilder instance() {
        return instance("FIQL");
    }

    /**
     * Creates instance of builder for specific language.
     * 
     * @param language alias of language, case insensitive. If alias is unknown, default FIQL implementation
     *            is returned.
     * @return implementation of expected or default builder.
     */
    public static SearchConditionBuilder instance(String language) {
        SearchConditionBuilder impl = null;
        if (language != null) {
            impl = lang2impl.get(language.toLowerCase());
        }
        if (impl == null) {
            impl = new FiqlSearchConditionBuilder();
        }
        return impl;
    }

    /** Finalize condition construction and build search condition query. */
    public abstract String query();
}
