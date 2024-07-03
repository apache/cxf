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

package org.apache.cxf.sts.claims.mapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;

/**
 * This claim util class provides methods to make the handling of claims and claim values easier. The input
 * claims (and their values) shall be treated as immutable. All util methods return a clone of the
 * provided claim containing the desired claim update.
 */
public class ClaimUtils {

    /**
     * @param collection Collection that should be used to add further claims to
     * @param claims Claims to be added to the provided collection
     * @return Returns clone of the provided collection including additional claims
     */
    public ProcessedClaimCollection add(ProcessedClaimCollection collection, ProcessedClaim... claims) {
        ProcessedClaimCollection resultClaimCollection = null;
        if (collection != null) {
            resultClaimCollection = (ProcessedClaimCollection)collection.clone();
            for (ProcessedClaim c : claims) {
                if (c != null) {
                    resultClaimCollection.add(c);
                }
            }
        }
        return resultClaimCollection;
    }

    /**
     * @param collection Collection that should be used to add claims from the other provided claim
     *            collections
     * @param claimCollections All claims contained within the provided collections will be added to the
     *            targetCollection
     * @return Returns a clone of the provided collection containing all claims from all other claimCollections
     */
    public ProcessedClaimCollection add(ProcessedClaimCollection collection,
        ProcessedClaimCollection... claimCollections) {
        ProcessedClaimCollection resultClaimCollection = null;
        if (collection != null) {
            resultClaimCollection = (ProcessedClaimCollection)collection.clone();
            for (ProcessedClaimCollection cc : claimCollections) {
                resultClaimCollection.addAll(cc);
            }
        }
        return resultClaimCollection;
    }

    /**
     * @param processedClaimTypeURI claim type URI
     * @param values values of created claim. Can be null if no values shall be added to claim.
     * @return Returns new claim with provided claim type and values
     */
    public ProcessedClaim create(String processedClaimTypeURI, String... values) {
        ProcessedClaim processedClaim = new ProcessedClaim();
        if (processedClaimTypeURI != null) {
            processedClaim.setClaimType(URI.create(processedClaimTypeURI));
        }
        if (values != null) {
            Collections.addAll(processedClaim.getValues(), values);
        }
        return processedClaim;
    }

    /**
     * @param processedClaims Collection of multiple claims with different claim types
     * @param processedClaimType URI of claim type to be selected from claim collection
     * @return Returns first claim from claims collection matching the provided claimType
     */
    public ProcessedClaim get(ProcessedClaimCollection processedClaims, String processedClaimType) {
        if (processedClaimType == null || processedClaims == null) {
            return null;
        }
        for (ProcessedClaim c : processedClaims) {
            if (c.getClaimType() != null && processedClaimType.equals(c.getClaimType())) {
                return c;
            }
        }
        return null;
    }

    /**
     * @param processedClaims Collection of claims to be mapped to a different claim type
     * @param map Map of old:new claim types
     * @param keepUnmapped if set to false only claims with a claim type contained in the map will be
     *            returned. If set to false claims with an unmapped claim type will also be returned.
     * @return Returns claim collection with mapped claim types
     */
    public ProcessedClaimCollection mapType(ProcessedClaimCollection processedClaims, Map<String, String> map,
        boolean keepUnmapped) {
        ProcessedClaimCollection mappedProcessedClaims = new ProcessedClaimCollection();
        if (processedClaims != null && map != null) {
            for (ProcessedClaim c : processedClaims) {
                String processedClaimType = (c.getClaimType() != null)
                    ? c.getClaimType()
                    : "";
                String mappedProcessedClaimType = map.get(processedClaimType);
                if (mappedProcessedClaimType != null) {
                    ProcessedClaim processedClaim = c.clone();
                    processedClaim.setClaimType(URI.create(mappedProcessedClaimType));
                    mappedProcessedClaims.add(processedClaim);
                } else if (keepUnmapped) {
                    mappedProcessedClaims.add(c.clone());
                }
            }
        }
        return mappedProcessedClaims;
    }

