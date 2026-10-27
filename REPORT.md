# Ladon S3 Server 2.5.2 → 2.6.0 — Abschlussbericht

**Branch**: `agenticUpdate` · **Datum**: 2026-10-27 · **Build**: `mvn -B -ntp clean verify` (alle 7 Module, **125 Tests, 0 Fehler**, 1 bewusst `@Disabled`)

## Executive Summary

- **AWS-Signaturkompatibilität spec-konform**: SigV4 (Header, Presigned-URLs, Skew/Expires-Window,
  STS, UNSIGNED-PAYLOAD) und SigV2 (Header + Presigned-URLs) wurden an der AWS-Spezifikation
  ausgerichtet und mit der **offiziellen AWS-SigV4-Referenzsuite (31 Fälle)** sowie
  nachgerechneten Signatur-Vektoren (Python) verifiziert. SigV2 war bisher für reale AWS-Klienten
  nicht korrekt signierbar (Positional-Lines-Whitespace-Mutation).
- **9 Produktions-Bugs** aus der Test-P4-Auswertung behoben (Chunked-Stream-EOF-Vertrag,
  `NumberFormatException`→`IOException`, SigV2-Date-Zeile, SigV2-Subressourcen-Namen,
  SigV2-Presigned-Zukunftsgrenze, V4-Future-Rejection, u.a.).
- **Chunked Upload (SigV4 streaming)**: End-to-End verifiziert (Signatur-Kette, 0-Chunk-EOF,
  MD5/Content-Length-Kontrolle) — in `FSRepository` und neuen `VFSRepository`.
- **Abhängigkeiten reduziert**: Guava komplett entfernt, JAXB auf `glassfish:jaxb-runtime`
  (bind-api 4.0.2), JUnit 5 + Surefire 3.2.5, slf4j 2.0.16; Spring Boot **4.0.2 → 4.1.2**
  (schließt spring-web **CVE-2026-41854**, s. D23).
- **API-Ergänzung (additiv)**: Bulk-Delete `POST ?delete`, ListObjectsV2 (`list-type=2`,
  `continuation-token`, `key-count`), `response-*`-Overrides (Content-Type, Disposition,
  Encoding, Language, Cache-Control, Expires).
- **Neues Modul `ladon-s3-server-vfs`** (T34–T39): content-addressed Storage
  (SHA-256-Dedup), vollständige AWS-Objektversionierung (Versionen, Delete-Marker, `?versionId`,
  `listVersions`, permanente Versions-Delete, Copy mit Blob-Sharing), Mark-and-Sweep-GC,
  Cross-Prozess-Locking (`flock` + JVM-Monitors — `OverlappingFileLockException` des JDK
  behandelt), Platte-Bug-Fixes durch Integrationstest (AWS-SDK gegen echten HTTP-Stack auf Boot, Port).
  — **25/25 Unit-Tests + 1/1 Integrationstest (Spring Boot, Port 8081) grün.**
- **OSGi-Laufzeit funktional verifiziert** (T43): echtes **Apache-Felix-7.0.5**-Framework,
  Bundle Resolve/Start + Service-Registry + Whiteboard-Wiring (S3Repository→S3Servlet) +
  `FSRepository`-Roundtrip + SigV4-Authentifizierung im Bundle-Kontext — **5/5 grün**.
- **CI/Doku**: `.github/workflows/ci.yml` (JDK 21, push+PR), `maven-publish.yml` (JDK 11→21,
  Actions v3→v4), `.travis.yml` entfernt, README (CI-Badge, „Supported Operations", „Limitations"
  inkl. Versioning-per-VFS), `TASKS.md` / `PROGRESS.md` / `DECISIONS.md` final.
- **Limitationen (bewusst)**: Multipart-Upload-Object-API & Browser POST-Policy **nicht**
  implementiert. Versioning-Configuration-Endpoints (`PUT/GET ?versioning`) nicht implementiert
  (Versioning ist im VFS-Repository immer aktiv).

## Was geändert wurde

### P1–P3 (T01–T12) — Projekt-Hygiene, Signatur-Kern, Chunked-Upload
- Version 2.6.0, Root-POM-Namensfix (`s3-server-boot`→`ladon-s3-server`), Surefire-Pin.
- `SigV4.java` (pure JDK): `hmacSha256`/`sha256Hex`/`signingKey`/`uriEncode`/
  `canonicalQueryString`/`canonicalHeaderValue`/Header-Wert-Normalisierung (Multi-Value,
  Whitespace-Kollaps).
