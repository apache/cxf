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

# Agent Guide for Apache CXF

This file is read by automated agents (security scanners, code analyzers,
AI assistants) operating on this repository. It points them at the
human-authored references they should consult before producing output.

## Security

Security model: [SECURITY.md](./SECURITY.md) -> [THREAT_MODEL.md](./THREAT_MODEL.md)

Agents that scan this repository should consult `SECURITY.md` and the linked
`THREAT_MODEL.md` for the project's threat model before reporting issues. Note
that CXF is a *framework*: it provides security mechanisms (WS-Security, TLS
conduits, authorization interceptors, XML-processing limits) and many
properties are conditional on how the integrator configures them. The model
covers the runtime SOAP/JAX-WS and REST/JAX-RS pipeline, WS-Security
processing, XML parsing (XXE/DoS), the SSRF surface of remote
WSDL/schema/MTOM resolution, and JAX-RS provider deserialization.
