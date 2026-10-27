# Plan: Modernisierung Ladon S3 Server 2.5.2 → 2.6.0

## Gesamtziel
Das Projekt wird auf einen modernen Stand gebracht, **API- und Storage-kompatibel** (Produktivbetrieb!),
mit reduziertem Abhängigkeitsprofil. Die AWS-Signatur (SigV2/SigV4) wird streng nach AWS-Spezifikation
verifiziert und um fehlende, produktrelevante Funktionen ergänzt. Ein neues Modul
`ladon-s3-server-vfs` liefert eine versionierungsfähige Storage-Implementierung
(Vorbild: „Papers", Dropbox).

## Abnahmekriterien (Definition of Done)
1. `mvn clean verify` grün, **alle** Module (inkl. repariertem `ladon-s3-server-jetty`), Java 21.
2. **SigV4 Header-Auth**: vollständig konform AWS-Spec (canonical request, UriEncode, Multi-Value-Header,
   UNSIGNED-PAYLOAD, x-amz-security-token), mit offizieller AWS-Testvektor-Abdeckung.
3. **SigV4 Presigned URLs (Query-String-Auth)**: werden unterstützt (war bisher 403), inkl.
   Expires-Prüfung (1–604800s) und 15-Min-Clock-Skew-Prüfung (RequestTimeTooSkewed).
4. **SigV4 Chunked Upload**: Erkennung über `x-amz-content-sha256=STREAMING-AWS4-HMAC-SHA256-PAYLOAD`
   (bisher fehlerhaft über Authorization-Präfix!) + **Verifikation der Chunk-Signaturen** (Kette),
   abgedeckt mit AWS-Beispiel-Vektoren (64KB/1KB/0-Chunk).
5. **SigV2** bleibt funktionsfähig (Header- + neue Query-Auth), Regression-Tests.
6. **Neue API-Flächen (additiv)**: Bulk-Delete (`POST ?delete`), ListObjectsV2
   (`KeyCount`, `ContinuationToken`/`NextContinuationToken`, `list-type=2`),
   `response-*`-Header-Overrides bei GET/HEAD-Object.
7. **Dependencies**: Guava entfernt, JAXB-Runtimes konsolidiert (ein Runtime),
   kein `javax.xml.bind`-Rest mehr, JUnit 5, gepeintes surefire, jetty-Modul-Fix,
   OSGi-Kompatibilität erhalten.
8. **Testumfeld**: breite Unit- + Integrationstests (reale AWS-SDK-Clients), alle grün.
9. **Neues Modul `ladon-s3-server-vfs`**: Versionierung (Delete-Marker, Version-Liste,
   content-addressed dedup, referenzaufgelöschtes Garbage-Collecting), unit + Integrationstests.
10. **CI/Doku**: GitHub-Actions-CI (JDK 21, Testlauf), Publish-Workflow auf JDK 21 fix,
    README aktuell (Badges, unterstützte Operationen, Limitationen), `DECISIONS.md`/`PROGRESS.md` aktuell.
11. **Version**: 2.5.2 → 2.6.0 in allen POMs; alle Commits auf Branch `agenticUpdate`; push nach Abschluss.
12. **Bericht**: abschließender Bericht über Änderungen, Tests, Entscheidungen, Limitationen.

## Vorgehen (Phasen)
| Phase | Inhalt | Abhängigkeit |
|---|---|---|
| 0 | Baseline (Build + Tests dokumentiert) | – |
| 1 | Projekt-Hygiene: POMs (Name, Version 2.6.0, surefire), Merge-Konflikt-Reste, jetty-fs-Fix | – |
| 2 | Signatur-Kern: `SigV4`-Utilities (pure JDK), `AwsSignatureVersion4`-Refactor, `Authorization` (V4/V2, Header+Query), Clock-Skew, `RestUtils`-V2-Spec, `S3RequestHeaderImpl.getExpect`-Fix | P1 |
| 3 | Chunked-Upload: `S3ChunkedInputStream`-Rewrite + `SigV4ChunkVerifier`, `FSRepository`-Anbindung | P2 |
| 4 | Tests (Unit) gegen offizielle AWS-Spezifikations-Vektoren (GET/PUT/Presigned/Chunk-Beispiele der AWS-Doku) + Negativtests | P2/P3 |
| 5 | API-Ergänzungen: Bulk-Delete, ListObjectsV2 (KeyCount, ContinuationToken), response-Overrides | P2 |
| 6 | Modernisierung Abhängigkeiten: Guava raus, JAXB konsolidieren, `Encoding`→HexFormat, JUnit 5, jetty/Osgi-Starter | P1 |
| 7 | Neue API-Flächen (Bulk-Delete/ListV2/Overrides) | P5 |
| 8 | CI (GitHub Actions JDK 21), Publish-Workflow-Fix, README-Update, Travis-Ausmusterung | P1–P7 |
| 9 | **Modul `ladon-s3-server-vfs`**: versionierende Storage (Vorbild „Papers" (Dropbox)), Tests, Boot-Integration (Test-Bean) | P1 |
| 10 | Full-Build + komplette Test-Suite, Bugfixes, Doku final, Bericht | alle |

## Risiken & Gegenmaßnahmen
- **Produktiv-Kompatibilität**: `S3Repository`-Interface unverändert; FS-Verhalten bleibt
  identisch, nur Chunked-Erkennung wird korrigiert (Bugfix, nicht Bruch).
- **JAXB-Runtime in OSGi**: Glassfish `jaxb-runtime` (OSGi-fähig) ersetzt ServiceMix-Bundle;
  OSGi-Laufzeit kann hier nicht getestet werden → als „nicht verifiziert" kennzeichnen.
- **AWS-SigV4-Details** (Multi-Value-Header, UriEncode): strikt an offizielles AWS-Doku-Beispiel
  binden, Tests mit den exakten Vektoren aus der Doku.
- **Parallele Schreibzugriffe** auf Dateien: Umsetzung sequenziell, keine parallel schreibenden
  Subagents; Subagents nur für Analyse/Review.