- `AwsSignatureVersion4`: auf `SigV4` umgestellt, Canonical-Request (Header) + Query (sortiert,
  UriEncode), fehlende signierte Header → `MissingSecurityHeader` (403 statt NPE/500).
- `Authorization`: V4-Header, V4-Query (Presigned, `X-Amz-Expires` 1..604800, Ablauf/Skew),
  V2-Header, V2-Query (neu), korrekte AWS-Fehler-Codes.
- `SigV2` (Auszug auf Spez, verifiziert mit Python): Positional-Lines wörtlich (Trim,
  **keine** SigV4-Comma-Regel → `Thu, 24 Feb …` bleibt so), `x-amz-date`-Präzedenz, Subressourcen
  in **kanonischer camelCase-Form**, `CanonicalizedAmzHeaders` (klein, sortiert, Comma-Join
  ohne Spaces, Wert-Trim).
- `S3ChunkedInputStream` (rewrite): Chunk-Parsing, `read(byte[])`/`available()`, EOF nach
  0-chunk, oversized/non-hex Chunk-Size → `IOException` (400-Routen).
- `SigV4ChunkVerifier`: Ketten-Signatur seed = Request-Signatur (AWS-Sample-Signer-Oracle).
- `FSRepository`: `isChunked` nur über `x-amz-content-sha256 == STREAMING-AWS4-HMAC-SHA256-PAYLOAD`
  (bisher: `Authorization.startsWith("AWS4")` — Bug), MD5/Content-Length-Verify, Cleanup.
- `S3RequestHeaderImpl.getExpect()`-Typo behoben.

### P4 (T13–T19) — Unit-Tests (AWS-Vektoren) — **Core-Modul 71/71 grün**
- **T14: offizielle AWS-SigV4-Referenzsuite** (31 Fälle, `src/test/resources/aws4testsuite`),
  alle signierten — Whitespace-Kollaps-Regel der Canonical-Header (Produktions-Fix
  `SigV4.canonicalHeaderValue`), Multi-Value-Header + Duplikat-Query-Parameter (S3-Presentation).
- **T15: `PresignedUrlTest`** (14): V4-Query-Semantik, Ablauf/Skew-Fenster, unbekannte Keys,
  `MissingSecurityHeader`, statischer Cross-Implementierungs-Vektor (Python,
  `8c4695f32dbcac50f004c1a82b189d581bc5fb3b1be58fd3a7ab1452bbca0fef`).
- **T16: `ChunkVerifierTest`** (4): Ketten-Signatur + Tamper-Erkennung.
- **T17: `SigV2Test`** (12): StringToSign-Vektoren (Python-nachgerechnet,
  `V4Dd8WsCQpayfLQF30IWx75UqaA=`, `6odoAkf/c+0vfQ6QX6jX2JRh3W4=`, `NXTxfpJ57/EWdnx0XI3OYwshZYs=`),
  Header- + Query-Akzeptanz, `x-amz-date`-Präzedenz, Fehler-Codes.
- **T18: `ChunkedStreamTest`** (17): Framing, `read()`/`read(byte[])`/`available()`,
  EOF/Fehlerpfade.
- **T19: `SigV4UtilsTest`** (19): `uriEncode`/`sha256Hex`/`signingKey`/`canonicalQueryString`/
  `canonicalHeaderValue` gegen Referenzwerte.

### P4.5 — Produktions-Bugs (von P4-Tests gefunden, behoben)
1. `S3ChunkedInputStream`: `read()` nach 0-chunk EOF → `EOFException` statt `-1` → `finished`-Guard.
2. `S3ChunkedInputStream`: oversized/non-hex Chunk-Size → `NumberFormatException` (500) →
   validiert → `IOException` (400).
3. `verifyHeaderV2`: `Date`-Zeile mit leerem String signiert → jetzt mit `Date`-Header-Wert
   (oder `x-amz-date` bei Präzedenz).
