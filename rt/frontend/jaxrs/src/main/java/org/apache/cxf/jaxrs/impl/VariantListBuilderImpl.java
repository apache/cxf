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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;
import javax.ws.rs.core.Variant.VariantListBuilder;

public class VariantListBuilderImpl extends VariantListBuilder {
    
    private List<String> encodings = new ArrayList<String>();
    private List<Locale> languages = new ArrayList<Locale>();
    private List<MediaType> mediaTypes = new ArrayList<MediaType>();
    private List<Variant> variants = new ArrayList<Variant>();
    
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
        List<Variant> vs = new ArrayList<Variant>(variants);
        reset();
        return vs;
    }

    @Override
    public VariantListBuilder encodings(String... encs) {
        encodings.addAll(Arrays.asList(encs));
        return this;
    }

    @Override
    public VariantListBuilder mediaTypes(MediaType... types) {
        mediaTypes.addAll(Arrays.asList(types));
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
        if (mediaTypes.size() > 0) {
            handleMediaTypes();
        } else if (languages.size() > 0) {
            handleLanguages(null);
        } else if (encodings.size() > 0) {
            for (String enc : encodings) {
                variants.add(new Variant(null, null, enc));
            }
        } 
    }
    
    private void handleMediaTypes() {
        for (MediaType type : mediaTypes) {
            if (languages.size() > 0) {
                handleLanguages(type);
            } else if (encodings.size() > 0) {
                for (String enc : encodings) {
                    variants.add(new Variant(type, null, enc));
                }
            } else {
                variants.add(new Variant(type, null, null));
            }
        }
    }
    
    private void handleLanguages(MediaType type) {
        for (Locale lang : languages) {
            if (encodings.size() > 0) {
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
        languages.addAll(Arrays.asList(ls));
        return this;
    }
}
