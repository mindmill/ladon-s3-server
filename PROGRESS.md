# Progress

## Baseline (2026-10-26, Branch `agenticUpdate`, Tag 2.5.2)
- `mvn -B clean verify`: ladon-s3-server, ladon-s3-server-fs, s3-server-boot, osgi-starter OK.
- `ladon-s3-server-jetty`: **FAIL** — `ladon-s3-server-fs:2.4.1` nicht resolvable (Version-Mismatch).
- Tests: 24 runs, 24 ok, 1 skipped (6GB-Stress-Test, `@Disabled`).
- Abhängigkeiten Core: Guava 33, JAXB-ServiceMix 2.2.11, javax.xml.bind (Encoding), JUnit4.
- Findings (vor Änderungen):
  - SigV4 Presigned URLs (Query-String-Auth): **nicht implementiert** → 403 MissingSecurityHeader.
  - SigV4 Chunked-Upload: `FSRepository.isChunked()` prüft `Authorization.startsWith("AWS4")`
    → **Normal-SigV4-PUT (bekannte Länge) wird fälschlich als Chunked-Stream gelesen** → 500.
  - Chunk-Signaturen werden gelesen, aber **nie verifiziert**.
  - Multi-Value-Header (SigV4-Canonical): `trim` statt `split(",")→trim→join(",")`.
  - fehlende signierte Header → NPE → 500 statt 403.
  - `response-*-`-Parameter werden nicht angewendet.
  - `S3RequestHeaderImpl.getExpect()` liest `Authorization`.
  - `S3Constants.java`: aufgelöste Merge-Konflikt-Blöcke im Kommentar.
  - Root-POM Name `s3-server-boot`, jetty-FS-Version 2.4.1, travis-Workflow (tot), Publish-Workflow JDK 11.

## Current (Stand 2026-10-27)
- **P1/P2/P3 (T01–T12) erledigt**: Version 2.6.0, SigV4-Core (Header/Query/Presigned/Skew/Expires),
  SigV2 neu, Chunked-Upload-Erkennung (`x-amz-content-sha256=STREAMING…`) + Ketten-Verifikation,
  FSRepository-Verdrahtung + Cleanup.
