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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;

public class ClaimUtils {

    /**
     * @param targetCollection Collection that should be used to add further claims to
     * @param claims Claims to be added to the provided collection
     * @return Returns provided collection including provided claims
     */
    public ProcessedClaimCollection add(ProcessedClaimCollection targetCollection, ProcessedClaim... claims) {
        for (ProcessedClaim c : claims) {
            if (c != null) {
                targetCollection.add(c);
            }
        }
        return targetCollection;
    }

    /**
     * @param targetCollection Collection that should be used to add claims from the other provided claim
     *            collections
     * @param claimCollections All claims contained within the provided collections will be added to the
     *            targetCollection
     * @return Returns claim collection containing all claims that have been provided
     */
    public ProcessedClaimCollection add(ProcessedClaimCollection targetCollection,
        ProcessedClaimCollection... claimCollections) {
        for (ProcessedClaimCollection cc : claimCollections) {
            targetCollection.addAll(cc);
        }
        return targetCollection;
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
            processedClaim.getValues().addAll(Arrays.asList(values));
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
            if (c.getClaimType() != null && processedClaimType.equals(c.getClaimType().toString())) {
                return c;
            }
        }
        return null;
    }

    /**
     * @param processedClaims Collection of claims to be mapped to a differnt claim type
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
                    ? c.getClaimType().toString()
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
     * @param processedClaim Claim providing values to be mapped
     * @param map Map of old:new mapping values
     * @param keepUnmapped if set to false only values contained in the map will be returned. If set to true,
     *            values not contained in the map will also remain in the returned claim.
     * @return Returns the provided claim with mapped values
     */
    public ProcessedClaim mapValues(ProcessedClaim processedClaim, Map<Object, Object> mapping, boolean keepUnmapped) {

        if (processedClaim != null) {
            List<Object> values = processedClaim.getValues();
            List<Object> mappedValues = new ArrayList<Object>();

            if (values == null || mapping == null || mapping.size() == 0) {
                processedClaim.setValues(mappedValues);
                return processedClaim;
            }

            for (Object value : values) {
                Object newValue = mapping.get(value);
                if (newValue != null) {
                    mappedValues.add(newValue);
                } else if (keepUnmapped) {
                    mappedValues.add(value);
                }
            }
            processedClaim.setValues(mappedValues);
        }
        return processedClaim;
    }
    
    /**
     * @param processedClaim Claim containing arbitrary values 
     * @param filter Regex filter to be used to match with claim values
     * @return Returns a claim containing only values from the processedClaim which matched the provided filter
     */
    public ProcessedClaim filterValues(ProcessedClaim processedClaim, String filter) {

        if (processedClaim != null) {
            List<Object> values = processedClaim.getValues();
            List<Object> filteredValues = new ArrayList<Object>();

            if (values == null || filter == null) {
                processedClaim.setValues(filteredValues);
                return processedClaim;
            }

            for (Object value : values) {
                if (value != null && value.toString().matches(filter)) {
                    filteredValues.add(value);
                }
            }
            processedClaim.setValues(filteredValues);
        }
        return processedClaim;
    }

    /**
     * @param processedClaims Collection of claims containing claims with claim types of listed <code>claimType</code>
     *            array
     * @param targetClaimType claim type URI of merged result claim
     * @param delimiter Delimiter added between multiple claim types. Value can be <code>null</code>.
     * @param processedClaimType URIs of claim types to be merged. Merging will be in the same order as the provided
     *            claim type URIs. If a claim type is not found in the collection this claim type will be
     *            omitted.
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
                if (values != null && values.size() > 0) {
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
        processedClaim.setClaimType(URI.create(processedClaimTypeURI));
        return processedClaim;
    }

    /**
     * All claims within the provided collection will be updated in the following manner: If no original
     * issuer is set, the issuer in the provided claims will be set as original issuer. If an original issuer
     * was already set before, the original issuer will not be updated. All claims will be updated to have the
     * provided issuer name be set as the claim issuer.
     * 
     * @param processedClaims Collection of claims to be updated
     * @param issuerName Issuer to be set for all claims within the collection
     * @return Returns updated claim collection
     */
    public ProcessedClaimCollection updateIssuer(ProcessedClaimCollection processedClaims, String newIssuer) {
        for (ProcessedClaim c : processedClaims) {
            if (c.getOriginalIssuer() == null) {
                c.setOriginalIssuer(c.getIssuer());
            }
            c.setIssuer(newIssuer);
        }
        return processedClaims;
    }

    /**
     * @param processedClaim values of this claim will be transformed to uppercase format
     * @return Returns claim with values all in uppercase format
     */
    public ProcessedClaim upperCaseValues(ProcessedClaim processedClaim) {
        if (processedClaim != null && processedClaim.getValues() != null) {
            List<Object> oldValues = processedClaim.getValues();
            List<Object> newValues = new ArrayList<Object>();
            for (Object value : oldValues) {
                newValues.add(value.toString().toUpperCase());
            }
            processedClaim.getValues().clear();
            processedClaim.getValues().addAll(newValues);
        }
        return processedClaim;
    }

    /**
     * @param processedClaim values of this claim will be transformed to lowercase format
     * @return Returns claim with values all in lowercase format
     */
    public ProcessedClaim lowerCaseValues(ProcessedClaim processedClaim) {
        if (processedClaim != null && processedClaim.getValues() != null) {
            List<Object> oldValues = processedClaim.getValues();
            List<Object> newValues = new ArrayList<Object>();
            for (Object value : oldValues) {
                newValues.add(value.toString().toLowerCase());
            }
            processedClaim.getValues().clear();
            processedClaim.getValues().addAll(newValues);
        }
        return processedClaim;
    }

    /**
     * @param processedClaim Claim providing values to be wrapped
     * @param prefix Prefix to be added to each claim value. Can be null.
     * @param suffix Suffix to be appended to each claim value. Can be null.
     * @return Returns the provided claim with wrapped values
     */
    public ProcessedClaim wrapValues(ProcessedClaim processedClaim, String prefix, String suffix) {
        prefix = (prefix == null)
            ? ""
            : prefix;
        suffix = (suffix == null)
            ? ""
            : suffix;
        if (processedClaim != null && processedClaim.getValues() != null) {
            List<Object> oldValues = processedClaim.getValues();
            List<Object> newValues = new ArrayList<Object>();
            for (Object value : oldValues) {
                newValues.add(prefix + value.toString() + suffix);
            }
            processedClaim.getValues().clear();
            processedClaim.getValues().addAll(newValues);
        }
        return processedClaim;
    }

    /**
     * This function is especially useful if multi values from a claim are stored within a single value entry.
     * 
     * For example multi user roles could all be stored in a single value element separated by comma:
     * USER,MANAGER,ADMIN 
     * The result of this function will provide a claim with three distinct values: 
     * USER and MANAGER and ADMIN.
     * 
     * @param processedClaim claim containing multi-values in a single value entry
     * @param delimiter Delimiter to split multi-values into single values
     * @return Returns a Processed Claim containing only single values per value entry
     */
    public ProcessedClaim singleToMultiValue(ProcessedClaim processedClaim, String delimiter) {
        if (processedClaim != null && processedClaim.getValues() != null) {
            List<Object> oldValues = processedClaim.getValues();
            List<Object> newValues = new ArrayList<Object>();
            for (Object value : oldValues) {
                String multivalue = value.toString();
                StringTokenizer st = new StringTokenizer(multivalue, delimiter);
                while (st.hasMoreTokens()) {
                    newValues.add(st.nextToken());
                }
            }
            processedClaim.getValues().clear();
            processedClaim.getValues().addAll(newValues);
        }
        return processedClaim;
    }
    
    /**
     * This function is especially useful if values from multiple claim values need to be condensed into a
     * single value element. 
     * 
     * For example a user has three roles: USER and MANAGER and ADMIN. If ',' is used as a delimiter, 
     * then this method would provide the following claim with only a single value looking like this:
     * USER,MANAGER,ADMIN
     * 
     * @param processedClaim claim containing multi-values
     * @param delimiter Delimiter to concatenate multi-values into a single value
     * @return Returns a Processed Claim containing only one single value
     */
    public ProcessedClaim multiToSingleValue(ProcessedClaim processedClaim, String delimiter) {
        if (processedClaim != null && processedClaim.getValues() != null) {
            List<Object> oldValues = processedClaim.getValues();
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
            processedClaim.getValues().clear();
            processedClaim.getValues().add(sb.toString());
        }
        return processedClaim;
    }
}
