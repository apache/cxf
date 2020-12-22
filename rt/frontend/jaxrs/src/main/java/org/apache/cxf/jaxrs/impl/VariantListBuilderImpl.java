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


package org.apache.cxf.jaxrs.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Variant;
import jakarta.ws.rs.core.Variant.VariantListBuilder;

public class VariantListBuilderImpl extends VariantListBuilder {

    private List<String> encodings = new ArrayList<>();
    private List<Locale> languages = new ArrayList<>();
    private List<MediaType> mediaTypes = new ArrayList<>();
    private List<Variant> variants = new ArrayList<>();

    public VariantListBuilderImpl() {

    }

    @Override
    public VariantListBuilder add() {
        addVariants();
        resetMeta();
        return this;
    }

    @Override
    public List<Variant> build() {
        addVariants();
        List<Variant> vs = new ArrayList<>(variants);
        reset();
        return vs;
    }

    @Override
    public VariantListBuilder encodings(String... encs) {
        Collections.addAll(encodings, encs);
        return this;
    }

    @Override
    public VariantListBuilder mediaTypes(MediaType... types) {
        Collections.addAll(mediaTypes, types);
        return this;
    }

    private void reset() {
        variants.clear();
        resetMeta();
    }

    private void resetMeta() {
        mediaTypes.clear();
        languages.clear();
        encodings.clear();
    }

    private void addVariants() {
        if (!mediaTypes.isEmpty()) {
            handleMediaTypes();
        } else if (!languages.isEmpty()) {
            handleLanguages(null);
        } else if (!encodings.isEmpty()) {
            for (String enc : encodings) {
                variants.add(new Variant(null, (Locale)null, enc));
            }
        }
    }

    private void handleMediaTypes() {
        for (MediaType type : mediaTypes) {
            if (!languages.isEmpty()) {
                handleLanguages(type);
            } else if (!encodings.isEmpty()) {
                for (String enc : encodings) {
                    variants.add(new Variant(type, (Locale)null, enc));
                }
            } else {
                variants.add(new Variant(type, (Locale)null, null));
            }
        }
    }

    private void handleLanguages(MediaType type) {
        for (Locale lang : languages) {
            if (!encodings.isEmpty()) {
                for (String enc : encodings) {
                    variants.add(new Variant(type, lang, enc));
                }
            } else {
                variants.add(new Variant(type, lang, null));
            }
        }
    }

    @Override
    public VariantListBuilder languages(Locale... ls) {
        Collections.addAll(languages, ls);
        return this;
    }
}