4. SigV2 `x-amz-date`: Wert in Date-Zeile, **nicht** zusätzlich in `CanonicalizedAmzHeaders`.
5. SigV2 Subressourcen: **kanonische camelCase-Namen** (`partNumber`, `uploadId`, …) —
   Multi-Part-SigV2-Requests waren sonst nie gültig.
6. SigV2 Positional-Lines: wörtlich (keine SigV4-Comma-Regel) — `Thu, 24 Feb …` bleibt so.
7. SigV2 Presigned: `Expires` > 15 min Zukunft wurde abgelehnt → entfallen (AWS: bis 7 Tage).
8. SigV4 Presigned: unparsebares `X-Amz-Date` → `IllegalArgumentException` (500) → `InvalidSecurity` (403).
9. SigV4-V4-Query: `x-amz-content-sha256`-Payload-Hash-Check korrekt (bisher NPE-Risiko bei
   fehlendem `content-type`).

### P5 (T20–T23) — API-Ergänzung (additiv)
- Bulk-Delete `POST ?delete` (JAXB `Delete`/`DeleteResult`, pro-Key `Deleted`/`Error`).
- ListObjectsV2: `KeyCount`, `ContinuationToken` (alias für marker), `NextContinuationToken`.
- `response-*`-Overrides in `FSRepository.getObject` (6 Header).
- `S3Servlet`: Bulk-Dispatch, Guava raus, `StandardCharsets`.

### P6 (T24–T28) — Modernisierung
- Guava komplett entfernt (RestUtils, AuthKeyGen, S3ACLGranteeImpl, FSRepository, POMs).
- JAXB konsolidiert: fs → `org.glassfish.jaxb:jaxb-runtime 4.0.5` + `bind-api 4.0.2`;
  OSGi-Starter → `jaxb-runtime`.
- JUnit 5 + Surefire 3.2.5 in Core.
- slf4j 2.0.16.
- **T27**: Jetty auf **12.1.14** (12.1.15 existiert nicht — die 12.1.x-Linie endet bei 12.1.14).

### P7 (T29–T33) — CI/Doku
- `.github/workflows/ci.yml`: JDK 21 (Temurin), `mvn -B -ntp clean verify`, auf push+PR.
- `maven-publish.yml`: JDK 11 → 21, Actions v3 → v4.
- `.travis.yml` entfernt.
- README: CI-Badge (branch `master`), „Supported Operations" (inkl. VFS-Versionierung),
  „Limitations" (Multipart, POST-Policy, Versioning-Config-Endpoints).

### P8 (T34–T39) — Modul `ladon-s3-server-vfs` (neu)
- **Storage-Layout** (content-addressed, Dedup ist strukturell):
  - `data/<sha256hex>` — immutable Blobs; identisches Content wird **einmal** gespeichert
    (über Keys *und* Buckets hinweg).
  - `buckets/<bucket>/objects/<keyhash>/{key, head, versions/<versionId>}`,
    `keyhash = hex(SHA-256(bucket + "\n" + key))`.
  - `versions/<versionId>` = eine Zeile `V1\tsha256hex\tmd5hex\tcreatedMs\tsize\t0|1\tcontentType`
    (Marker: Hash-Felder leer, `1`). `head` = aktuelle `versionId` (Objekt **oder** Marker).
  - `userdb.txt` im Repo-Root: Plain-Text `accessKey\tsecretKey\tuserId\tuserName`
    (bewusst **kein JAXB** — Abhängigkeits-Minimierung); first-Read initialisiert den
    Default-Demo-User mit den Identischen Credentials wie `FSRepository`.
