# Tasks

Status: `[ ]` offen · `[~]` in Arbeit · `[x]` erledigt · `[-]` verworfen/beschlossen

## P1 – Projekt-Hygiene
- [x] T01 Root-POM: Name `s3-server-boot`→`ladon-s3-server`, surefire pin, Version 2.6.0
- [x] T02 Alle Module-POMs: Version 2.6.0; jetty: `ladon-s3-server-fs` 2.4.1→`${project.version}`
- [x] T03 `S3Constants.java`: Merge-Konflikt-Marker aus Kommentar entfernen

## P2 – Signatur-Kern
- [x] T04 `auth/SigV4.java`: pure-JDK-Utilities (hmac-sha256, sha256-hex, signing-key, UriEncode, canonical-query, header-value-normalisierung, sig-kontaktvergleich)
- [x] T05 `AwsSignatureVersion4`: auf SigV4 migrieren, Header-Canonicalisierung (Multi-Value, fehlende Header, host-Pflicht), Query-Canonicalisierung sortiert + UriEncode
- [x] T06 `Authorization` neu: V4-Header, V4-Query (Presigned, Expires+Skew), V2-Header, V2-Query (neue), Fehler-Codes je AWS
- [x] T07 `RestUtils`: V2-StringToSign strikt laut Spec (MD5/Type/Date/x-amz-*, Subressourcen), Guava raus
- [x] T08 `S3RequestHeaderImpl.getExpect()`-Typo behoben
- [x] T09 `Encoding.toHex` → `HexFormat` (keine javax.xml.bind mehr)

## P3 – Chunked Upload
- [x] T10 `S3ChunkedInputStream` rewrite: korrekte Chunk-Parsing, EOF-Handling, optionaler Verifikations-Hook
- [x] T11 `SigV4ChunkVerifier` (Ketten-Signatur nach AWS-Spec)
- [x] T12 `FSRepository`: `isChunked` via `x-amz-content-sha256==STREAMING…`, Verifikatoren-Verdrahtung, Cleanup bei Fehler