    /**
     * Mapping all values from the given claim according to the provided map. Input claims will not be
     * modified. Result claim will be a clone of the provided claims just with different (mapped) claim
     * values.
     *
     * @param processedClaim Claim providing values to be mapped
     * @param mapping Map of old:new mapping values
     * @param keepUnmapped if set to false only values contained in the map will be returned. If set to true,
     *            values not contained in the map will also remain in the returned claim.
     * @return Returns the provided claim with mapped values
     */
    public ProcessedClaim mapValues(ProcessedClaim processedClaim, Map<Object, Object> mapping, boolean keepUnmapped) {
        ProcessedClaim resultClaim = null;
        if (processedClaim != null) {
            resultClaim = processedClaim.clone();
            List<Object> values = resultClaim.getValues();
            List<Object> mappedValues = new ArrayList<>();

            if (values == null || mapping == null || mapping.isEmpty()) {
                resultClaim.setValues(mappedValues);
                return resultClaim;
            }

            for (Object value : values) {
                Object newValue = mapping.get(value);
                if (newValue != null) {
                    mappedValues.add(newValue);
                } else if (keepUnmapped) {
                    mappedValues.add(value);
                }
            }
            resultClaim.setValues(mappedValues);
        }
        return resultClaim;
    }

    /**
     * Filtering all values from the given claim according to the provided regex filter. Input claims will not
     * be modified. Result claim will be a clone of the provided claims just possible fewer (filtered) claim
     * values.
     *
     * @param processedClaim Claim containing arbitrary values
     * @param filter Regex filter to be used to match with claim values
     * @return Returns a claim containing only values from the processedClaim which matched the provided
     *         filter
     */
    public ProcessedClaim filterValues(ProcessedClaim processedClaim, String filter) {
        ProcessedClaim resultClaim = null;
        if (processedClaim != null) {
            resultClaim = processedClaim.clone();
            List<Object> values = resultClaim.getValues();
            List<Object> filteredValues = new ArrayList<>();

            if (values == null || filter == null) {
                resultClaim.setValues(filteredValues);
                return resultClaim;
            }

            for (Object value : values) {
                if (value != null && value.toString().matches(filter)) {
                    filteredValues.add(value);
                }
            }
            resultClaim.setValues(filteredValues);
        }
        return resultClaim;
    }

    /**
     * Merges the first value (only) from different claim types in a collection to a new claim type separated
     * by the provided delimiter.
     *
     * @param processedClaims Collection of claims containing claims with claim types of listed
     *            <code>claimType</code> array
     * @param targetClaimType claim type URI of merged result claim
     * @param delimiter Delimiter added between multiple claim types. Value can be <code>null</code>.
     * @param processedClaimType URIs of claim types to be merged. Merging will be in the same order as the
     *            provided claim type URIs. If a claim type is not found in the collection this claim type
     *            will be omitted.
     * @return Returns merged claim of all found claim types
     */
    public ProcessedClaim merge(ProcessedClaimCollection processedClaims, String targetClaimType, String delimiter,
        String... processedClaimType) {
        ProcessedClaim mergedProcessedClaim = null;
        StringBuilder sbProcessedClaimValue = new StringBuilder();
        for (String sc : processedClaimType) {
            ProcessedClaim c = get(processedClaims, sc);
            if (c != null) {
                List<Object> values = c.getValues();
                if (values != null && !values.isEmpty()) {
                    if (mergedProcessedClaim == null) {
                        // First match TODO refactor for better method override
                        mergedProcessedClaim = c.clone();
                        sbProcessedClaimValue.append(values.get(0));
                        mergedProcessedClaim.getValues().clear();
                    } else {
                        sbProcessedClaimValue.append(delimiter).append(values.get(0));
                    }
                }
            }
        }
        if (mergedProcessedClaim != null) {
            mergedProcessedClaim.setClaimType(URI.create(targetClaimType));
            mergedProcessedClaim.addValue(sbProcessedClaimValue.toString());
        }

        return mergedProcessedClaim;
    }

