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
package org.apache.cxf.rs.security.jose.cookbook;

import java.io.InputStream;
import java.util.List;


import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.EcDsaJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJsonConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJsonProducer;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JwsJoseCookBookTest {
    private static final String PAYLOAD = "It’s a dangerous business, Frodo, going out your door. "
        + "You step onto the road, and if you don't keep your feet, "
        + "there’s no knowing where you might be swept off to.";
    private static final String ENCODED_PAYLOAD = "SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywgZ29pbmcgb3V0IH"
        + "lvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9hZCwgYW5kIGlmIHlvdSBk"
        + "b24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXigJlzIG5vIGtub3dpbmcgd2hlcm"
        + "UgeW91IG1pZ2h0IGJlIHN3ZXB0IG9mZiB0by4";
    private static final String RSA_KID_VALUE = "bilbo.baggins@hobbiton.example";
    private static final String RSA_V1_5_SIGNATURE_PROTECTED_HEADER =
          "eyJhbGciOiJSUzI1NiIsImtpZCI6ImJpbGJvLmJhZ2dpbnNAaG9iYml0b24uZX"
        + "hhbXBsZSJ9";
    private static final String RSA_V1_5_SIGNATURE_PROTECTED_HEADER_JSON = ("{"
        + "\"alg\": \"RS256\","
        + "\"kid\": \"bilbo.baggins@hobbiton.example\""
        + "}").replaceAll(" ", "");
    private static final String RSA_V1_5_SIGNATURE_VALUE =
          "MRjdkly7_-oTPTS3AXP41iQIGKa80A0ZmTuV5MEaHoxnW2e5CZ5NlKtainoFmK"
        + "ZopdHM1O2U4mwzJdQx996ivp83xuglII7PNDi84wnB-BDkoBwA78185hX-Es4J"
        + "IwmDLJK3lfWRa-XtL0RnltuYv746iYTh_qHRD68BNt1uSNCrUCTJDt5aAE6x8w"
        + "W1Kt9eRo4QPocSadnHXFxnt8Is9UzpERV0ePPQdLuW3IS_de3xyIrDaLGdjluP"
        + "xUAhb6L2aXic1U12podGU0KLUQSE_oI-ZnmKJ3F4uOZDnd6QZWJushZ41Axf_f"
        + "cIe8u9ipH84ogoree7vjbU5y18kDquDg";
    private static final String RSA_V1_5_JSON_GENERAL_SERIALIZATION = ("{"
        + "\"payload\": \"SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywg"
        + "Z29pbmcgb3V0IHlvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9h"
        + "ZCwgYW5kIGlmIHlvdSBkb24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXi"
        + "gJlzIG5vIGtub3dpbmcgd2hlcmUgeW91IG1pZ2h0IGJlIHN3ZXB0IG9m"
        + "ZiB0by4\","
        + "\"signatures\": ["
        + "{"
        + "\"protected\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6ImJpbGJvLmJhZ2"
        + "dpbnNAaG9iYml0b24uZXhhbXBsZSJ9\","
        + "\"signature\": \"MRjdkly7_-oTPTS3AXP41iQIGKa80A0ZmTuV5MEaHo"
        + "xnW2e5CZ5NlKtainoFmKZopdHM1O2U4mwzJdQx996ivp83xuglII"
        + "7PNDi84wnB-BDkoBwA78185hX-Es4JIwmDLJK3lfWRa-XtL0Rnlt"
        + "uYv746iYTh_qHRD68BNt1uSNCrUCTJDt5aAE6x8wW1Kt9eRo4QPo"
        + "cSadnHXFxnt8Is9UzpERV0ePPQdLuW3IS_de3xyIrDaLGdjluPxU"
        + "Ahb6L2aXic1U12podGU0KLUQSE_oI-ZnmKJ3F4uOZDnd6QZWJush"
        + "Z41Axf_fcIe8u9ipH84ogoree7vjbU5y18kDquDg\""
        + "}"
        + "]"
        + "}").replaceAll(" ", "");
    private static final String RSA_V1_5_JSON_FLATTENED_SERIALIZATION = ("{"
        + "\"payload\": \"SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywg"
        + "Z29pbmcgb3V0IHlvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9h"
        + "ZCwgYW5kIGlmIHlvdSBkb24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXi"
        + "gJlzIG5vIGtub3dpbmcgd2hlcmUgeW91IG1pZ2h0IGJlIHN3ZXB0IG9m"
        + "ZiB0by4\","
        + "\"protected\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6ImJpbGJvLmJhZ2dpbn"
        + "NAaG9iYml0b24uZXhhbXBsZSJ9\","
        + "\"signature\": \"MRjdkly7_-oTPTS3AXP41iQIGKa80A0ZmTuV5MEaHoxnW2"
        + "e5CZ5NlKtainoFmKZopdHM1O2U4mwzJdQx996ivp83xuglII7PNDi84w"
        + "nB-BDkoBwA78185hX-Es4JIwmDLJK3lfWRa-XtL0RnltuYv746iYTh_q"
        + "HRD68BNt1uSNCrUCTJDt5aAE6x8wW1Kt9eRo4QPocSadnHXFxnt8Is9U"
        + "zpERV0ePPQdLuW3IS_de3xyIrDaLGdjluPxUAhb6L2aXic1U12podGU0"
        + "KLUQSE_oI-ZnmKJ3F4uOZDnd6QZWJushZ41Axf_fcIe8u9ipH84ogore"
        + "e7vjbU5y18kDquDg\""
        + "}").replaceAll(" ", "");
    private static final String RSA_PSS_SIGNATURE_PROTECTED_HEADER_JSON = ("{"
        + "\"alg\": \"PS384\","
        + "\"kid\": \"bilbo.baggins@hobbiton.example\""
        + "}").replaceAll(" ", "");
    private static final String RSA_PSS_SIGNATURE_PROTECTED_HEADER =
          "eyJhbGciOiJQUzM4NCIsImtpZCI6ImJpbGJvLmJhZ2dpbnNAaG9iYml0b24uZX"
        + "hhbXBsZSJ9";
    private static final String RSA_PSS_SIGNATURE_VALUE =
          "cu22eBqkYDKgIlTpzDXGvaFfz6WGoz7fUDcfT0kkOy42miAh2qyBzk1xEsnk2I"
        + "pN6-tPid6VrklHkqsGqDqHCdP6O8TTB5dDDItllVo6_1OLPpcbUrhiUSMxbbXU"
        + "vdvWXzg-UD8biiReQFlfz28zGWVsdiNAUf8ZnyPEgVFn442ZdNqiVJRmBqrYRX"
        + "e8P_ijQ7p8Vdz0TTrxUeT3lm8d9shnr2lfJT8ImUjvAA2Xez2Mlp8cBE5awDzT"
        + "0qI0n6uiP1aCN_2_jLAeQTlqRHtfa64QQSUmFAAjVKPbByi7xho0uTOcbH510a"
        + "6GYmJUAfmWjwZ6oD4ifKo8DYM-X72Eaw";
    private static final String RSA_PSS_JSON_GENERAL_SERIALIZATION = ("{"
        + "\"payload\": \"SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywg"
        + "Z29pbmcgb3V0IHlvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9h"
        + "ZCwgYW5kIGlmIHlvdSBkb24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXi"
        + "gJlzIG5vIGtub3dpbmcgd2hlcmUgeW91IG1pZ2h0IGJlIHN3ZXB0IG9m"
        + "ZiB0by4\","
        + "\"signatures\": ["
        + "{"
        + "\"protected\": \"eyJhbGciOiJQUzM4NCIsImtpZCI6ImJpbGJvLmJhZ2"
        + "dpbnNAaG9iYml0b24uZXhhbXBsZSJ9\","
        + "\"signature\": \"cu22eBqkYDKgIlTpzDXGvaFfz6WGoz7fUDcfT0kkOy"
        + "42miAh2qyBzk1xEsnk2IpN6-tPid6VrklHkqsGqDqHCdP6O8TTB5"
        + "dDDItllVo6_1OLPpcbUrhiUSMxbbXUvdvWXzg-UD8biiReQFlfz2"
        + "8zGWVsdiNAUf8ZnyPEgVFn442ZdNqiVJRmBqrYRXe8P_ijQ7p8Vd"
        + "z0TTrxUeT3lm8d9shnr2lfJT8ImUjvAA2Xez2Mlp8cBE5awDzT0q"
        + "I0n6uiP1aCN_2_jLAeQTlqRHtfa64QQSUmFAAjVKPbByi7xho0uT"
        + "OcbH510a6GYmJUAfmWjwZ6oD4ifKo8DYM-X72Eaw\""
        + "}"
        + "]"
        + "}").replace(" ", "");
    private static final String RSA_PSS_JSON_FLATTENED_SERIALIZATION = ("{"
        + "\"payload\": \"SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywg"
        + "Z29pbmcgb3V0IHlvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9h"
        + "ZCwgYW5kIGlmIHlvdSBkb24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXi"
        + "gJlzIG5vIGtub3dpbmcgd2hlcmUgeW91IG1pZ2h0IGJlIHN3ZXB0IG9m"
        + "ZiB0by4\","
        + "\"protected\": \"eyJhbGciOiJQUzM4NCIsImtpZCI6ImJpbGJvLmJhZ2dpbn"
        + "NAaG9iYml0b24uZXhhbXBsZSJ9\","
        + "\"signature\": \"cu22eBqkYDKgIlTpzDXGvaFfz6WGoz7fUDcfT0kkOy42mi"
        + "Ah2qyBzk1xEsnk2IpN6-tPid6VrklHkqsGqDqHCdP6O8TTB5dDDItllV"
        + "o6_1OLPpcbUrhiUSMxbbXUvdvWXzg-UD8biiReQFlfz28zGWVsdiNAUf"
        + "8ZnyPEgVFn442ZdNqiVJRmBqrYRXe8P_ijQ7p8Vdz0TTrxUeT3lm8d9s"
        + "hnr2lfJT8ImUjvAA2Xez2Mlp8cBE5awDzT0qI0n6uiP1aCN_2_jLAeQT"
        + "lqRHtfa64QQSUmFAAjVKPbByi7xho0uTOcbH510a6GYmJUAfmWjwZ6oD"
        + "4ifKo8DYM-X72Eaw\""
        + "}").replace(" ", "");
    private static final String ECDSA_KID_VALUE = "bilbo.baggins@hobbiton.example";
    private static final String ECDSA_SIGNATURE_PROTECTED_HEADER_JSON = ("{"
        + "\"alg\": \"ES512\","
        + "\"kid\": \"bilbo.baggins@hobbiton.example\""
        + "}").replace(" ", "");
    private static final String ECSDA_SIGNATURE_PROTECTED_HEADER =
              "eyJhbGciOiJFUzUxMiIsImtpZCI6ImJpbGJvLmJhZ2dpbnNAaG9iYml0b24uZX"
            + "hhbXBsZSJ9";
    private static final String HMAC_KID_VALUE = "018c0ae5-4d9b-471b-bfd6-eef314bc7037";
    private static final String HMAC_SIGNATURE_PROTECTED_HEADER_JSON = ("{"
        + "\"alg\": \"HS256\","
        + "\"kid\": \"018c0ae5-4d9b-471b-bfd6-eef314bc7037\""
        + "}").replaceAll(" ", "");
    private static final String HMAC_SIGNATURE_PROTECTED_HEADER =
          "eyJhbGciOiJIUzI1NiIsImtpZCI6IjAxOGMwYWU1LTRkOWItNDcxYi1iZmQ2LW"
        + "VlZjMxNGJjNzAzNyJ9";
    private static final String HMAC_SIGNATURE_VALUE = "s0h6KThzkfBBBkLspW1h84VsJZFTsPPqMDA7g1Md7p0";
    private static final String HMAC_JSON_GENERAL_SERIALIZATION = ("{"
        + "\"payload\": \"SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywg"
        + "Z29pbmcgb3V0IHlvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9h"
        + "ZCwgYW5kIGlmIHlvdSBkb24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXi"
        + "gJlzIG5vIGtub3dpbmcgd2hlcmUgeW91IG1pZ2h0IGJlIHN3ZXB0IG9m"
        + "ZiB0by4\","
        + "\"signatures\": ["
        + "{"
        + "\"protected\": \"eyJhbGciOiJIUzI1NiIsImtpZCI6IjAxOGMwYWU1LT"
        + "RkOWItNDcxYi1iZmQ2LWVlZjMxNGJjNzAzNyJ9\","
        + "\"signature\": \"s0h6KThzkfBBBkLspW1h84VsJZFTsPPqMDA7g1Md7p"
        + "0\""
        + "}"
        + "]"
        + "}").replaceAll(" ", "");
    private static final String HMAC_JSON_FLATTENED_SERIALIZATION = ("{"
        + "\"payload\": \"SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywg"
        + "Z29pbmcgb3V0IHlvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9h"
        + "ZCwgYW5kIGlmIHlvdSBkb24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXi"
        + "gJlzIG5vIGtub3dpbmcgd2hlcmUgeW91IG1pZ2h0IGJlIHN3ZXB0IG9m"
        + "ZiB0by4\","
        + "\"protected\": \"eyJhbGciOiJIUzI1NiIsImtpZCI6IjAxOGMwYWU1LTRkOW"
        + "ItNDcxYi1iZmQ2LWVlZjMxNGJjNzAzNyJ9\","
        + "\"signature\": \"s0h6KThzkfBBBkLspW1h84VsJZFTsPPqMDA7g1Md7p0\""
        + "}").replaceAll(" ", "");
    private static final String DETACHED_HMAC_JWS =
          ("eyJhbGciOiJIUzI1NiIsImtpZCI6IjAxOGMwYWU1LTRkOWItNDcxYi1iZmQ2LW"
        + "VlZjMxNGJjNzAzNyJ9"
        + "."
        + "."
        + "s0h6KThzkfBBBkLspW1h84VsJZFTsPPqMDA7g1Md7p0").replaceAll(" ", "");
    private static final String HMAC_DETACHED_JSON_GENERAL_SERIALIZATION = ("{"
        + "\"signatures\": ["
        + "{"
        + "\"protected\": \"eyJhbGciOiJIUzI1NiIsImtpZCI6IjAxOGMwYWU1LT"
        + "RkOWItNDcxYi1iZmQ2LWVlZjMxNGJjNzAzNyJ9\","
        + "\"signature\": \"s0h6KThzkfBBBkLspW1h84VsJZFTsPPqMDA7g1Md7p"
        + "0\""
        + "}"
        + "]"
        + "}").replaceAll(" ", "");
    private static final String HMAC_DETACHED_JSON_FLATTENED_SERIALIZATION = ("{"
        + "\"protected\": \"eyJhbGciOiJIUzI1NiIsImtpZCI6IjAxOGMwYWU1LTRkOW"
        + "ItNDcxYi1iZmQ2LWVlZjMxNGJjNzAzNyJ9\","
        + "\"signature\": \"s0h6KThzkfBBBkLspW1h84VsJZFTsPPqMDA7g1Md7p0\""
        + "}").replaceAll(" ", "");
    private static final String PROTECTING_SPECIFIC_HEADER_FIELDS_JSON_GENERAL_SERIALIZATION = ("{"
        + "\"payload\": \"SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywg"
        + "Z29pbmcgb3V0IHlvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9h"
        + "ZCwgYW5kIGlmIHlvdSBkb24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXi"
        + "gJlzIG5vIGtub3dpbmcgd2hlcmUgeW91IG1pZ2h0IGJlIHN3ZXB0IG9m"
        + "ZiB0by4\","
        + "\"signatures\": ["
        + "{"
        + "\"protected\": \"eyJhbGciOiJIUzI1NiJ9\","
        + "\"header\": {"
        + "\"kid\": \"018c0ae5-4d9b-471b-bfd6-eef314bc7037\""
        + "},"
        + "\"signature\": \"bWUSVaxorn7bEF1djytBd0kHv70Ly5pvbomzMWSOr2"
        + "0\""
        + "}"
        + "]"
        + "}").replace(" ", "");
    private static final String PROTECTING_SPECIFIC_HEADER_FIELDS_JSON_FLATTENED_SERIALIZATION = ("{"
        + "\"payload\": \"SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywg"
        + "Z29pbmcgb3V0IHlvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9h"
        + "ZCwgYW5kIGlmIHlvdSBkb24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXi"
        + "gJlzIG5vIGtub3dpbmcgd2hlcmUgeW91IG1pZ2h0IGJlIHN3ZXB0IG9m"
        + "ZiB0by4\","
        + "\"protected\": \"eyJhbGciOiJIUzI1NiJ9\","
        + "\"header\": {"
        + "\"kid\": \"018c0ae5-4d9b-471b-bfd6-eef314bc7037\""
        + "},"
        + "\"signature\": \"bWUSVaxorn7bEF1djytBd0kHv70Ly5pvbomzMWSOr20\""
        + "}").replace(" ", "");
    private static final String PROTECTING_CONTENT_ONLY_JSON_GENERAL_SERIALIZATION = ("{"
        + "\"payload\": \"SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywg"
        + "Z29pbmcgb3V0IHlvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9h"
        + "ZCwgYW5kIGlmIHlvdSBkb24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXi"
        + "gJlzIG5vIGtub3dpbmcgd2hlcmUgeW91IG1pZ2h0IGJlIHN3ZXB0IG9m"
        + "ZiB0by4\","
        + "\"signatures\": ["
        + "{"
        + "\"header\": {"
        + "\"alg\": \"HS256\","
        + "\"kid\": \"018c0ae5-4d9b-471b-bfd6-eef314bc7037\""
        + "},"
        + "\"signature\": \"xuLifqLGiblpv9zBpuZczWhNj1gARaLV3UxvxhJxZu"
        + "k\""
        + "}"
        + "]"
        + "}").replace(" ", "");
    private static final String PROTECTING_CONTENT_ONLY_JSON_FLATTENED_SERIALIZATION = ("{"
        + "\"payload\": \"SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywg"
        + "Z29pbmcgb3V0IHlvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9h"
        + "ZCwgYW5kIGlmIHlvdSBkb24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXi"
        + "gJlzIG5vIGtub3dpbmcgd2hlcmUgeW91IG1pZ2h0IGJlIHN3ZXB0IG9m"
        + "ZiB0by4\","
        + "\"header\": {"
        + "\"alg\": \"HS256\","
        + "\"kid\": \"018c0ae5-4d9b-471b-bfd6-eef314bc7037\""
        + "},"
        + "\"signature\": \"xuLifqLGiblpv9zBpuZczWhNj1gARaLV3UxvxhJxZuk\""
        + "}").replace(" ", "");
    private static final String FIRST_SIGNATURE_ENTRY_MULTIPLE_SIGNATURES = ("{"
        + "\"protected\": \"eyJhbGciOiJSUzI1NiJ9\","
        + "\"header\": {"
        + "\"kid\": \"bilbo.baggins@hobbiton.example\""
        + "},"
        + "\"signature\": \"MIsjqtVlOpa71KE-Mss8_Nq2YH4FGhiocsqrgi5NvyG53u"
        + "oimic1tcMdSg-qptrzZc7CG6Svw2Y13TDIqHzTUrL_lR2ZFcryNFiHkS"
        + "w129EghGpwkpxaTn_THJTCglNbADko1MZBCdwzJxwqZc-1RlpO2HibUY"
        + "yXSwO97BSe0_evZKdjvvKSgsIqjytKSeAMbhMBdMma622_BG5t4sdbuC"
        + "HtFjp9iJmkio47AIwqkZV1aIZsv33uPUqBBCXbYoQJwt7mxPftHmNlGo"
        + "OSMxR_3thmXTCm4US-xiNOyhbm8afKK64jU6_TPtQHiJeQJxz9G3Tx-0"
        + "83B745_AfYOnlC9w\""
        + "}").replace(" ", "");
    private static final String SECOND_SIGNATURE_ENTRY_MULTIPLE_SIGNATURES = ("{"
        + "\"header\": {"
        + "\"alg\": \"ES512\","
        + "\"kid\": \"bilbo.baggins@hobbiton.example\""
        + "},"
        + "\"signature\": \"ARcVLnaJJaUWG8fG-8t5BREVAuTY8n8YHjwDO1muhcdCoF"
        + "ZFFjfISu0Cdkn9Ybdlmi54ho0x924DUz8sK7ZXkhc7AFM8ObLfTvNCrq"
        + "cI3Jkl2U5IX3utNhODH6v7xgy1Qahsn0fyb4zSAkje8bAWz4vIfj5pCM"
        + "Yxxm4fgV3q7ZYhm5eD\""
        + "}").replace(" ", "");
    private static final String SECOND_SIGNATURE_UNPROTECTED_HEADER_MULTIPLE_SIGNATURES = ("{"
        + "\"alg\": \"ES512\","
        + "\"kid\": \"bilbo.baggins@hobbiton.example\""
        + "}").replace(" ", "");
    private static final String THIRD_SIGNATURE_ENTRY_MULTIPLE_SIGNATURES = ("{"
        + "\"protected\": \"eyJhbGciOiJIUzI1NiIsImtpZCI6IjAxOGMwYWU1LTRkOW"
        + "ItNDcxYi1iZmQ2LWVlZjMxNGJjNzAzNyJ9\","
        + "\"signature\": \"s0h6KThzkfBBBkLspW1h84VsJZFTsPPqMDA7g1Md7p0\""
        + "}").replace(" ", "");
    private static final String MULTIPLE_SIGNATURES_JSON_GENERAL_SERIALIZATION = ("{"
        + "\"payload\": \"SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywg"
        + "Z29pbmcgb3V0IHlvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9h"
        + "ZCwgYW5kIGlmIHlvdSBkb24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXi"
        + "gJlzIG5vIGtub3dpbmcgd2hlcmUgeW91IG1pZ2h0IGJlIHN3ZXB0IG9m"
        + "ZiB0by4\","
        + "\"signatures\": ["
        + "{"
        + "\"protected\": \"eyJhbGciOiJSUzI1NiJ9\","
        + "\"header\": {"
        + "\"kid\": \"bilbo.baggins@hobbiton.example\""
        + "},"
        + "\"signature\": \"MIsjqtVlOpa71KE-Mss8_Nq2YH4FGhiocsqrgi5Nvy"
        + "G53uoimic1tcMdSg-qptrzZc7CG6Svw2Y13TDIqHzTUrL_lR2ZFc"
        + "ryNFiHkSw129EghGpwkpxaTn_THJTCglNbADko1MZBCdwzJxwqZc"
        + "-1RlpO2HibUYyXSwO97BSe0_evZKdjvvKSgsIqjytKSeAMbhMBdM"
        + "ma622_BG5t4sdbuCHtFjp9iJmkio47AIwqkZV1aIZsv33uPUqBBC"
        + "XbYoQJwt7mxPftHmNlGoOSMxR_3thmXTCm4US-xiNOyhbm8afKK6"
        + "4jU6_TPtQHiJeQJxz9G3Tx-083B745_AfYOnlC9w\""
        + "},"
        + "{"
        + "\"header\": {"
        + "\"alg\": \"ES512\","
        + "\"kid\": \"bilbo.baggins@hobbiton.example\""
        + "},"
        + "\"signature\": \"ARcVLnaJJaUWG8fG-8t5BREVAuTY8n8YHjwDO1muhc"
        + "dCoFZFFjfISu0Cdkn9Ybdlmi54ho0x924DUz8sK7ZXkhc7AFM8Ob"
        + "LfTvNCrqcI3Jkl2U5IX3utNhODH6v7xgy1Qahsn0fyb4zSAkje8b"
        + "AWz4vIfj5pCMYxxm4fgV3q7ZYhm5eD\""
        + "},"
        + "{"
        + "\"protected\": \"eyJhbGciOiJIUzI1NiIsImtpZCI6IjAxOGMwYWU1LT"
        + "RkOWItNDcxYi1iZmQ2LWVlZjMxNGJjNzAzNyJ9\","
        + "\"signature\": \"s0h6KThzkfBBBkLspW1h84VsJZFTsPPqMDA7g1Md7p"
        + "0\""
        + "}"
        + "]"
        + "}").replace(" ", "");
    @Test
    public void testEncodedPayload() throws Exception {
        assertEquals(Base64UrlUtility.encode(PAYLOAD), ENCODED_PAYLOAD);
    }
    @Test
    public void testRSAv15Signature() throws Exception {
        JwsCompactProducer compactProducer = new JwsCompactProducer(PAYLOAD);
        compactProducer.getJwsHeaders().setSignatureAlgorithm(SignatureAlgorithm.RS256);
        compactProducer.getJwsHeaders().setKeyId(RSA_KID_VALUE);
        JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();
        assertEquals(reader.toJson(compactProducer.getJwsHeaders().asMap()), RSA_V1_5_SIGNATURE_PROTECTED_HEADER_JSON);
        assertEquals(compactProducer.getUnsignedEncodedJws(),
                RSA_V1_5_SIGNATURE_PROTECTED_HEADER + "." + ENCODED_PAYLOAD);
        JsonWebKeys jwks = readKeySet("cookbookPrivateSet.txt");
        List<JsonWebKey> keys = jwks.getKeys();
        JsonWebKey rsaKey = keys.get(1);
        compactProducer.signWith(rsaKey);
        assertEquals(compactProducer.getSignedEncodedJws(),
                RSA_V1_5_SIGNATURE_PROTECTED_HEADER + "." + ENCODED_PAYLOAD + "." + RSA_V1_5_SIGNATURE_VALUE);
        JwsCompactConsumer compactConsumer = new JwsCompactConsumer(compactProducer.getSignedEncodedJws());
        JsonWebKeys publicJwks = readKeySet("cookbookPublicSet.txt");
        List<JsonWebKey> publicKeys = publicJwks.getKeys();
        JsonWebKey rsaPublicKey = publicKeys.get(1);
        assertTrue(compactConsumer.verifySignatureWith(rsaPublicKey,
                                                       SignatureAlgorithm.RS256));

        JwsJsonProducer jsonProducer = new JwsJsonProducer(PAYLOAD);
        assertEquals(jsonProducer.getPlainPayload(), PAYLOAD);
        assertEquals(jsonProducer.getUnsignedEncodedPayload(), ENCODED_PAYLOAD);
        JwsHeaders protectedHeader = new JwsHeaders();
        protectedHeader.setSignatureAlgorithm(SignatureAlgorithm.RS256);
        protectedHeader.setKeyId(RSA_KID_VALUE);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(rsaKey,
                                                            SignatureAlgorithm.RS256), protectedHeader);
        assertEquals(jsonProducer.getJwsJsonSignedDocument(), RSA_V1_5_JSON_GENERAL_SERIALIZATION);
        JwsJsonConsumer jsonConsumer = new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument());
        assertTrue(jsonConsumer.verifySignatureWith(rsaPublicKey, SignatureAlgorithm.RS256));

        jsonProducer = new JwsJsonProducer(PAYLOAD, true);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(rsaKey, SignatureAlgorithm.RS256), protectedHeader);
        assertEquals(jsonProducer.getJwsJsonSignedDocument(), RSA_V1_5_JSON_FLATTENED_SERIALIZATION);
        jsonConsumer = new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument());
        assertTrue(jsonConsumer.verifySignatureWith(rsaPublicKey, SignatureAlgorithm.RS256));
    }
    @Test
    public void testRSAPSSSignature() throws Exception {
        

        JwsCompactProducer compactProducer = new JwsCompactProducer(PAYLOAD);
        compactProducer.getJwsHeaders().setSignatureAlgorithm(SignatureAlgorithm.PS384);
        compactProducer.getJwsHeaders().setKeyId(RSA_KID_VALUE);
        JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();
        assertEquals(reader.toJson(compactProducer.getJwsHeaders().asMap()), RSA_PSS_SIGNATURE_PROTECTED_HEADER_JSON);
        assertEquals(compactProducer.getUnsignedEncodedJws(),
                RSA_PSS_SIGNATURE_PROTECTED_HEADER + "." + ENCODED_PAYLOAD);
        JsonWebKeys jwks = readKeySet("cookbookPrivateSet.txt");
        List<JsonWebKey> keys = jwks.getKeys();
        JsonWebKey rsaKey = keys.get(1);
        compactProducer.signWith(rsaKey);
        assertEquals(compactProducer.getSignedEncodedJws().length(),
                (RSA_PSS_SIGNATURE_PROTECTED_HEADER + "." + ENCODED_PAYLOAD + "." + RSA_PSS_SIGNATURE_VALUE).length());
        JwsCompactConsumer compactConsumer = new JwsCompactConsumer(compactProducer.getSignedEncodedJws());
        JsonWebKeys publicJwks = readKeySet("cookbookPublicSet.txt");
        List<JsonWebKey> publicKeys = publicJwks.getKeys();
        JsonWebKey rsaPublicKey = publicKeys.get(1);
        assertTrue(compactConsumer.verifySignatureWith(rsaPublicKey, SignatureAlgorithm.PS384));

        JwsJsonProducer jsonProducer = new JwsJsonProducer(PAYLOAD);
        assertEquals(jsonProducer.getPlainPayload(), PAYLOAD);
        assertEquals(jsonProducer.getUnsignedEncodedPayload(), ENCODED_PAYLOAD);
        JwsHeaders protectedHeader = new JwsHeaders();
        protectedHeader.setSignatureAlgorithm(SignatureAlgorithm.PS384);
        protectedHeader.setKeyId(RSA_KID_VALUE);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(rsaKey, SignatureAlgorithm.PS384), protectedHeader);
        assertEquals(jsonProducer.getJwsJsonSignedDocument().length(), RSA_PSS_JSON_GENERAL_SERIALIZATION.length());
        JwsJsonConsumer jsonConsumer = new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument());
        assertTrue(jsonConsumer.verifySignatureWith(rsaPublicKey, SignatureAlgorithm.PS384));

        jsonProducer = new JwsJsonProducer(PAYLOAD, true);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(rsaKey, SignatureAlgorithm.PS384), protectedHeader);
        assertEquals(jsonProducer.getJwsJsonSignedDocument().length(), RSA_PSS_JSON_FLATTENED_SERIALIZATION.length());
        jsonConsumer = new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument());
        assertTrue(jsonConsumer.verifySignatureWith(rsaPublicKey, SignatureAlgorithm.PS384));

    }
    @Test
    public void testECDSASignature() throws Exception {

        
        JwsCompactProducer compactProducer = new JwsCompactProducer(PAYLOAD);
        compactProducer.getJwsHeaders().setSignatureAlgorithm(SignatureAlgorithm.ES512);
        compactProducer.getJwsHeaders().setKeyId(ECDSA_KID_VALUE);
        JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();
        assertEquals(reader.toJson(compactProducer.getJwsHeaders().asMap()),
                     ECDSA_SIGNATURE_PROTECTED_HEADER_JSON);
        assertEquals(compactProducer.getUnsignedEncodedJws(),
                ECSDA_SIGNATURE_PROTECTED_HEADER + "." + ENCODED_PAYLOAD);
        JsonWebKeys jwks = readKeySet("cookbookPrivateSet.txt");
        List<JsonWebKey> keys = jwks.getKeys();
        JsonWebKey ecKey = keys.get(0);
        compactProducer.signWith(new EcDsaJwsSignatureProvider(JwkUtils.toECPrivateKey(ecKey),
                                                               SignatureAlgorithm.ES512));
        assertEquals(compactProducer.getUnsignedEncodedJws(),
                     ECSDA_SIGNATURE_PROTECTED_HEADER + "." + ENCODED_PAYLOAD);
        assertEquals(132, Base64UrlUtility.decode(compactProducer.getEncodedSignature()).length);

        JwsCompactConsumer compactConsumer = new JwsCompactConsumer(compactProducer.getSignedEncodedJws());
        JsonWebKeys publicJwks = readKeySet("cookbookPublicSet.txt");
        List<JsonWebKey> publicKeys = publicJwks.getKeys();
        JsonWebKey ecPublicKey = publicKeys.get(0);
        assertTrue(compactConsumer.verifySignatureWith(ecPublicKey, SignatureAlgorithm.ES512));
    }
    @Test
    public void testHMACSignature() throws Exception {
        JwsCompactProducer compactProducer = new JwsCompactProducer(PAYLOAD);
        compactProducer.getJwsHeaders().setSignatureAlgorithm(SignatureAlgorithm.HS256);
        compactProducer.getJwsHeaders().setKeyId(HMAC_KID_VALUE);
        JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();
        assertEquals(reader.toJson(compactProducer.getJwsHeaders().asMap()), HMAC_SIGNATURE_PROTECTED_HEADER_JSON);
        assertEquals(compactProducer.getUnsignedEncodedJws(),
                HMAC_SIGNATURE_PROTECTED_HEADER + "." + ENCODED_PAYLOAD);
        JsonWebKeys jwks = readKeySet("cookbookSecretSet.txt");
        List<JsonWebKey> keys = jwks.getKeys();
        JsonWebKey key = keys.get(0);
        compactProducer.signWith(key);
        assertEquals(compactProducer.getSignedEncodedJws(),
                HMAC_SIGNATURE_PROTECTED_HEADER + "." + ENCODED_PAYLOAD + "." + HMAC_SIGNATURE_VALUE);
        JwsCompactConsumer compactConsumer = new JwsCompactConsumer(compactProducer.getSignedEncodedJws());
        assertTrue(compactConsumer.verifySignatureWith(key, SignatureAlgorithm.HS256));

        JwsJsonProducer jsonProducer = new JwsJsonProducer(PAYLOAD);
        assertEquals(jsonProducer.getPlainPayload(), PAYLOAD);
        assertEquals(jsonProducer.getUnsignedEncodedPayload(), ENCODED_PAYLOAD);
        JwsHeaders protectedHeader = new JwsHeaders();
        protectedHeader.setSignatureAlgorithm(SignatureAlgorithm.HS256);
        protectedHeader.setKeyId(HMAC_KID_VALUE);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(key, SignatureAlgorithm.HS256), protectedHeader);
        assertEquals(jsonProducer.getJwsJsonSignedDocument(), HMAC_JSON_GENERAL_SERIALIZATION);
        JwsJsonConsumer jsonConsumer = new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument());
        assertTrue(jsonConsumer.verifySignatureWith(key, SignatureAlgorithm.HS256));

        jsonProducer = new JwsJsonProducer(PAYLOAD, true);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(key, SignatureAlgorithm.HS256), protectedHeader);
        assertEquals(jsonProducer.getJwsJsonSignedDocument(), HMAC_JSON_FLATTENED_SERIALIZATION);
        jsonConsumer = new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument());
        assertTrue(jsonConsumer.verifySignatureWith(key, SignatureAlgorithm.HS256));
    }
    @SuppressWarnings("deprecation")
    @Test
    public void testDetachedHMACSignature() throws Exception {
        JwsCompactProducer compactProducer = new JwsCompactProducer(PAYLOAD, true);
        compactProducer.getJwsHeaders().setSignatureAlgorithm(SignatureAlgorithm.HS256);
        compactProducer.getJwsHeaders().setKeyId(HMAC_KID_VALUE);
        JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();
        assertEquals(reader.toJson(compactProducer.getJwsHeaders().asMap()), HMAC_SIGNATURE_PROTECTED_HEADER_JSON);
        assertEquals(compactProducer.getUnsignedEncodedJws(),
                HMAC_SIGNATURE_PROTECTED_HEADER + ".");
        JsonWebKeys jwks = readKeySet("cookbookSecretSet.txt");
        List<JsonWebKey> keys = jwks.getKeys();
        JsonWebKey key = keys.get(0);
        compactProducer.signWith(key);
        assertEquals(compactProducer.getSignedEncodedJws(), DETACHED_HMAC_JWS);
        JwsCompactConsumer compactConsumer =
                new JwsCompactConsumer(compactProducer.getSignedEncodedJws(), ENCODED_PAYLOAD);
        assertTrue(compactConsumer.verifySignatureWith(key, SignatureAlgorithm.HS256));

        JwsJsonProducer jsonProducer = new JwsJsonProducer(PAYLOAD);
        assertEquals(jsonProducer.getPlainPayload(), PAYLOAD);
        assertEquals(jsonProducer.getUnsignedEncodedPayload(), ENCODED_PAYLOAD);
        JwsHeaders protectedHeader = new JwsHeaders();
        protectedHeader.setSignatureAlgorithm(SignatureAlgorithm.HS256);
        protectedHeader.setKeyId(HMAC_KID_VALUE);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(key, SignatureAlgorithm.HS256), protectedHeader);
        assertEquals(jsonProducer.getJwsJsonSignedDocument(true), HMAC_DETACHED_JSON_GENERAL_SERIALIZATION);
        JwsJsonConsumer jsonConsumer =
                new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument(true), ENCODED_PAYLOAD);
        assertTrue(jsonConsumer.verifySignatureWith(key, SignatureAlgorithm.HS256));

        jsonProducer = new JwsJsonProducer(PAYLOAD, true);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(key, SignatureAlgorithm.HS256), protectedHeader);
        assertEquals(jsonProducer.getJwsJsonSignedDocument(true), HMAC_DETACHED_JSON_FLATTENED_SERIALIZATION);
        jsonConsumer = new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument(true), ENCODED_PAYLOAD);
        assertTrue(jsonConsumer.verifySignatureWith(key, SignatureAlgorithm.HS256));
    }
    @Test
    public void testDetachedHMACSignature2() throws Exception {
        JsonWebKeys jwks = readKeySet("cookbookSecretSet.txt");
        List<JsonWebKey> keys = jwks.getKeys();
        JsonWebKey key = keys.get(0);
        
        JwsJsonProducer jsonProducer = new JwsJsonProducer(PAYLOAD, false, true);
        assertEquals(jsonProducer.getPlainPayload(), PAYLOAD);
        assertEquals(jsonProducer.getUnsignedEncodedPayload(), ENCODED_PAYLOAD);
        JwsHeaders protectedHeader = new JwsHeaders();
        protectedHeader.setSignatureAlgorithm(SignatureAlgorithm.HS256);
        protectedHeader.setKeyId(HMAC_KID_VALUE);
        
        String jwsJsonCompleteSequence = 
            jsonProducer.signWith(JwsUtils.getSignatureProvider(key, SignatureAlgorithm.HS256), protectedHeader);
        assertEquals(jwsJsonCompleteSequence, HMAC_DETACHED_JSON_GENERAL_SERIALIZATION);
        JwsJsonConsumer jsonConsumer =
                new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument(), ENCODED_PAYLOAD);
        assertTrue(jsonConsumer.verifySignatureWith(key, SignatureAlgorithm.HS256));

        jsonProducer = new JwsJsonProducer(PAYLOAD, true, true);
        String jwsJsonFlattenedSequence = 
            jsonProducer.signWith(JwsUtils.getSignatureProvider(key, SignatureAlgorithm.HS256), protectedHeader);
        assertEquals(jwsJsonFlattenedSequence, HMAC_DETACHED_JSON_FLATTENED_SERIALIZATION);
        jsonConsumer = new JwsJsonConsumer(jwsJsonFlattenedSequence, ENCODED_PAYLOAD);
        assertTrue(jsonConsumer.verifySignatureWith(key, SignatureAlgorithm.HS256));
    }
    @Test
    public void testProtectingSpecificHeaderFieldsSignature() throws Exception {
        JwsJsonProducer jsonProducer = new JwsJsonProducer(PAYLOAD);
        assertEquals(jsonProducer.getPlainPayload(), PAYLOAD);
        assertEquals(jsonProducer.getUnsignedEncodedPayload(), ENCODED_PAYLOAD);
        JwsHeaders protectedHeader = new JwsHeaders();
        protectedHeader.setSignatureAlgorithm(SignatureAlgorithm.HS256);
        JwsHeaders unprotectedHeader = new JwsHeaders();
        unprotectedHeader.setKeyId(HMAC_KID_VALUE);
        JsonWebKeys jwks = readKeySet("cookbookSecretSet.txt");
        List<JsonWebKey> keys = jwks.getKeys();
        JsonWebKey key = keys.get(0);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(key, SignatureAlgorithm.HS256),
                protectedHeader, unprotectedHeader);
        assertEquals(jsonProducer.getJwsJsonSignedDocument(),
                PROTECTING_SPECIFIC_HEADER_FIELDS_JSON_GENERAL_SERIALIZATION);
        JwsJsonConsumer jsonConsumer =
                new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument());
        assertTrue(jsonConsumer.verifySignatureWith(key, SignatureAlgorithm.HS256));

        jsonProducer = new JwsJsonProducer(PAYLOAD, true);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(key, SignatureAlgorithm.HS256),
                protectedHeader, unprotectedHeader);
        assertEquals(jsonProducer.getJwsJsonSignedDocument(),
                PROTECTING_SPECIFIC_HEADER_FIELDS_JSON_FLATTENED_SERIALIZATION);
        jsonConsumer = new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument());
        assertTrue(jsonConsumer.verifySignatureWith(key, SignatureAlgorithm.HS256));
    }
    @Test
    public void testProtectingContentOnlySignature() throws Exception {
        JwsJsonProducer jsonProducer = new JwsJsonProducer(PAYLOAD);
        assertEquals(jsonProducer.getPlainPayload(), PAYLOAD);
        assertEquals(jsonProducer.getUnsignedEncodedPayload(), ENCODED_PAYLOAD);
        JwsHeaders unprotectedHeader = new JwsHeaders();
        unprotectedHeader.setSignatureAlgorithm(SignatureAlgorithm.HS256);
        unprotectedHeader.setKeyId(HMAC_KID_VALUE);
        JsonWebKeys jwks = readKeySet("cookbookSecretSet.txt");
        List<JsonWebKey> keys = jwks.getKeys();
        JsonWebKey key = keys.get(0);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(key, SignatureAlgorithm.HS256),
                null, unprotectedHeader);
        assertEquals(jsonProducer.getJwsJsonSignedDocument(),
                PROTECTING_CONTENT_ONLY_JSON_GENERAL_SERIALIZATION);
        JwsJsonConsumer jsonConsumer =
                new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument());
        assertTrue(jsonConsumer.verifySignatureWith(key, SignatureAlgorithm.HS256));

        jsonProducer = new JwsJsonProducer(PAYLOAD, true);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(key, SignatureAlgorithm.HS256),
                null, unprotectedHeader);
        assertEquals(jsonProducer.getJwsJsonSignedDocument(),
                PROTECTING_CONTENT_ONLY_JSON_FLATTENED_SERIALIZATION);
        jsonConsumer = new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument());
        assertTrue(jsonConsumer.verifySignatureWith(key, SignatureAlgorithm.HS256));
    }
    @Test
    public void testMultipleSignatures() throws Exception {
       
        JwsJsonProducer jsonProducer = new JwsJsonProducer(PAYLOAD);
        assertEquals(jsonProducer.getPlainPayload(), PAYLOAD);
        assertEquals(jsonProducer.getUnsignedEncodedPayload(), ENCODED_PAYLOAD);
        JwsHeaders firstSignerProtectedHeader = new JwsHeaders();
        firstSignerProtectedHeader.setSignatureAlgorithm(SignatureAlgorithm.RS256);
        JwsHeaders firstSignerUnprotectedHeader = new JwsHeaders();
        firstSignerUnprotectedHeader.setKeyId(RSA_KID_VALUE);
        JsonWebKeys jwks = readKeySet("cookbookPrivateSet.txt");
        List<JsonWebKey> keys = jwks.getKeys();
        JsonWebKey rsaKey = keys.get(1);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(rsaKey, SignatureAlgorithm.RS256),
                firstSignerProtectedHeader, firstSignerUnprotectedHeader);
        assertEquals(jsonProducer.getSignatureEntries().get(0).toJson(),
                FIRST_SIGNATURE_ENTRY_MULTIPLE_SIGNATURES);

        JwsHeaders secondSignerUnprotectedHeader = new JwsHeaders();
        secondSignerUnprotectedHeader.setSignatureAlgorithm(SignatureAlgorithm.ES512);
        secondSignerUnprotectedHeader.setKeyId(ECDSA_KID_VALUE);
        JsonWebKey ecKey = keys.get(0);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(ecKey, SignatureAlgorithm.ES512),
                null, secondSignerUnprotectedHeader);
        assertEquals(new JsonMapObjectReaderWriter().toJson(
            jsonProducer.getSignatureEntries().get(1).getUnprotectedHeader()),
                SECOND_SIGNATURE_UNPROTECTED_HEADER_MULTIPLE_SIGNATURES);
        assertEquals(jsonProducer.getSignatureEntries().get(1).toJson().length(),
                SECOND_SIGNATURE_ENTRY_MULTIPLE_SIGNATURES.length());

        JwsHeaders thirdSignerProtectedHeader = new JwsHeaders();
        thirdSignerProtectedHeader.setSignatureAlgorithm(SignatureAlgorithm.HS256);
        thirdSignerProtectedHeader.setKeyId(HMAC_KID_VALUE);
        JsonWebKeys secretJwks = readKeySet("cookbookSecretSet.txt");
        List<JsonWebKey> secretKeys = secretJwks.getKeys();
        JsonWebKey hmacKey = secretKeys.get(0);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(hmacKey, SignatureAlgorithm.HS256),
                thirdSignerProtectedHeader);
        assertEquals(jsonProducer.getSignatureEntries().get(2).toJson(),
                THIRD_SIGNATURE_ENTRY_MULTIPLE_SIGNATURES);
        assertEquals(jsonProducer.getJwsJsonSignedDocument().length(),
                MULTIPLE_SIGNATURES_JSON_GENERAL_SERIALIZATION.length());
        JwsJsonConsumer jsonConsumer =
                new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument());
        JsonWebKeys publicJwks = readKeySet("cookbookPublicSet.txt");
        List<JsonWebKey> publicKeys = publicJwks.getKeys();
        JsonWebKey rsaPublicKey = publicKeys.get(1);
        JsonWebKey ecPublicKey = publicKeys.get(0);
        assertTrue(jsonConsumer.verifySignatureWith(rsaPublicKey, SignatureAlgorithm.RS256));
        assertTrue(jsonConsumer.verifySignatureWith(ecPublicKey, SignatureAlgorithm.ES512));
        assertTrue(jsonConsumer.verifySignatureWith(hmacKey, SignatureAlgorithm.HS256));
    }
    public JsonWebKeys readKeySet(String fileName) throws Exception {
        InputStream is = JwsJoseCookBookTest.class.getResourceAsStream(fileName);
        String s = IOUtils.readStringFromStream(is);
        return JwkUtils.readJwkSet(s);
    }
}
