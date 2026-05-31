<!--
SPDX-License-Identifier: Apache-2.0

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Threat Model — Apache CXF (umbrella)

## §1 Header

- **Project:** Apache CXF — an open-source **services framework** for building and consuming SOAP
  (JAX-WS) and REST (JAX-RS) services, with an interceptor-based message pipeline and broad WS-* support
  (WS-Security, WS-Trust, WS-SecurityPolicy, WS-Addressing, WS-ReliableMessaging) *(documented — project
  site, source `org.apache.cxf.interceptor.security`, `common.security`)*.
- **Scope of this umbrella:** the CXF **runtime framework** (`apache/cxf`). It also names the sibling
  in-scope repos and where they sit: `apache/cxf-fediz` (WS-Federation SSO — its own distinct runtime
  surface, addressed in its own model/addendum), and `apache/cxf-xjc-utils` + `apache/cxf-build-utils`
  (**build-time codegen / build tooling** — out of the *runtime* threat model; see §3).
- **Modelled against:** `apache/cxf` `main`/HEAD (2026-05-31), covering supported lines 3.6.x / 4.0.x /
  4.1.x *(documented — SECURITY.md)*.
- **Status:** **DRAFT — v0, not yet reviewed by the CXF PMC.** Produced by the ASF Security team via the
  `threat-model-producer` rubric (<https://gist.github.com/potiuk/da14a826283038ddfe38cc9fe6310573>).
- **Version binding:** versioned with the project; reports triaged against the model as it stood at the
  affected release. Existing advisories: <https://cxf.apache.org/security-advisories.html> *(documented)*.
- **Reporting cross-reference:** §8-property violations → report privately per `SECURITY.md` /
  <https://www.apache.org/security/>; §3 / §9 findings closed citing this document.
- **Provenance legend:** *(documented)* / *(maintainer)* / *(inferred)*; each *(inferred)* routes to §14.
- **Draft confidence:** ~14 documented / 0 maintainer / ~55 inferred.

**Framing note that governs the whole model:** CXF is a *framework*, not a deployed application. It provides
**mechanisms** (WS-Security, TLS conduits, auth/authorization interceptors, input limits); **which mechanisms
are active, and how, is chosen by the integrator** who builds a service on CXF. Many properties below are
therefore *conditional on configuration*, and §10 (integrator responsibilities) is unusually load-bearing.

## §2 Scope and intended use

Primary intended use *(documented)*: an in-application library/framework that a developer uses to expose or
consume web services — SOAP endpoints over JAX-WS and/or REST endpoints over JAX-RS — typically hosted in a
servlet container or via the CXF HTTP transport, with an interceptor chain processing each message.

Caller roles:

- **Service client (untrusted)** — any peer that can send a SOAP/REST request to a CXF endpoint.
- **Authenticated client** — a client whose WS-Security token / transport credential the integrator's
  configured interceptors have validated.
- **Service integrator (developer/operator)** — selects transports, interceptors, WS-SecurityPolicy, JAX-RS
  providers, and limits. **Trusted; configures the security posture; out of model as adversary (§3).**

**Component-family table:**

| Family | Entry point | Touches outside process | In model? |
| --- | --- | --- | --- |
| SOAP / JAX-WS message pipeline | endpoint + interceptor chain, StAX/XML parse | network; XML | **Yes** |
| WS-Security processing (via WSS4J) | sig/enc/UsernameToken/SAML/Timestamp interceptors | crypto; XML | **Yes** |
| REST / JAX-RS pipeline | resource matching, providers (JSON/XML/multipart) | network; deserialization | **Yes** |
| HTTP transport / conduit (client side) | HTTP(S) conduit, WSDL/schema/MTOM URL fetch | **network egress** | **Yes** |
| AuthN/AuthZ interceptors + security context | `interceptor.security.*`, `common.security.*` | — | **Yes** |
| WS-Federation SSO | `apache/cxf-fediz` | network; SAML | Sibling repo → own model |
| XJC codegen / build plugins | `apache/cxf-xjc-utils` | build-time only | No → §3 |
| Build utilities | `apache/cxf-build-utils` | build-time only | No → §3 |

## §3 Out of scope (explicit non-goals)

- **The service integrator / operator as adversary**, and any insecurity that is purely a *misconfiguration*
  of CXF by the integrator (e.g. disabling signature verification, trusting all TLS certs, exposing an
  unauthenticated endpoint). CXF provides the controls; choosing not to use them is the integrator's call,
  surfaced in §10/§11 rather than defended here *(inferred — framework norm)*.
- **Build-time tooling:** `apache/cxf-xjc-utils` (XJC schema→Java plugins) and `apache/cxf-build-utils` run at
  build time, not in the request path; their threat surface is the codegen/build pipeline, not a runtime
  adversary. They are in scan scope for hygiene but **out of this runtime threat model** *(inferred)*.
- **`apache/cxf-fediz`** — WS-Federation SSO has a distinct trust surface (IdP, RP plugins, SAML token
  issuance/consumption) and is modelled separately.
- **The underlying XML/crypto stacks** (the JAXP/StAX provider, the JCE provider, WSS4J internals) except as
  CXF configures and invokes them — CXF's *defaults and wiring* are in model; provider bugs are upstream.
- **Application business logic** running behind a CXF endpoint.

## §4 Trust boundaries and data flow

The trust boundary is the **endpoint's inbound interceptor chain**: bytes off the wire are untrusted until
the configured interceptors (decoding, optional WS-Security, optional auth) have processed them *(inferred)*.

Trust transitions:

1. **Wire → StAX/XML parse:** raw SOAP/XML is parsed. This is the classic XML-attack surface (XXE, entity
   expansion / billion-laughs, deeply-nested DoS). CXF ships input-limit and secure-parsing defaults that
   the model must pin *(inferred — wave-1)*.
2. **Parsed message → WS-Security:** if WS-SecurityPolicy / interceptors are configured, signatures and
   encryption are verified, tokens (UsernameToken, SAML, X.509, Kerberos) validated, timestamps/nonces
   checked for replay, and algorithms/BSP compliance enforced *(documented — interceptor classes; specifics
   inferred)*. Without this configuration, the message is only transport-trusted.
3. **Message → authZ:** authorizing interceptors map the established identity to roles/permitted operations
   *(documented — `AbstractAuthorizingInInterceptor`)*.
4. **REST body → provider:** JAX-RS providers unmarshal bodies (JSON/XML/multipart) into objects — a
   deserialization and parser-DoS surface *(inferred)*.
5. **Client conduit → remote fetch:** as a *client*, or when resolving WSDL/schema imports or MTOM
   references, CXF makes **outbound** requests to URLs — an SSRF surface if any URL is attacker-influenced
   *(inferred)*.

**Reachability preconditions:** a finding is in-model on the **server inbound** path if reachable from an
untrusted client message *before* the integrator-configured auth boundary; a finding that requires the
integrator to have *disabled* a security control is `OUT-OF-MODEL: non-default-build` / misconfig (§5a/§3);
a finding in WSDL/MTOM fetch is in-model if a URL is attacker-influenceable.

## §5 Assumptions about the environment

- **Runtime:** a JVM and (typically) a servlet container or the CXF HTTP transport hosting endpoints
  *(documented)*.
- **Crypto/keys:** keystores/truststores and JCE providers are supplied and managed by the integrator
  *(inferred)*.
- **Transport:** TLS is provided by the container/conduit configuration; CXF supports but does not force it
  *(inferred — wave-1 default)*.
- **What CXF does to its host (*(inferred)* — wave-2):** parses XML/JSON; performs crypto; opens **outbound**
  HTTP(S) for client calls, WSDL/schema imports, and MTOM; reads configured keystores. Not assumed to spawn
  processes or read arbitrary files outside configured resources.

## §5a Build-time and configuration variants

CXF's security envelope is set by **runtime configuration**, not compile flags. The high-leverage knobs
(defaults *(inferred)* — wave-1 confirmation targets, because each reshapes §8/§9/§11a):

| Knob | Effect | Default ruling needed |
| --- | --- | --- |
| StAX secure-processing / `DocumentDepth` / entity-expansion / element-count limits | XXE + XML-DoS posture | **Open (wave-1):** are secure defaults on out-of-the-box? |
| WS-SecurityPolicy / WS-Security interceptors configured | Whether messages are signed/encrypted/authenticated at all | Integrator choice; unconfigured = transport-trust only |
| `allowStreaming` / streaming vs DOM WS-Security | Different XSW/validation surface | Open |
| TLS enforcement on the conduit/listener | Transport confidentiality/integrity | Integrator choice (§10) |
| JAX-RS provider set (JSON/XML libs), `@XmlRootElement`/JAXB, Aegis | Deserialization + XXE surface | Open — which providers/defaults |
| Property/URL resolution for WSDL/schema/MTOM | SSRF surface | Open — is remote import allowed by default? |

## §6 Assumptions about inputs

| Entry point | Parameter | Attacker-controllable? | Caller/integrator must enforce |
| --- | --- | --- | --- |
| SOAP endpoint | SOAP/XML envelope (headers, body, attachments) | **yes** | XML limits; WS-Security policy; size caps |
| WS-Security headers | signatures, tokens, timestamps | **yes** | sig/enc verification; token validation; replay window |
| JAX-RS resource | path, query, headers, body (JSON/XML/multipart) | **yes** | provider hardening; deserialization allow-lists; size limits |
| WSDL/schema/MTOM reference | import URL | **yes if integrator allows remote refs** | disable/allow-list remote resolution (SSRF) |
| client conduit | response from remote service | from the **remote endpoint** | validate as untrusted on the client side too |
| Spring/Blueprint/`cxf.xml` config | all keys | **no — integrator-trusted** | never sourced from a request |

## §7 Adversary model

- **Primary adversary:** an untrusted network client of a CXF-hosted endpoint. Capabilities: send arbitrary
  SOAP/REST messages, malformed/oversized XML/JSON, crafted WS-Security headers (signature-wrapping, expired
  or forged tokens, replays), and (where remote references are enabled) attacker-chosen import URLs.
- **Secondary:** a malicious *remote service* when CXF is the **client** (hostile WSDL/response).
- **Goals:** bypass authentication/authorization; XXE/file/SSRF exfiltration; XML/JSON parser DoS; signature-
  wrapping to alter a signed message; deserialization RCE via a provider; replay of captured tokens.
- **Out of model:** the integrator/operator; an adversary who requires a misconfiguration the integrator
  chose; provider/library internals CXF merely invokes.

## §8 Security properties the project provides

*(All conditional on the integrator enabling the relevant mechanism; *(inferred)* pending §14.)*

1. **Message-level authentication & integrity via WS-Security** — when WS-SecurityPolicy/interceptors are
   configured, signatures and tokens are verified and replay windows enforced *(documented — interceptor
   stack; specifics inferred)*. *Symptom:* accepted forged/replayed/unsigned message where policy required
   otherwise; signature-wrapping. *Severity:* critical.
2. **Authorization enforcement** — authorizing interceptors deny operations the established identity lacks
   *(documented — `AbstractAuthorizingInInterceptor`)*. *Symptom:* operation invoked without the required
   role. *Severity:* critical.
3. **Safe-by-default XML processing** — secure StAX processing and input limits (depth/entity/element/size)
   mitigate XXE and XML-bomb DoS on inbound messages *(inferred — load-bearing; wave-1)*. *Symptom:* XXE
   read/SSRF, OOM/CPU exhaustion from crafted XML. *Severity:* critical.
4. **Transport security support** — TLS conduits/listeners with hostname + cert validation when configured
   *(inferred)*. *Symptom:* MITM where TLS was expected; accepted invalid cert. *Severity:* high.
5. **Robust message parsing** — malformed SOAP/REST input yields a clean fault, not memory corruption or
   crash *(inferred)*. *Symptom:* unhandled crash/hang from crafted input. *Severity:* high.

## §9 Security properties the project does NOT provide

- **No security without configuration.** An endpoint with no WS-Security policy and no transport security is
  authenticated and confidential only to the extent the integrator wired it; CXF does not impose message
  security by default *(inferred)*.
- **No defence against integrator misconfiguration** (disabling sig verification, `setValidateCert(false)`,
  permissive providers) — §3/§11.
- **No application-level authorization model** beyond the interceptor mechanisms the integrator configures.

**False friends:**

- *A `UsernameToken` in a SOAP header looks like authentication but is only as strong as the configured
  validation + transport* — a plaintext-password UsernameToken over non-TLS is interceptable.
- *XML Signature covering "a" element is not the same as covering "the" element* — signature-wrapping (XSW)
  defeats naive "is it signed?" checks; the binding must verify *what* is signed.
- *WS-ReliableMessaging / WS-Addressing are reliability/routing, not security.*

**Well-known attack classes the integrator must keep in view:** XXE and XML entity-expansion DoS; XML
signature-wrapping; SAML assertion forgery/replay; JSON/XML/Java **deserialization** via JAX-RS providers;
SSRF via WSDL/schema/MTOM remote references; padding-oracle / weak-algorithm acceptance if BSP/algorithm
restrictions are relaxed; ReDoS in any regex-based routing.

## §10 Downstream (integrator) responsibilities

- **Configure WS-SecurityPolicy / interceptors** for the authentication, signing, and encryption your service
  actually requires — and verify *what* is signed (anti-XSW), not merely *that* something is.
- **Keep the secure XML defaults on** (depth/entity/size limits); do not disable secure StAX processing.
- **Enforce TLS** with proper hostname/cert validation on both listeners and client conduits.
- **Harden JAX-RS providers** — restrict deserialization (no unsafe polymorphic JSON; disable DTDs in XML
  providers); set body-size limits.
- **Disable or allow-list remote WSDL/schema/MTOM resolution** to close SSRF.
- Validate **client-side** responses as untrusted when CXF consumes a remote service.
- Track <https://cxf.apache.org/security-advisories.html> and stay on a supported line (3.6.x / 4.0.x / 4.1.x).

## §11 Known misuse patterns

- Exposing a SOAP/REST endpoint with no WS-Security and no TLS and assuming the framework "is secure".
- Accepting a signed message without verifying the signature covers the security-relevant elements (XSW).
- Enabling remote entity/WSDL/schema resolution against untrusted input (XXE/SSRF).
- Using a JAX-RS JSON provider with unsafe polymorphic deserialization on untrusted bodies.
- Disabling certificate/hostname validation on the HTTP conduit for convenience.

## §11a Known non-findings (recurring false positives)

*(v0 seed — the PMC (coheigea/reta are WS-Security experts) will have the authoritative list — §14.)*

- **"Endpoint has no authentication"** against framework code/tests/samples — security is integrator-
  configured (§9); not a framework bug unless a *default* control is missing or broken.
- **XXE/SSRF reachable only when the integrator enabled remote resolution / disabled limits** —
  `OUT-OF-MODEL: non-default-build` (§5a) unless the *default* is unsafe (then `VALID` — wave-1).
- **Deserialization "gadget" in a provider the integrator did not enable** — out of the default surface (§3).
- **Findings in `apache/cxf-xjc-utils` / `apache/cxf-build-utils`** — build-time tooling (§3).
- **`systests`/`testtools`/sample code** — out of scope (§3).
- **Use of a weak algorithm explicitly configured by the integrator** — integrator choice, not a framework
  default break.

## §12 Conditions that would change this model

- A change to the default XML-processing limits / secure-parsing posture.
- A new transport, JAX-RS provider default, or WS-* mechanism enabled by default.
- A change to default algorithm/BSP restrictions in the WS-Security stack.
- Promotion of remote reference resolution to on-by-default.
- Any report not cleanly routable to a §13 disposition.

## §13 Triage dispositions

| Disposition | Meaning | Licensed by |
| --- | --- | --- |
| `VALID` | Violates a claimed property via an in-scope adversary/input *in a default/secure configuration*. | §8, §6, §7 |
| `VALID-HARDENING` | No §8 property broken in default config, but a §11 misuse is easy enough to warrant a safer default or guard. | §11 |
| `OUT-OF-MODEL: trusted-input` | Requires control of integrator config / keystores. | §6 |
| `OUT-OF-MODEL: adversary-not-in-scope` | Requires the integrator/operator capability. | §7, §3 |
| `OUT-OF-MODEL: unsupported-component` | Lands in build tooling (xjc/build-utils), systests, or samples. | §3 |
| `OUT-OF-MODEL: non-default-build` | Only manifests when the integrator disabled a control or enabled an unsafe option. | §5a |
| `BY-DESIGN: property-disclaimed` | Concerns a §9-disclaimed property (no security without configuration; reliability ≠ security). | §9 |
| `KNOWN-NON-FINDING` | Matches a §11a entry. | §11a |
| `MODEL-GAP` | Routes to none of the above → revise the model. | §12 |

## §14 Open questions for the maintainers

**Wave 1 — defaults that decide VALID-vs-misconfig (§5a/§8/§9):**
1. Out of the box, are CXF's **XML secure-processing limits** (entity expansion off, DTD/external-entity
   resolution off, depth/element/size caps) **on by default** for inbound SOAP and JAX-RS XML providers, so
   an XXE/XML-bomb report against defaults is `VALID`? *Proposed:* secure defaults on; XXE-on-defaults = VALID.