    /**
     * @param processedClaim Claim to be updated
     * @param processedClaimTypeURI URI as String to be set as claim type in provided claim
     * @return Returns updated claim
     */
    public ProcessedClaim setType(ProcessedClaim processedClaim, String processedClaimTypeURI) {
        if (processedClaim != null && processedClaimTypeURI != null) {
            processedClaim.setClaimType(URI.create(processedClaimTypeURI));
        }
        return processedClaim;
    }

    /**
     * All claims within the provided collection will be updated in the following manner: If no original
     * issuer is set, the issuer in the provided claims will be set as original issuer. If an original issuer
     * was already set before, the original issuer will not be updated. All claims will be updated to have the
     * provided issuer name be set as the claim issuer.
     *
     * @param processedClaims Collection of claims to be updated
     * @param newIssuer Issuer to be set for all claims within the collection
     * @return Returns a new claim collection with clones of updated claims
     */
    public ProcessedClaimCollection updateIssuer(ProcessedClaimCollection processedClaims, String newIssuer) {
        ProcessedClaimCollection resultClaimCollection = null;
        if (processedClaims != null) {
            resultClaimCollection = new ProcessedClaimCollection();
            for (ProcessedClaim c : processedClaims) {
                ProcessedClaim newClaim = c.clone();
                if (newClaim.getOriginalIssuer() == null) {
                    newClaim.setOriginalIssuer(newClaim.getIssuer());
                }
                newClaim.setIssuer(newIssuer);
                resultClaimCollection.add(newClaim);
            }
        }
        return resultClaimCollection;
    }

    /**
     * @param processedClaim values of this claim will be used for result claim
     * @return Returns clone of the provided claim with values all in uppercase format
     */
    public ProcessedClaim upperCaseValues(ProcessedClaim processedClaim) {
        ProcessedClaim resultClaim = null;
        if (processedClaim != null) {
            resultClaim = processedClaim.clone();
            if (resultClaim.getValues() != null) {
                List<Object> oldValues = resultClaim.getValues();
                List<Object> newValues = new ArrayList<>();
                for (Object value : oldValues) {
                    newValues.add(value.toString().toUpperCase());
                }
                resultClaim.getValues().clear();
                resultClaim.getValues().addAll(newValues);
            }
        }
        return resultClaim;
    }

    /**
     * @param processedClaim values of this claim will be used for result claim
     * @return Returns clone of provided claim with values all in lowercase format
     */
    public ProcessedClaim lowerCaseValues(ProcessedClaim processedClaim) {
        ProcessedClaim resultClaim = null;
        if (processedClaim != null) {
            resultClaim = processedClaim.clone();
            if (resultClaim.getValues() != null) {
                List<Object> oldValues = resultClaim.getValues();
                List<Object> newValues = new ArrayList<>();
                for (Object value : oldValues) {
                    newValues.add(value.toString().toLowerCase());
                }
                resultClaim.getValues().clear();
                resultClaim.getValues().addAll(newValues);
            }
        }
        return resultClaim;
    }

    /**
     * @param processedClaim Claim providing values to be wrapped
     * @param prefix Prefix to be added to each claim value. Can be null.
     * @param suffix Suffix to be appended to each claim value. Can be null.
     * @return Returns a clone of the the provided claim with wrapped values
     */
    public ProcessedClaim wrapValues(ProcessedClaim processedClaim, String prefix, String suffix) {
        prefix = (prefix == null)
            ? ""
            : prefix;
        suffix = (suffix == null)
            ? ""
            : suffix;
        ProcessedClaim resultClaim = null;
        if (processedClaim != null) {
            resultClaim = processedClaim.clone();
            if (resultClaim.getValues() != null) {
                List<Object> oldValues = resultClaim.getValues();
                List<Object> newValues = new ArrayList<>();
                for (Object value : oldValues) {
                    newValues.add(prefix + value.toString() + suffix);
                }
                resultClaim.getValues().clear();
                resultClaim.getValues().addAll(newValues);
            }
        }
        return resultClaim;
    }

