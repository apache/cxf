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

package org.apache.cxf.jaxrs.utils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.MediaType;

public class AccumulatingIntersector implements MimeTypesIntersector {
    private static final String MEDIA_TYPE_DISTANCE_PARAM = "d";
    private final Set<MediaType> supportedMimeTypeList = new LinkedHashSet<>();
    private final boolean addRequiredParamsIfPossible;
    private final boolean addDistanceParameter;

    AccumulatingIntersector(boolean addRequiredParamsIfPossible, boolean addDistanceParameter) {
        this.addRequiredParamsIfPossible = addRequiredParamsIfPossible;
        this.addDistanceParameter = addDistanceParameter;
    }

    @Override
    public boolean intersect(MediaType requiredType, MediaType userType) {
        boolean requiredTypeWildcard = requiredType.getType().equals(MediaType.MEDIA_TYPE_WILDCARD);
        boolean requiredSubTypeWildcard = requiredType.getSubtype().contains(MediaType.MEDIA_TYPE_WILDCARD);

        String type = requiredTypeWildcard ? userType.getType() : requiredType.getType();
        String subtype = requiredSubTypeWildcard ? userType.getSubtype() : requiredType.getSubtype();

        Map<String, String> parameters = userType.getParameters();
        if (addRequiredParamsIfPossible) {
            parameters = new LinkedHashMap<>(parameters);
            for (Map.Entry<String, String> entry : requiredType.getParameters().entrySet()) {
                if (!parameters.containsKey(entry.getKey())) {
                    parameters.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (addDistanceParameter) {
            int distance = 0;
            if (requiredTypeWildcard) {
                distance++;
            }
            if (requiredSubTypeWildcard) {
                distance++;
            }
            parameters.put(MEDIA_TYPE_DISTANCE_PARAM, Integer.toString(distance));
        }
        getSupportedMimeTypeList().add(new MediaType(type, subtype, parameters));
        return true;
    }

    public Set<MediaType> getSupportedMimeTypeList() {
        return supportedMimeTypeList;
    }
}