2. Is **remote WSDL/schema/MTOM resolution** disabled (or allow-listed) by default? *Proposed:* not fetched
   from untrusted input by default; SSRF requires integrator opt-in.
3. For the shipped **JAX-RS providers**, is unsafe polymorphic/DTD deserialization off by default? *Proposed:*
   yes; unsafe modes are integrator opt-in.

**Wave 2 — WS-Security guarantees (§4/§8):**
4. Which WS-Security guarantees does CXF consider *its* responsibility vs. WSS4J's, and does the binding
   defend **signature-wrapping (XSW)** by verifying signed-element identity by default when a policy is set?
   *Proposed:* CXF+WSS4J enforce XSW protections under policy.
5. Are **replay protections** (timestamp window, nonce cache) on by default when a UsernameToken/Timestamp
   policy is configured? *Proposed:* yes, with a default window.

**Wave 3 — boundaries, advisories, §11a (§3/§9/§11a):**
6. Confirm the **integrator-misconfiguration** boundary: which "insecure" outcomes are explicitly the
   integrator's responsibility (→ `OUT-OF-MODEL`) vs. a framework default we'd fix? *Proposed:* per §3/§11.
7. From the long advisory history, what do scanners most often (re)report that the PMC considers a
   **non-finding** today (already-fixed patterns, by-design items)? (Seeds §11a.)

**Meta:**
8. Confirm the umbrella shape: this model in `apache/cxf` (linked from its existing `SECURITY.md`), a
   **separate** model for `apache/cxf-fediz`, and `cxf-xjc-utils` / `cxf-build-utils` carrying only a
   discoverability pointer to this one as build-time tooling. *Proposed:* yes.

## §15 Machine-readable companion

Deferred for v0; a `threat-model.yaml` can later encode the §6 trust table, §2/§3 scoping, §8
property/severity/symptom rows, §9 false friends, §11a non-findings, and §13 dispositions.
