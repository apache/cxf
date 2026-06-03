# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 4.1.x   | :white_check_mark: |
| 4.0.x   | :white_check_mark: |
| 3.6.x   | :white_check_mark: |
| <= 3.5.x | :x:                |

## Reporting a Vulnerability

For information on how to report a new security problem please see [here](https://www.apache.org/security/).
Our existing security advisories are published [here](https://cxf.apache.org/security-advisories.html).

## Threat Model

What CXF treats as in scope and out of scope, the security properties it
provides and the ones it disclaims, the adversary model, and how inbound
reports and tool/AI findings are triaged are documented in
[THREAT_MODEL.md](./THREAT_MODEL.md). Because CXF is a framework, many of those
properties are conditional on how the integrator configures it; the
integrator-responsibilities and known-non-findings sections of that document
are the most useful starting points for triaging a report.