## P4 – Unit-Tests (AWS-Vektoren)
- [x] T13 Test-Kontext (Mock-freie `S3CallContext`-Implementierung, JDK-Proxy-Fakes für Servlet 6.1)
- [x] T14 `AwsSignatureVersion4Test`: die **offizielle AWS-SigV4-Referenzsuite** (31 Fälle,
  `src/test/resources/aws4testsuite`, Quelle: AWS-Doku „Signature v4 test suite"):
  canonical URI/Query aus `.creq`, `x-amz-content-sha256 = sha256(body)` je Fall,
  Multi-Value-Header + Duplikat-Query-Parameter (servlet-Präsentation) — **5/5 Test-Methoden grün**
- [x] T15 `PresignedUrlTest`: V4-Query-Semantik (Algorithm, Scope-`s3`, Expires 1..604800,
  Ablauf/Window, Skew, unbekannte Keys, fehlende `X-Amz-SignedHeaders`/ohne `host`,
  fehlende Signatur → `MissingSecurityHeader`, UNSIGNED-PAYLOAD-Canonical) + statischer
  cross-Implementierungs-Vektor (Python-Referenz)
- [x] T16 `ChunkVerifierTest`: Chunk-Kette gegen den AWS-Sample-Signer
  (`AWS4SignerForChunkedUpload`), unabh. Spec-Vector, Tamper-Erkennung
- [x] T17 `SigV2Test`: StringToSign-Vektoren (independent berechnet), Header- + Query-Akzeptanz,
  x-amz-date-Prezedenz, Fehler-Codes
- [x] T18 `ChunkedStreamTest`: Framing, `read()`/`read(byte[])`/`available()`,
  EOF-/Fehlerpfade (truncated Data/Header, oversized/non-hex Size → `IOException`)
- [x] T19 `SigV4UtilsTest`: `uriEncode`/`sha256Hex`/`signingKey`/`canonicalQueryString(Multi)`/
  `canonicalHeaderValue`/Marker/`hex` gegen Referenzwerte

## P4.5 – Produktions-Bugs, von den P4-Tests gefunden und behoben
- [x] `S3ChunkedInputStream`: `read()` nach dem finalen (0-Byte-)Chunk warf
  `EOFException` statt `-1` (InputStream-Vertrag) → `finished`-Guard
- [x] `S3ChunkedInputStream`: Chunk-Size `Integer.parseInt(…,16)` ließ `NumberFormatException`s
  nach oben wandern (500) → `parseChunkSize` validiert und wirft `IOException` (400-Routen)
- [x] `verifyHeaderV2` rief `computeV2(ctx, "")` auf: die Date-Zeile wurde mit leerem
  String signiert, statt mit dem `Date`-Header → jetzt `null` (und `""` heißt „kein Expires”)
- [x] SigV2 `x-amz-date`-Header wird wie in der AWS-Spezifikation behandelt: Wert in der
  Date-Zeile, Präzedenz vor `Date`, nicht noch zusätzlich in `CanonicalizedAmzHeaders`
- [x] SigV2-Subressourcen werden mit den **kanonischen camelCase-Namen** signiert
  (`partNumber`, `uploadId`, `requestPayment`, `versionId`) statt klein —
  Multi-Part-SigV2-Requests waren sonst nie gültig
- [x] SigV2 positional-Lines (MD5/Type/Date) werden wörtlich signiert — die SigV4-
  Komma-Whitespace-Regel hatte `Thu, 24 Feb …` zu `Thu,24 Feb …` mutiert (alle realen
  SigV2-Requests falsch signiert!)
- [x] `verifyQueryV2`: Presigned-URLs mit `Expires` > 15 min in der Zukunft wurden
  abgelehnt (AWS: gültig bis 7 Tage) → nur noch `now > Expires` → `RequestTimeTooSkewed`
- [x] `verifyQueryV4`: unparsebares `X-Amz-Date` warf `IllegalArgumentException` (500)
  → jetzt `InvalidSecurity` (403)

## P5 – API-Ergänzung (additiv)
- [x] T20 Bulk-Delete `POST ?delete` (JAXB `Delete`/`DeleteResult`, pro-Key Ergebnis/Fehler)
- [x] T21 ListObjectsV2: `KeyCount`, `ContinuationToken` (alias for marker), `NextContinuationToken`, `list-type=2`
- [x] T22 `response-*-`-Overrides in `FSRepository.getObject` (Content-Type, Disposition, Encoding, Language, Cache-Control, Expires)
- [x] T23 `S3Servlet`: Bulk-Dispatch, Guava raus, `StandardCharsets`

## P6 – Modernisierung
- [x] T24 Guava überall raus (RestUtils/…/AuthKeyGen/S3ACLGranteeImpl/FSRepository), kein Guava in POMs
- [x] T25 JAXB: ServiceMix-Bundle aus core-POM raus; fs→`org.glassfish.jaxb:jaxb-runtime`; OSGi-Starter→`jaxb-runtime`
- [x] T26 JUnit 5 in Core-Modul (DelimiterUtil, TestAuthKeyGen), pom junit-jupiter, surefire
- [x] T27 jetty: Version → **12.1.14** gesetzt (ee11-servlet; 12.1.15 existiert nicht — die 12.1.x-Linie endet bei 12.1.14), Full-Build verifiziert
- [x] T28 slf4j: 2.0.16 in Core/Fs-Modulen

## P7 – CI/Doku
- [x] T29 `.github/workflows/ci.yml`: build+test, JDK 21 (Temurin), push/PR, `mvn -B -ntp clean verify`
- [x] T30 `maven-publish.yml`: JDK 11 → 21, Actions v3 → v4
- [x] T31 README: CI-Badge (GH Actions), „Supported Operations", „Limitations" (Multipart, POST-Policy, Versioning per VFS-Modul)
- [x] T32 `.travis.yml` entfernt
- [x] T33 `DECISIONS.md`/`PROGRESS.md` final

## P8 – Modul VFS (Versionierung, Vorbild „Papers" (Dropbox))
- [x] T34 `ladon-s3-server-vfs`-Modul: POM (bundle 5.1.2, wie fs), Root-POM-Module, Dependency nur core
- [x] T35 Layout + Datenmodell: content-addressed Blobs (`data/<sha256>` — Dedup ist strukturell),
  `buckets/<bucket>/objects/<keyhash>/{key, head, versions/<versionId>}` (Ein-Zeilen-`VersionEntry`
  `V1\tsha256\tmd5\tcreatedMs\tsize\t0|1\tcontentType`), `userdb.txt` (Plain-Text, bewusst kein JAXB)
- [x] T36 `VFSRepository` (implements `S3Repository`): createBucket/deleteBucket (BucketNotEmpty)/
  listBucket + **listVersions** (S3-Ordnung, isLatest, Marker-Pagination)/listAllBuckets/
  getObject (head/`versionId`/Marker→404)/createObject (chunk-aware, MD5/Length-Verify, SHA-256-Dedup)/
  copyObject (geteilter Blob)/deleteObject (Marker + permanente Versions-Delete + Head-Rückzug)/getUser
- [x] T37 Locking (`VFSLockManager`: per-Path-JVM-Monitors + `FileChannel.tryLock` (flock) für
  Cross-Process, Retry bis Timeout; behandelt `OverlappingFileLockException` des JDK bei intra-JVM-
  Konkurrenz — empirisch reproduziert `/tmp/opencode/LockRepro.java`) + GC (`VFSBlobGC` mark-and-sweep,
  Auto-Trigger nach permanentem Delete/deleteBucket, `gc()` public)
- [x] T38 Unit-Test (VFS): **25/25 grün** — full-life-cycle (PUT v1→v2, head/versionId-Get, listVersions
  Order/Pagination, delete→marker→revive, permanente Version-Delete, GC-Sweep, Dedup 1-Blob/3-Payloads,
  copy-Blob-Share, Prefix/Delimiter/Marker-Listings, response-Overrides, User-DB, 5×12 Concurrent-Puts,
  Path-Traversal-Id-Verwerfung)
- [x] T39 Integrationstest (Boot-Modul): `VFSStorageTests` — `@TestConfiguration` mit `@Bean @Primary
  VFSRepository` (verdrängt die `@ConditionalOnMissingBean`-FS-Bean), AWS-SDK-v1, Port 8081:
  put v1→v2, get head/versionId, listVersions (Order/isLatest), Dedup, marker→404→revive,
  Versions-Delete-Historie, GC (Blob-Zählung), Bucket-Delete — **1/1 grün**
  (SDK v1, nicht v2: bereits im Modul als Test-Dependency vorhanden)

## P8.5 – OSGi-Laufzeit-Verifikation (Ergänzung, im Bestandsmodul `ladon-s3-server-osgi-starter`)
- [x] T43 OSGi-Funktionstest mit echtem Apache-Felix-7.0.5-Framework in JUnit 5:
  Bundle Resolve+Start (core, fs, osgi-starter), Shared-System-Packages (servlet/slf4j/jaxb),
  Service-Registry + Whiteboard-Wiring (S3Repository-Injection in S3Servlet über LoggingRepository),
  FSRepository-Roundtrip (JAXB/Logging) und SigV4-Verifikation (pos.+neg.) **im Bundle-Kontext** — 5/5 grün
- [x] Begleitende Fixe: fs `Export-Package` root→`de.mc.ladon.s3server.*`; `bundle:bundle`-Execution
  für `process-test-classes` (Bundle existiert vor den Tests); Relax-Mechanik im Test
  (Versionsbereiche in `Import-Package` + `Require-Capability` entfernt, API-Pakete als System-Packages)

## P9 – Abschluss
- [x] T40 Full `mvn clean verify` (alle 7 Module), alle Tests grün
  (Core 71, VFS 25, Boot 24 (1 skipped), OSGi 5) — inkl. jetty 12.1.14
- [x] T41 `PROGRESS.md` final, `REPORT.md` (Bericht)
- [x] T42 Commits auf `agenticUpdate`, push nach origin