    /**
     * This function is especially useful if multi values from a claim are stored within a single value entry.
     * For example multi user roles could all be stored in a single value element separated by comma:
     * USER,MANAGER,ADMIN The result of this function will provide a claim with three distinct values: USER
     * and MANAGER and ADMIN.
     *
     * @param processedClaim claim containing multi-values in a single value entry
     * @param delimiter Delimiter to split multi-values into single values
     * @return Returns a clone of the provided claim containing only single values per value entry
     */
    public ProcessedClaim singleToMultiValue(ProcessedClaim processedClaim, String delimiter) {
        ProcessedClaim resultClaim = null;
        if (processedClaim != null) {
            resultClaim = processedClaim.clone();
            if (resultClaim.getValues() != null) {
                List<Object> oldValues = resultClaim.getValues();
                List<Object> newValues = new ArrayList<>();
                for (Object value : oldValues) {
                    String multivalue = value.toString();
                    StringTokenizer st = new StringTokenizer(multivalue, delimiter);
                    while (st.hasMoreTokens()) {
                        newValues.add(st.nextToken());
                    }
                }
                resultClaim.getValues().clear();
                resultClaim.getValues().addAll(newValues);
            }
        }
        return resultClaim;
    }

    /**
     * This function is especially useful if values from multiple claim values need to be condensed into a
     * single value element. For example a user has three roles: USER and MANAGER and ADMIN. If ',' is used as
     * a delimiter, then this method would provide the following claim with only a single value looking like
     * this: USER,MANAGER,ADMIN
     *
     * @param processedClaim claim containing multi-values
     * @param delimiter Delimiter to concatenate multi-values into a single value
     * @return Returns a clone of the provided claim containing only one single value
     */
    public ProcessedClaim multiToSingleValue(ProcessedClaim processedClaim, String delimiter) {
        ProcessedClaim resultClaim = null;
        if (processedClaim != null) {
            resultClaim = processedClaim.clone();
            if (resultClaim.getValues() != null) {
                List<Object> oldValues = resultClaim.getValues();
                boolean first = true;
                StringBuilder sb = new StringBuilder();
                for (Object value : oldValues) {
                    if (first) {
                        sb.append(value);
                        first = false;
                    } else {
                        sb.append(delimiter).append(value);
                    }
                }
                resultClaim.getValues().clear();
                resultClaim.getValues().add(sb.toString());
            }
        }
        return resultClaim;
    }

    /**
     * This function removes duplicated values.
     *
     * @param processedClaim claim containing multi-values of which some might be duplicated
     * @return Returns a clone of the provided claim containing only distinct values
     */
    public ProcessedClaim distinctValues(ProcessedClaim processedClaim) {
        ProcessedClaim resultClaim = null;
        if (processedClaim != null) {
            resultClaim = processedClaim.clone();
            if (resultClaim.getValues() != null) {
                List<Object> oldValues = resultClaim.getValues();
                Set<Object> distincValues = new LinkedHashSet<>(oldValues);
                resultClaim.getValues().clear();
                resultClaim.getValues().addAll(distincValues);
            }
        }
        return resultClaim;
    }

    /**
     * Removes Claims without values.
     *
     * @param processedClaims Collection of claims with and/or without values
     * @return Returns a collection of claims which contain values only
     */
    public ProcessedClaimCollection removeEmptyClaims(ProcessedClaimCollection processedClaims) {
        ProcessedClaimCollection resultClaimCollection = null;
        if (processedClaims != null) {
            resultClaimCollection = new ProcessedClaimCollection();
            for (ProcessedClaim c : processedClaims) {
                if (c.getValues() != null && c.getValues().size() > 0) {
                    resultClaimCollection.add(c);
                }
            }
        }
        return resultClaimCollection;
    }
}