- **`VFSRepository`** (implementiert das unveränderte `S3Repository`-Interface):
  - **PUT**: chunk-aware (ebendieselbe `S3ChunkedInputStream` + `SigV4ChunkVerifier` wie FS),
    MD5/Content-Length-Verify, SHA-256-Dedup über Temp-File + atomic move, neue Version +
    head-Umstellung unter exklusivem Lock.
  - **GET**: head oder `?versionId=` (case-insensitiv aus `getAllParams()`); Marker →
    `NoSuchKey` (404), fehlende Version → `NoSuchVersion`, `response-*`-Overrides.
  - **DELETE** ohne `versionId` → **Delete-Marker** (neue Version, `x-amz-delete-marker: true`),
    mit `versionId` → **permanente** Version-Entferung, head-Rückzug (neueste verbleibende
    Version; letztes Objekt → Key wegfällt) **und** automatischem GC-Trigger.
  - **COPY**: Quelle = head-Version, teilt den Blob (keine Datenkopie), Ziel = neue Version;
    Quell-Bucket nicht vorhanden → `NoSuchBucket`, Quell-Marker → `NoSuchKey`.
  - **LIST**: normal (head-Liste, Marker-Keys raus, `DelimiterUtil`, `max-keys`, `prefix`)
    und **`?versions`** — keys aufsteigend, Versionen absteigend (newest first), `isLatest`,
    `key-marker`/`version-id-marker`-Pagination (AWS-Semantik, inkl. „nur key-marker" =
    ganzer Key wird übersprungen).
  - **BUCKETS**: `createBucket` idempotent (AWS), `deleteBucket` leer oder `BucketNotEmpty`,
    danach GC; `listAllBuckets`.
- **Locking** (`VFSLockManager`), `locks/<bucket>/<keyhash>`:
  1. **JVM-intern**: Per-Path-JVM-Monitors — zwei Threads derselben JVM, die parallel `tryLock`
     auf dieselbe Datei rufen, erzeugen eine **`OverlappingFileLockException`** (statt `null`)
     — empirisch reproduziert (`/tmp/opencode/LockRepro.java`: 4 Fehler in 100 Versuchen).
     Die JVM serialisiert darum die Lock-Anfragen pro Datei.
  2. **Inter-Prozess** (`flock(2)`): `FileChannel.tryLock(0, MAX, shared)`, Retry-Loop (10 ms)
     bis Timeout (30 s) → `OperationAbortedException`; der OS release die Lock automatisch
     bei Prozessende — **keine verwaisten Lock-Dateien**.
  - Die `OverlappingFileLockException` wird im Retry-Loop behandelt, nie an Kaller durchgereicht.
- **GC** (`VFSBlobGC`, mark-and-sweep): `referencedBlobs()` (aus allen Version-Entries) vs.
  `data/` — unreferenzierte Blobs werden entfernt; Trigger nach **permanenter** Delete und
  `deleteBucket`, manuell via `gc()` (Count).
- **Version-Ids** (`v<base36(ms)><-seq&0xFFFF><3 random>`): monoton wachsend, eindeut pro
  (Repo, ms), valid als Dateiname (`^[A-Za-z0-9_-]{3,64}$` — Path-Traversal-Ids werden
  konsequent abgelehnt, `NoSuchVersion`).

### T43 (P8.5) — OSGi-Laufzeit funktional verifiziert
- `OsgiFunctionalTest` im Modul `ladon-s3-server-osgi-starter` bootet ein echtes
  **Apache-Felix-7.0.5**-Framework, installiert+startet die drei Ladon-Bundles (core, fs,
  starter) und beweist die OSGi-Integration **im Bundle-Kontext**: Bundle Resolve/-Start,
  geteilte System-Packages (servlet/slf4j/jaxb → eine Klassen-Identität), Service-Registry +
  Whiteboard-Wiring (S3Repository-Registrierung im fs-Bundle, Lookup im starter-Bundle,
  Injektion in `S3Servlet` via `LoggingRepository`), `FSRepository`-Roundtrip (Bucket/Object
  Create/List/Get/Delete; JAXB + slf4j) und SigV4-Authentifizierung (positiv + verfälschte
  Signatur → `InvalidSecurity`). — **5/5 Tests grün**.
- **Wichtige Erkenntnis (OSGi-R8 Service-Visibility)**: ein Service ist nur für Kaller-Bundles
  sichtbar, die die Service-Interface-Klasse mit **gleicher Identität** auflösen können
  (Felix: `ServiceReference.isAssignableTo`). Lookups aus dem *System-Bundle*-Kontext auf
  `de.mc.ladon.*`-Services schlagen deshalb (zutreffend) fehl; die Tests nutzen die richtigen
  Bundle-Perspektiven (fs-Provider / starter-Konsument). Ein „Service fehlt"-Fund aus dem
  System-Bundle-Kontext wäre ein **Fehlalarmsignal**.

## Testübersicht (Stand `agenticUpdate`, `mvn -B -ntp clean verify`)

