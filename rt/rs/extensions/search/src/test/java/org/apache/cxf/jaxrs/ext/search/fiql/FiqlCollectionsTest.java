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
package org.apache.cxf.jaxrs.ext.search.fiql;

import java.time.LocalDate;
import java.util.Set;

import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchParseException;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class FiqlCollectionsTest {
    @Test
    public void testWithCollectionAfterFirstLevelOnCollection() throws SearchParseException {
        FiqlParser<Place> placeParser = new FiqlParser<>(Place.class);
        SearchCondition<Place> placeCondition = placeParser
                .parse("specs.features.description==description");
        Place place = placeCondition.getCondition();
        assertNotNull(place);
    }

    @Test
    public void testWithCollectionAfterFirstLevelOnSingleObject() throws SearchParseException {
        FiqlParser<Room> roomParser = new FiqlParser<>(Room.class);
        SearchCondition<Room> roomCondition = roomParser
                .parse("furniture.spec.features.description==description");
        Room room = roomCondition.getCondition();
        assertNotNull(room);
    }

    @Test
    public void testTemporalUsedOnCollection() throws SearchParseException {
        FiqlParser<Room> roomParser = new FiqlParser<>(Room.class);
        SearchCondition<Room> roomCondition = roomParser.parse("furniture.spec.features.localDate==2023-08-24");
        Room room = roomCondition.getCondition();
        assertNotNull(room);
    }

    public static class Room {
        Set<Furniture> furniture;
        public Set<Furniture> getFurniture() {
            return furniture;
        }
        public void setFurniture(Set<Furniture> furniture) {
            this.furniture = furniture;
        }
    }

    public static class Place {
        Set<Spec> specs;
        public Set<Spec> getSpecs() {
            return specs;
        }
        public void setSpecs(Set<Spec> specs) {
            this.specs = specs;
        }
    }

    public static class Furniture {
        Spec spec;
        public Spec getSpec() {
            return spec;
        }
        public void setSpec(Spec spec) {
            this.spec = spec;
        }
    }

    public static class Spec {
        Set<Feature> features;
        public Set<Feature> getFeatures() {
            return features;
        }
        public void setFeatures(Set<Feature> features) {
            this.features = features;
        }
    }

    public static class Feature {
        String description;
        LocalDate localDate;
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }

        public LocalDate getLocalDate() {
            return localDate;
        }

        public void setLocalDate(LocalDate localDate) {
            this.localDate = localDate;
        }
    }
}