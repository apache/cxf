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

package org.apache.cxf.management.web.browser.client.service.browser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FilterOptions {
    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    public static final FilterOptions EMPTY = new FilterOptions();

    @Nullable private String phrase;
    @Nullable private Date from;
    @Nullable private Date to;
    @Nonnull private List<Level> levels;

    private FilterOptions() {
        this.levels = new ArrayList<Level>();
    }

    public FilterOptions(@Nullable String phrase, @Nullable Date from,
                         @Nullable Date to, @Nonnull List<Level> levels) {
        this.phrase = phrase;
        this.from = from;
        this.to = to;
        this.levels = levels;
    }

    @Nullable
    public String getPhrase() {
        return phrase;
    }

    @Nullable
    public Date getFrom() {
        return from;
    }

    @Nullable
    public Date getTo() {
        return to;
    }

    @Nonnull
    public List<Level> getLevels() {
        return levels;
    }
}