- **P4 (T13–T19) ERLEDGT — Core-Modul 71/71 Tests grün**:
  - **T14**: offizielle **AWS-SigV4-Referenzsuite** (31 Fälle) — alle signierten,
    inkl. Whitespace-Kollaps-Regel der Canonical-Header (Produktions-Fix `SigV4.canonicalHeaderValue`).
  - **T15**: Presigned-URL-Semantik (V4-Query), inkl. statischer Cross-Implementierungs-Vektor.
  - **T16–T19**: Chunk-Verifizierer (AWS-Sample-Signer-Oracle + Spec-Vector), SigV2 (3
    nachgerechnete Vektoren), Chunk-Stream (17 Tests), SigV4-Utilities (19 Tests).
  - Die P4-Tests haben **9 Produktions-Bugs** offengelegt, die behoben sind
    (siehe TASKS.md „P4.5" und DECISIONS.md D19) — hervorzuheben: SigV2 war für
    reale AWS-Klienten nicht korrekt signiert (komplette SigV2-Ausrichtung an der
    Spezifikation, verifiziert mit den signierten AWS-Dokument-Beispielen).
- **P5 (T20–T23) erledigt**: Bulk-Delete `POST ?delete`, ListObjectsV2 (`list-type=2`,
  Continuation-Tokens, `KeyCount`), `response-*`-Overrides, S3Servlet-Dispatch.
- **P6 (T24–T28) erledigt**: Guava raus, JAXB auf `glassfish:jaxb-runtime 4.0.5`/`bind-api 4.0.2`,
  JUnit 5 + Surefire 3.2.5, slf4j 2.0.16, T27 jetty auf **12.1.14** (12.1.15 existiert nicht —
  die 12.1.x-Linie endet bei 12.1.14), Full-Build verifiziert.
- **P7 (T29–T33) erledigt**: `.github/workflows/ci.yml` (JDK 21 Temurin, push/PR),
  `maven-publish.yml` (JDK 11→21, Actions v3→v4), `.travis.yml` entfernt, README final
  (CI-Badge, „Supported Operations", „Limitations" inkl. Versioning-per-VFS),
  `DECISIONS.md`/`PROGRESS.md` konsistent.
- **P8 (T34–T39) erledigt — neues Modul `ladon-s3-server-vfs`**:
  - **Storage-Layout**: Content-addressed Blobs (`data/<sha256>` — Dedup ist strukturbedingt
    gratis), Objekt-Metadaten `buckets/<bucket>/objects/<keyhash>/{key, head, versions/<versionId>}`,
    Version-Entry 1 Zeile `V1\tsha256\tmd5\tcreatedMs\tsize\t0|1(marker)\tcontentType`,
    `userdb.txt` Plain-Text (bewusst kein JAXB — Abhängigkeits-Minimierung).
  - **`VFSRepository`** (implements `S3Repository`, interface unverändert):
    PUT=neue Version (chunk-aware `S3ChunkedInputStream`, MD5/Content-Length-Verify),
    GET=head oder `?versionId=` (Marker→`NoSuchKey`/404, unbekannte Id→`NoSuchVersion`),
    DELETE=Delete-Maker (`x-amz-delete-marker`), DELETE+`versionId`=permanente Versions-Delete
    mit Head-Rückzug + auto-GC, `copyObject` teilt den Quell-Blob (neue Version),
    `listBucket` inkl. **`listVersions`** (S3-Ordnung: keys aufsteigend, Versionen absteigend,
    `isLatest`, `key-marker`/`version-id-marker`/`max-keys`), `response-*`-Overrides,
    `getUser` (Default-Demo-User wie FS-Repository, `addUser()` admin-Getter).
  - **Locking** (`VFSLockManager`): Per-Path-JVM-Monitors + `FileChannel.tryLock` (flock,
    Cross-Process), Retry bis 30 s Timeout → `OperationAbortedException`; behandelt die
    `OverlappingFileLockException`, die dieser JDK wirft, wenn zwei Threads derselben JVM
    `tryLock` auf dieselbe Datei rufen (empirisch reproduziert mit `/tmp/opencode/LockRepro.java`).
  - **GC** (`VFSBlobGC`): Mark-and-Sweep (`referencedBlobs()`/`sweep()`), Trigger nach
    permanentem Delete + `deleteBucket`, manuell via `gc()`.
  - **T38: 25/25 Unit-Tests grün** — Bucket-Lifecycle, PUT/GET (head/versionId/etag),
    Dedup (1 Blob/3 Payloads), GC-Sweep, Marker-Flow, permanente Version-Delete + Head-Rückzug,
    `listVersions`-Pagination, Prefix/Delimiter/Marker-Listings, `response-*`-Overrides,
    User-DB, 5×12 Concurrent-Puts (keine verlorene Version), Path-Traversal-Ids abgelehnt.
  - **T39: Boot-Integrationstest `VFSStorageTests` grün** — `@TestConfiguration` mit
    `@Bean @Primary VFSRepository` auf Temp-Road (verdrängt die `@ConditionalOnMissingBean`-
    FS-Bean out-of-the-box, kein Produktiv-Code-Change), AWS-SDK-v1 auf Port 8081:
    put v1→v2, get head/`versionId`, `listVersions` (Order/`isLatest`), Dedup-Blobzählung,
    Marker→404 (`NoSuchKey`)→Revive, Versions-Delete-Historie, GC-Blobzählung (2→1→0),
    Bucket-Delete (`NoSuchBucket`).
- **T43 (OSGi-Laufzeit) erledigt**: `OsgiFunctionalTest` im Modul `ladon-s3-server-osgi-starter`
  bootet ein echtes **Apache-Felix-7.0.5**-Framework, installiert+startet die drei Ladon-Bundles
  (core, fs, starter) und beweist die OSGi-Integration **funktional**: Bundle-Resolve/-Start,
  geteilte System-Packages (servlet/slf4j/jaxb), Service-Registry + Whiteboard-Wiring
  (S3Repository → S3Servlet via LoggingRepository), FSRepository-Roundtrip (JAXB+Logging)
  und SigV4-Authentifizierung (positiv + negativ) — **5/5 Tests grün** (`mvn -pl ladon-s3-server-osgi-starter verify`).
- Wichtige Erkenntnis bei T43 (für REPORT): OSGi R8 **Service-Visibility** — ein Service ist nur
  für Kaller-Bundles sichtbar, die die Service-Interface-Klasse mit gleicher Identität auflösen können
  (`ServiceReference.isAssignableTo`). Lookups aus dem *System-Bundle*-Kontext auf `de.mc.ladon.*`-Services
  schlagen deshalb (zutreffend) fehl; die Tests nutzen die richtigen Bundle-Perspektiven.

## Next Steps
- P9 (T40–T42): Full-Build (`mvn clean verify`, alle 7 Module, jetty 12.1.14) ✅,
  `REPORT.md` (Bericht), Commits + Push nach origin.

## Blockers / Offene Punkte
- **OSGi-Laufzeit: funktional verifiziert** (T43, oben) — kein Blocker mehr.
- Multipart-Upload & POST-Policy: bewusste **nicht**-implementiert (große neue API-Fläche, Storage-Implikationen)
  → als Limitation dokumentiert.
