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

import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

import org.junit.Assert;
import org.junit.Test;

public class VariantListBuilderImplTest extends Assert {
    
    @Test
    public void testBuildAll() {
        VariantListBuilderImpl vb = new VariantListBuilderImpl();
        MediaType mt1 = new MediaType("*", "*"); 
        MediaType mt2 = new MediaType("text", "xml");
        List<Variant> variants = vb.mediaTypes(mt1, mt2)
            .languages(new Locale("en"), new Locale("fr")).encodings("zip", "identity").add().build();
        assertEquals("8 variants need to be created", 8, variants.size());
        assertTrue(verifyVariant(variants, new Variant(mt1, new Locale("en"), "zip")));
        assertTrue(verifyVariant(variants, new Variant(mt1, new Locale("en"), "identity")));
        assertTrue(verifyVariant(variants, new Variant(mt1, new Locale("fr"), "zip")));
        assertTrue(verifyVariant(variants, new Variant(mt1, new Locale("fr"), "identity")));
        assertTrue(verifyVariant(variants, new Variant(mt2, new Locale("en"), "zip")));
        assertTrue(verifyVariant(variants, new Variant(mt2, new Locale("en"), "identity")));
        assertTrue(verifyVariant(variants, new Variant(mt2, new Locale("fr"), "zip")));
        assertTrue(verifyVariant(variants, new Variant(mt2, new Locale("fr"), "identity")));
    }
    
    @Test
    public void testBuildTypeAndEnc() {
        VariantListBuilderImpl vb = new VariantListBuilderImpl();
        MediaType mt1 = new MediaType("*", "*"); 
        MediaType mt2 = new MediaType("text", "xml");
        List<Variant> variants = 
            vb.mediaTypes(mt1, mt2).encodings("zip", "identity").add().build();
        assertEquals("4 variants need to be created", 4, variants.size());
        assertTrue(verifyVariant(variants, new Variant(mt1, null, "zip")));
        assertTrue(verifyVariant(variants, new Variant(mt1, null, "identity")));
        assertTrue(verifyVariant(variants, new Variant(mt2, null, "zip")));
        assertTrue(verifyVariant(variants, new Variant(mt2, null, "identity")));
    }
    
    @Test
    public void testBuildTypeAndLang() {
        VariantListBuilderImpl vb = new VariantListBuilderImpl();
        MediaType mt1 = new MediaType("*", "*"); 
        MediaType mt2 = new MediaType("text", "xml");
        List<Variant> variants = vb.mediaTypes(mt1, mt2).languages(new Locale("en"), 
                                                                   new Locale("fr")).add().build();
        assertEquals("8 variants need to be created", 4, variants.size());
        assertTrue(verifyVariant(variants, new Variant(mt1, new Locale("en"), null)));
        assertTrue(verifyVariant(variants, new Variant(mt1, new Locale("fr"), null)));
        assertTrue(verifyVariant(variants, new Variant(mt2, new Locale("en"), null)));
        assertTrue(verifyVariant(variants, new Variant(mt2, new Locale("fr"), null)));
    }
    
    @Test
    public void testBuildLangAndEnc() {
        VariantListBuilderImpl vb = new VariantListBuilderImpl();
        List<Variant> variants = vb.languages(new Locale("en"), 
                                              new Locale("fr")).encodings("zip", "identity").add().build();
        assertEquals("4 variants need to be created", 4, variants.size());
        assertTrue(verifyVariant(variants, new Variant(null, new Locale("en"), "zip")));
        assertTrue(verifyVariant(variants, new Variant(null, new Locale("en"), "identity")));
        assertTrue(verifyVariant(variants, new Variant(null, new Locale("fr"), "zip")));
        assertTrue(verifyVariant(variants, new Variant(null, new Locale("fr"), "identity")));
    }
    
    @Test
    public void testBuildLang() {
        VariantListBuilderImpl vb = new VariantListBuilderImpl();
        List<Variant> variants = 
            vb.languages(new Locale("en"), new Locale("fr")).add().build();
        assertEquals("2 variants need to be created", 2, variants.size());
        assertTrue(verifyVariant(variants, new Variant(null, new Locale("en"), null)));
        assertTrue(verifyVariant(variants, new Variant(null, new Locale("en"), null)));
    }
    
    @Test
    public void testBuildEnc() {
        VariantListBuilderImpl vb = new VariantListBuilderImpl();
        List<Variant> variants = 
            vb.encodings("zip", "identity").add().build();
        assertEquals("2 variants need to be created", 2, variants.size());
        assertTrue(verifyVariant(variants, new Variant(null, null, "zip")));
        assertTrue(verifyVariant(variants, new Variant(null, null, "identity")));
    }
    
    @Test
    public void testBuildType() {
        VariantListBuilderImpl vb = new VariantListBuilderImpl();
        List<Variant> variants = 
            vb.mediaTypes(new MediaType("*", "*"), new MediaType("text", "xml")).add().build();
        assertEquals("2 variants need to be created", 2, variants.size());
        assertTrue(verifyVariant(variants, new Variant(new MediaType("*", "*"), null, null)));
        assertTrue(verifyVariant(variants, new Variant(new MediaType("text", "xml"), null, null)));
    }

    private boolean verifyVariant(List<Variant> vs, Variant var) {
        for (Variant v : vs) {
            
            if (v.getLanguage() == null
                && v.getEncoding() == null
                && v.getMediaType() == null) {
                return false;
            }
            boolean encodCheck = v.getEncoding() == null && var.getEncoding() == null
                                 || v.getEncoding().equals(var.getEncoding());
            boolean langCheck = v.getLanguage() == null && var.getLanguage() == null
                                || v.getLanguage().equals(var.getLanguage());
            boolean typeCheck = v.getMediaType() == null && var.getMediaType() == null
                                || v.getMediaType().equals(var.getMediaType());
            if (encodCheck && langCheck && typeCheck) {
                return true;
            }
        }
        
        return false;
    }
}