| Modul / Suite | Tests | Ergebnis |
| --- | --- | --- |
| `ladon-s3-server` (Core) — SigV4-Suite (31), Presigned (14), SigV2 (12), Chunk (17+4), Utils (19), Delimiter (1), AuthKeyGen (1) | **71** | ✅ |
| `ladon-s3-server-vfs` — VFSRepositoryTest (Bucket-Lifecycle, PUT/GET head/versionId/etag, Dedup, GC, Copy, Marker-Flow, permanente Delete, `listVersions`-Pagination, Prefix/Delimiter, `response-*`, User, 5×12 Concurrent, Path-Traversal) | **25** | ✅ |
| `s3-server-boot` — FSStorageTests (AWS-SDK-v1, Port 8080), **VFSStorageTests** (AWS-SDK-v1, Port 8081, `@Primary` VFS-Bean), S3ServerApplicationTests (21), FileEncryptor (1) | **24** (1 skipped) | ✅ |
| `ladon-s3-server-osgi-starter` — OsgiFunctionalTest (Felix 7.0.5) | **5** | ✅ |
| **Total** | **125** (1 skipped) | **✅** |

## Wichtige Erkenntnisse / Found-Bugs

- **SigV2 war für reale AWS-Klienten nicht korrekt signierbar** (Whitespace-Mutation der
  Positional-Lines, `x-amz-date`-Doppel-Aufnahme, nicht-kanonische Subressourcen). Alle
  Dokument-Beispiele reproduzieren jetzt byte-exakt (Python-referenziert).
- **`OverlappingFileLockException` (JDK)**: zwei Threads derselben JVM, die parallel `tryLock`
  auf dieselbe Datei rufen, erzeugen eine `OverlappingFileLockException` (statt `null`) —
  empirisch reproduziert. `VFSLockManager` behandelt sie im Retry-Loop (nie an Kaller).
- **`StreamUtils.copy` wirft `InterruptedException`** — in `VFSRepository.writeContent`
  korrekt gecatcht (Produktions-500-Vermeidung).
- **`S3ResponseHeaderImpl.setContentLength` auf PUT-Antworten**: setzt den Content-Length-Header,
  aber die Antwort hat keinen Body → Client wartet auf N Bytes (50 s Socket-Timeout,
  `SocketException`). `VFSRepository.createObject` setzt sie bewusst nicht (wie `FSRepository`),
  `appendHeaderToResponse` schreibt sie nur wenn `!= null`.
- **OSGi-R8 Service-Visibility** (s. o.) — System-Bundle-Lookups sind **Fehlalarmsignale**.
- **JDK-13-Feature** in VFS-Test-Code: `TreeMap(Comparator, Map)` ist Java 13+ — wurde in
  Java-8-kompatible Form gebracht (Core baut mit `target 8`).

## Limitationen (bewusst, 2.6.0)

- **Multipart-Upload-Object-API** (`POST /?uploads`, `PUT /?partNumber`, `POST /?uploadId`) —
  **nicht** implementiert (SigV4 *streaming* ist vorhanden, die Object-Multipart-API nicht).
- **Browser POST-Policy** (`POST /bucket?policy`) — **nicht** implementiert.
- **Versioning-Configuration-Endpoints** (`PUT/GET ?versioning`) — **nicht** implementiert;
  Versioning ist im VFS-Repository immer aktiv (kein On/Off-Schalter).
- **ACLs / Bucket-Policies / Requester-Pays / Torrent / Website** — nicht unterstützt.
- **Authentication**: pro User-Key, keine feingranulare ACL-Bewertung.

## Build & Test

```bash
mvn -B -ntp clean verify        # alle 7 Module, 125 Tests, 0 Fehler
```

CI: `.github/workflows/ci.yml` (JDK 21 Temurin, push+PR), `maven-publish.yml` (JDK 21).

## Limitations für dieses Sprint (Zurückgestellt)

- Block-Level-Dedup (VFS: nur Datei-Level) — kann via Refinement ergänzt werden.
- Encryption (VFS: Plaintext-Blobs, `FileEncryptor`-Interface kompatibel).
- `?versioning=`-On/Off-Schalter (VFS: immer aktiv).
- Multipart-Upload-Object-API, POST-Policy (beiden Repositories).
