# Entscheidungen & Recherche

Legende: Status: `Entschieden` (ausgeführt) · `Offen` (ausstehend) · `Zurückgestellt` (nicht in diesem Sprint)

## D1 — SigV4 Presigned URLs (Query-String-Signatur)
- **Entschieden**: Implementierung in `Authorization` (V4-Query-Pfad) + `RestUtils`/`SigV4`-Utilities.
  - Canonical-Query: alle Query-Parameter außer `X-Amz-Signature`, sortiert, `UriEncode(key)=UriEncode(value)`.
  - Canonical-Header: nur `X-Amz-SignedHeaders`-Liste, Werte aus Request (Multi-Value normalize).
  - HashedPayload: immer `UNSIGNED-PAYLOAD` (Query-Auth hat keine Payload) (Spec: sigv4-auth-using-presigned / s3-presigned-url).
  - `X-Amz-Expires` Pflicht (1–604800), Ablaufprüfung `now > xamzDate + expires` → `RequestTimeTooSkewed`.
  - Clock-Skew: `now < xamzDate - 15min` → `RequestTimeTooSkewed`.
  - STS: `X-Amz-Security-Token` als Parameter akzeptiert (wird im Canonical-Query mit-signiert).
- **Rationale**: war bisher 403 (MissingSecurityHeader), produkt-kritisch, spec-konform umsetzbar.

## D2 — SigV4 Chunked Upload (Ketten-Signatur)
- **Entschieden**: `isChunked` wird ausschließlich über `x-amz-content-sha256 == STREAMING-AWS4-HMAC-SHA256-PAYLOAD`
  entschieden (bisher: `Authorization.startsWith("AWS4")` — Bug). Verifikation von Chunk-Signaturen
  (Kette, seed = Request-Signatur) in neuem `SigV4ChunkVerifier`. Fehler → `InvalidRequestException` (400).
  `x-amz-decoded-content-length` wird weiterhin geprüft (FSRepository).
- **Rationale**: AWS-Spec (sigv4-streaming); bisher wurden Chunk-Signaturen gelesen, nie verifiziert;
  und normale SigV4-PUTs (bekannte Länge) wurden als Chunked interpretiert → 500.

## D3 — SigV4 Canonical-Request (Header)
- **Entschieden**:
  - Multi-Value-Header: `split(',') → trim → join(',')` (statt nur `trim()`).
  - Fehlende signierte Header → `InvalidSecurityException` (statt NPE/500).
  - `host` **muss** in `SignedHeaders` sein (AWS-Pflicht), sonst `InvalidSecurity` (403).
  - Query-Parameters: sortiert (TreeMap), `UriEncode` (keine Guava), Werte aus `getParameterMap`
    (bereits URL-decoded, neu escaped — korrekt).
  - `x-amz-content-sha256`-Header wird (falls vorhanden) als Wert in CanonicalRequest-Endzeile verwendet
    (kein Canonical-Header nötig), `UNSIGNED-PAYLOAD`/`STREAMING…`-Marker akzeptiert.
- **Rationale**: AWS-Spec (sig-v4-header-based-auth); NPE war 500, jetzt 403 + klare Fehlermeldung.

## D4 — SigV2 (Header + Query)
- **Entschieden**: `RestUtils.makeS3CanonicalString` strikt nach Spec (MD5/Type/Date/x-amz-*, Sortierung,
  Subressourcen). V2-Query-Auth (Presigned) ergänzt (`AWSAccessKeyId`/`Signature`/`Expires`),
  `Expires` (Epoch-Sekunden) ersetzt die `Date`-Zeile. Clock-Skew (Date) geprüft.
- **Rationale**: Spec-konform (S3 SigV2, deprecated but supported), bisher nur Header-Pfad teils implementiert
  (Date/x-amz-date-Mapping war unklar, jetzt strikt Spec).

## D5 — Dependencies: Guava raus, JAXB konsolidieren
- **Entschieden**:
  - `com.google.guava:*` aus allen POMs entfernt (JDK 21 ersetzt: `Base64`, `HexFormat`, `MessageDigest`,
    `StandardCharsets`, eigene `SigV4.UriEncode`).
  - `org.apache.servicemix.bundles:jaxb-impl 2.2.11` aus Core-POM entfernt.
    Core braucht nur die `jakarta.xml.bind:jakarta.xml.bind-api` (API) — Implementation kommt zur Laufzeit
    vom Consumer (boot/fat-jar/OSGi) — `org.glassfish.jaxb:jaxb-runtime:4.0.x`.
  - `fs`-Modul: `com.sun.xml.bind:jaxb-impl` → `org.glassfish.jaxb:jaxb-runtime:4.0.5` (OSGi-fähig).
  - OSGi-Starter: `org.glassfish.jaxb:jaxb-runtime:4.0.5` (runtime).
  - `Encoding.toHex` → `HexFormat.of().formatHex` (keine javax.xml.bind mehr).
- **Rationale**: Projektziel „geringe Abhängigkeiten", JAXB-2.x-Bundle war OSGi-Kompatibilität mit
  veralteter Runtime → moderne, stabile Runtime, weniger Artefakte, OSGi bleibt intakt.
- **Status**: OSGi-Laufzeit funktional verifiziert (sehe D18).

## D6 — Testumgebung: JUnit 5 + Surefire-Pin
- **Entschieden**: Core-Modul auf JUnit 5 (junit-jupiter 5.10+), `maven-surefire-plugin` ≥3.2.5
  (gepinnt im Root-POM). Boot-Modul bleibt bei JUnit 5/5.10 (Spring-Boot-Parent 4.0.2).
- **Rationale**: JUnit 4 ist legacy, Surefire 2.x kann JUnit 5 nicht zuverlässig ausführen.

## D7 — Jetty-Modul-Fix
- **Entschieden**: `ladon-s3-server-fs:2.4.1 → ${project.parent.version}` (2.6.0), `jetty.version` → 12.1.x
  (zentr. verfügbar, aktuellste bekannte 12.1.x). `slf4j-simple` bleibt. `assembly-plugin` bleibt
  (fat-jar).
- **Rationale**: Build war kaputt; Versionen sollten mit dem Projekt mitlaufen.

## D8 — Bulk-Delete (`POST ?delete`)
- **Entschieden**: Neue `S3Call.deleteobjects` in `S3Servlet`, `POST /bucket?delete` → `S3Repository.deleteObject`
  pro Key; JAXB-Entity `Delete` (In) + `DeleteResult`/`Deleted`/`Error` (Out). Fehler pro Key werden
  gesammelt, Response enthält `Deleted` + `Error`-Elemente (Status 200, wie AWS).
- **Rationale**: produkt-kritisch (AWS SDKs nutzen `deleteObjects`), reine API-Ergänzung (additiv),
  nutzt vorhandene `deleteObject`-Repository-Methode (keine Storage-Änderung).

## D9 — ListObjectsV2 (`KeyCount`, `ContinuationToken`)
- **Entschieden**: `ListBucketResult` JAXB-Entity: `KeyCount` (Int, Anzahl `Contents`),
  `ContinuationToken` (optional, Echo von `continuation-token`), `NextContinuationToken` (optional,
  = `nextKeyMarker` wenn `isTruncated && listType2`). `S3RequestParams`: `continuation-token` als
  alias für `marker`. `S3RequestHeader`/S3Servlet: `?list-type=2` wird erkannt (Query-Parameter).
- **Rationale**: AWS SDK v1/v2 nutzen `listObjectsV2` (KeyCount ist Pflicht im Modell); rein additive
  XML-Felder, kompatibel mit `ListObjects`.

## D10 — `response-*-`-Overrides
- **Entschieden**: `FSRepository.getObject` wertet `response-content-type`, `response-content-disposition`,
  `response-content-encoding`, `response-content-language`, `response-cache-control`, `response-expires`
  aus (`callContext.getParams().getAllParams()`), setzt die jeweiligen Response-Header-Override-Felder
  (wenn gesetzt). Andere Repositorys können das analog übernehmen.
- **Rationale**: AWS-Presigned-Pattern (Datei-Downloads mit custom-Name/Typ); bisher ignoriert.
- **Nicht implementiert**: `response-` für `headBucket`/`listBuckets` (unüblich).

## D11 — Multipart & POST-Policy: Zurückgestellt
- **Entschieden (Zurückgestellt)**: Multipart-Upload (Part-Management, `?uploads`), POST-Policy (Browser-Upload)
  in diesem Sprint **nicht** implementiert. Stattdessen als Limitation dokumentiert (README + REPORT).
- **Rationale**: Große neue API- + Storage-Implementierung (Part-Daten, ETag-Kombination, Cleanup);
  nicht im Kernziel „Moderner Stand + Sig-Compat + Tests". Kann in einem Follow-up-Sprint erfolgen.

## D12 — OSGi-Kompatibilität
- **Entschieden**: Bundle-Packaging bleibt für alle Module (core, fs, osgi-starter, jetty-jar).
  `Export-Package`/`Import-Package` (maven-bundle-plugin) unverändert. `osgi-starter` bekommt
  `org.glassfish.jaxb:jaxb-runtime:4.0.5` (runtime), da `jaxb-impl` (ServiceMix) aus core entfernt wird.
- **Verifiziert** (sehe D18): OSGi-Laufzeit funktional mit Apache Felix 7.0.5 getestet (`OsgiFunctionalTest`).

## D13 — VFS (Versionierung, Vorbild „Papers" (Dropbox))
- **Entschlossen (Design)**:
  - Content-addressed, immutable Blobs: `data/<sha256>` (Dedup über Identität).
  - Referenzen: `refs/<sha256>/<versionId>` (leere Marker-Datei). GC: Blob + Verzeichnis wird entfernt,
    wenn `refs/<sha256>/` leer wird (Atomic-Op, FSLock-Modell).
  - Versions-Log pro Key: `objects/<key>` (Zeilen: `versionId|timestamp|size|etag|sha256|deleted`).
  - Head-Pointer implizit = letzte Zeile. Delete ohne versionId = Delete-Marker (neue Zeile `deleted=true`).
  - `getUser`/`listAllBuckets`/`getBucket` (Head) analog FSRepository (User aus meta.xml, System-User als Fallback).
  - ETag = MD5(Hex) der Blob-Bytes (kompatibel mit FSRepository/SDK).
  - Locking: FSLock-Analog (Datei + Zeitstempel, User-basiert) — `vFS.Lock`, eigene Implementierung (keine
    Abhängigkeit auf fs-Modul).
- **Nicht implementiert (bewusst)**: Block-Level-Dedup (nur Datei-Level), Encryption (Plaintext-Blobs,
  kann später via `FileEncryptor`-Interface ergänzt werden), Versioning-Configuration-Endpunkte
  (`PUT/GET/DELETE ?versioning`, `PUT ?lifecycle`, `?tagging`, ACLs) — bleiben API-Limitationen in diesem
  Sprint (README/REPORT).
- **Rationale**: „Papers" (Dropbox) betont content-addressing, immutable snapshots, referenzaufgelöste
  Speichernutzung — passt gut zu S3-Versionierung (Delete-Marker als Version, PUT erzeugt Version,
  GET ohne versionId = Head = letzte Zeile).

## D14 — CI/Doku
- **Entschlossen**:
  - `.github/workflows/ci.yml`: build+test, JDK 21, push+PR.
  - `maven-publish.yml`: JDK 11 → 21 (war inkompatibel mit dem Projekt).
  - README: Travis-Badge → GH-Actions-Badge, „Supportierte Operationen" + „Limitationen" (Multipart,
    POST-Policy, Versioning-Endpoints, OSGi-Status), Dependencies-Notiz.
  - `.travis.yml` entfernen (Travis CI ist tot).
- **Rationale**: Travis-Workflow ist seit ~2021 inaktiv; GitHub-Actions existiert bereits (Publish).

## D15 — Fehler-Codes & -Semantik (AWS-kompatibel)
- **Entschlossen**:
  - Presigned URL Ablauf: `RequestTimeTooSkewed` (403), AWS-nah („ExpiredPresignedUrl"-Semantik ist
    AWS-intern `RequestTimeTooSkewed`; wir verwenden denselben Code, Message klar).
  - Unsignierte/fehlerhafte Signature: `SignatureDoesNotMatch` / `InvalidSecurity` (403).
  - Fehlende Authorization: `MissingSecurityHeader` (400) — bleibt.
  - Chunk-Signatur-Fehler: `InvalidRequest` (400).
  - Fehlende signierte Header: `InvalidSecurity` (403).
- **Rationale**: AWS-kompatible Fehler-Codes/Status, klar zuordenbar, keine neuen Codes.

## D16 — `S3RequestHeaderImpl.getExpect()`
- **Entschlossen**: `getExpect()` liest jetzt `Expect` (bisher `Authorization`). Kein Consumer gefunden,
  Fix ohne Risiko.
- **Rationale**: offensichtliche Typo, kein API-Vertrag (Header-Getter, nicht Teil des S3-Protokolls).

## D17 — `S3Constants` Merge-Konflikt
- **Entschlossen**: Konflikt-Marker aus Kommentar entfernt, Text konsolidiert.
- **Rationale:** Cosmetic, aber wichtig für Code-Revision.

## D18 — OSGi-Laufzeit funktional verifizieren (statt „nicht verifiziert")
- **Entschlossen**: Der OSGi-Nachweis wird **im bestehenden Modul `ladon-s3-server-osgi-starter`**
  erbracht (kein neues Modul), als JUnit 5 Test, der ein **echtes Apache-Felix-7.0.5-Framework** in der
  JVM startet und die drei Ladon-Bundles (`ladon-s3-server`, `-fs`, `-osgi-starter`) installiert,
  auflöst und startet. `OsgiFunctionalTest` (5 Tests) beweist anschließend **im Bundle-Kontext**:
  - Bundle Resolve+Start (alle `Import-Package`-Wires auflösbar),
  - geteilte System-Packages (servlet/slf4j/jaxb aus dem Test-Classpath, eine Klassen-Identität),
  - Service-Registry + Whiteboard-Wiring: fs-Registerung des `S3Repository`-Services, Lookup
    durch das Starter-Bundle, Injektion in die `S3Servlet` (via `LoggingRepository`),
  - `FSRepository`-Rundlauf (Bucket/Object Create/List/Get/Delete; JAXB-Marshal/Unmarshal + slf4j),
  - SigV4-Header-Authentifizierung vom Bundle-Code (positiv + verfälschte Signatur → `InvalidSecurity`).
  Ergebnis: **5/5 grün** (Felix 7.0.5, Maven `verify` der osgi-starter).
- **Umsetzungsdetails**:
  - API-Pakete (servlet, slf4j, jaxb-api) sind **System-Packages** (`org.osgi.framework.system.packages.extra`)
    → eine geteilte Klassen-Identität zwischen Test- und Bundle-Welt.
  - Eigene Bundles werden vor der Installation **relaxed** (Manifest-Umwicklung im Test):
    `Require-Capability` entfernt (z. B. slf4j 2.x zwingende `osgi.serviceloader.processor`-Extender)
    und `Import-Package`-Versionsbereiche per Regex `;version="[^"]*"` global entfernt
    (unversionierte System-Packages erfüllen nur unversionierte Anforderungen).
  - `bundle:bundle`-Execution bei `process-test-classes`, damit das zu testende Starter-Bundle existiert,
    bevor die Tests laufen (chicken-and-egg mit `package`).
  - fs-`Export-Package`: wurzel-nur → `de.mc.ladon.s3server.*` (sonst `enc`/`repository.impl` unsichtbar).
- **Wichtige Erkenntnis (im REPORT)**: OSGi R8 **Service-Visibility** — ein Service ist nur für
  Kaller-Bundles sichtbar, die die Service-Interface-Klasse mit **gleicher Identität** auflösen können
  (Felix: `ServiceReference.isAssignableTo` → `Util.isServiceAssignable` filtert in `Felix.getServiceReferences`).
  Deshalb ist das `S3Repository`-Service aus der **System-Bundle**-Perspektive unsichtbar (dort kommt die
  core-Klasse vom Framework-Classpath, bei der Provider-Bundle vom core-Bundle → getrennte Identitäten),
  aber von der fs- (Provider) und starter- (Konsument, gemeinsamer core-Provider) Perspektive aus **sichtbar** —
  genau die Perspektiven, in denen ein echter OSGi-Host (z. B. web console) die Services konsumiert.
  Die Tests nutzen diese Perspektiven; ein „Service fehlt"-Fund aus dem System-Bundle-Kontext wäre ein Fehlalarmsignal.
- **Rationale**: OSGi-Fähigkeit ist ein ausdrückliches Verkaufs-/Betriebsversprechen (Bundles seit 2.x).
  Ein reiner „Manifest statisch geprüft"-Status ist zu schwach für Produktivbetrieb; ein echter
  Resolve+Start+Service-Wiring-Rundlauf mit Felix ist die minimal ausreichende funktionale Verifikation.

## D19 — SigV2 an der AWS-Spezifikation ausgerichtet (verifiziert mit den beispielhaft
## dokumentierten, nachvollziehbar signierten Anfragen)
- **Entschlossen**: Die P4-Tests (`SigV2Test`, `PresignedUrlTest`) haben mehrere Abweichungen der
  Implementierung von der AWS SigV2-Spezifikation aufgedeckt; die Produktion wurde an der
  **AWS-Dokumenten-Beispiel-Signatur** ausgerichtet (unabhängig mit Python nachgerechnet — beide
  Dokument-Beispiele reproduzieren die dokumentierte Signatur byte-exakt):
  - **Positional-Lines** (Content-MD5 / Content-Type / Date·x-amz-date / Expires) werden
    **wörtlich** signiert (Umlaut-Trimming), ohne die SigV4-Komma-Whitespace-Regel — `Thu, 24 Feb …`
    bleibt `Thu, 24 Feb …`.
  - **`x-amz-date`-Header**: sein Wert kommt in die Date-Zeile (Präzedenz vor `Date`) und der
    Header wird **nicht** zusätzlich in `CanonicalizedAmzHeaders` aufgenommen.
  - **Subressourcen** werden mit den **kanonischen camelCase-Namen** signiert (`partNumber`,
    `uploadId`, `requestPayment`, `versionId`) — die Kleinbuchstaben-Variante signiert AWS-Klienten
    (Multi-Part-Upload!) nie korrekt; die Liste bleibt eine Superset-Liste (inkl. tagging/cors/…).
  - **`CanonicalizedAmzHeaders`**: Namen klein, nach Name sortiert, Multi-Value-Werte
    Komma-gejoint ohne Leerzeichen, Wert-Trimming — aber **ohne** SigV4-„konsekutives Whitespace zu
    einem Space"-Regel (die SigV2-Spezifikation kennt sie nicht).
  - **Presigned (V2)**: `Expires` ersetzt die Date-Zeile; gültig solange
    `now ≤ Expires` (bis 7 Tage in der Zukunft!) — die frühere „15-min-Future"-Ablehnung ist
    ein Kompatibilitätsbruch gewesen und wurde entfernt.
  - **Presigned (V4)**: gültig wenn `now > xamzDate + X-Amz-Expires` (Ablauf) bzw.
    `now < xamzDate − 15min` (Uhr weit voraus) außerhalb des Validitätsfensters —
    `X-Amz-Expires` 1..604800s, sonst `AccessDenied`.
- **Rationale**: SigV2 ist Legacy, aber laut Produkt-Vertrag weiter zu akzeptieren; „kompatibel
  genug" reicht nicht — AWS-Klienten (boto2, alte SDKs) signieren exakt nach Spezifikation, daher
  ist die Spezifikation der einzig gültige Maßstab.

## D20 — VFS: finales On-Disk-Design (abgewichen vom D13-Estskizze, umsetzungsorientiert)
- **Entschlossen** (implementiert in `ladon-s3-server-vfs`):
  - **Content-addressed Blobs**: `data/<sha256hex>` — immutable, teilt sich automatisch über
    Keys und Buckets (Dedup ist strukturell, nicht Feature).
  - **Objekt-Metadaten**: `buckets/<bucket>/objects/<keyhash>/{key, head, versions/<versionId>}`,
    `keyhash = hex(SHA-256(bucket + "\n" + key))`. Jeder `versions/<versionId>` ist eine
    Einzel-Zeile: `V1\tsha256hex\tmd5hex\tcreatedMs\tsize\t0|1(marker)\tcontentType`
    (Marker: Hash-Felder leer). `head` = aktuelle `versionId` (objekt oder Marker).
    *(Abweichung zu D13: statt `refs/<sha256>/<versionId>`-Markerdateien + zentralem
    Version-Log `objects/<key>`: pro-Key-Ordner mit `key`/`head`/`versions/` — einfacher zu
    locken, GC ist Mark-and-Sweep über Version-Entries, und es entsteht kein Hot-Dating
    einer einzigen Datei.)*
  - **Version-Ids**: `v<base36(epoch-ms)><- seq&0xFFFF><3 random>` — monoton, eindeut pro
    (Repository,Ms), valid als Datei-Name (`^[A-Za-z0-9_-]{3,64}$`, sonst `NoSuchVersion`).
  - **S3-Semantik**: `createObject`=neue Version + head-Umstellung (exklusiv-locked);
    `getObject` per `head` oder `?versionId=` — Marker→`NoSuchKey`(404), fehlende Version→
    `NoSuchVersion`, `response-*`-Overrides; `deleteObject` ohne versionId=**Delete-Marker**
    (neue Version, `x-amz-delete-marker`), mit versionId=**permanente** Version-Entferung mit
    head-Rückzug (neueste verbleibende Version, leeres Verzeichnis→Key wegfällt) + automatische
    GC; `copyObject` liest die Quelle aus der head-Version, teilt den Blob (keine Kopie) und
    erzeugt eine neue Ziel-Version; `deleteBucket` ist leer (nicht leer→`BucketNotEmpty`),
    danach GC.
  - **User**: `userdb.txt` im Repo-Root, Plain-Text `accessKey\tsecretKey\tuserId\tuserName` —
    **bewusst kein JAXB** (Abhängigkeits-Minimierung ist ein Projektziel, der fs-Modul hat das
    JAXB-Meta nur für seine Historie). Bei first-Read wird der Default-Demo-User mit den
    identischen Credentials wie `FSRepository` initialisiert, dadurch läuft der Boot-Test (AWS-SDK)
    mit den selben Schlüssel wie die bestehende Produktion.
- **Rationale**: das Ziel ist ein schlanker, OSGi-fähiger Speicher mit AWS-kompatibler
  Versionierung bei minimaler API-Flächenerweiterung (kein neues Modul-Interface).

## D21 — Inter-JVM-Locking: `OverlappingFileLockException` des JDK + per-Path-JVM-Monitors
- **Entschlossen**: `VFSLockManager` kombiniert zwei Mechanismen:
  1. **JVM-intern**: Per-Path-Monitors (`ConcurrentHashMap<Path,Object>` + `synchronized`) —
     zwei Threads derselben JVM, die parallel `tryLock` auf dieselbe Datei rufen, erzeugen eine
     **`OverlappingFileLockException`** (statt `null`) — empirisch reproduziert
     (`/tmp/opencode/LockRepro.java`, 4 Fehler in 100 Versuchen). Die JVM serialisiert darum die
     Lock-Anfragen pro Datei, bevor sie auf den OS-Lock trifft.
  2. **Inter-Prozess (flock)**: `FileChannel.tryLock(0,MAX,shared)` (advisory `flock(2)`),
     Retry-Loop (10 ms) bis Timeout (30 s) → `OperationAbortedException`. Der OS release die
     automatisch bei Prozessende — es gibt keine verwaisten Lock-Dateien.
  - Die `OverlappingFileLockException` wird im Retry-Loop behandelt, nie an den Kaller durchgereicht.
- **Rationale**: `tryLock()` allein ist intra-JVM unsicher — der JDK-Verhalten ist nicht
  dokumentiert, aber zuverlässig reproduzierbar. Die Monitors-Kombination ist deterministisch
  (kein Race), die `flock` zusätzlich Cross-Prozess (Produktion: mehrere Server-Instanzen auf
  demselben Filesystem).

## D22 — `S3ResponseHeaderImpl` + `appendHeaderToResponse` (PUT ohne Content-Length)
- **Entschlossen**: Die `VFSRepository.createObject` setzt auf der PUT-Antwort **keine**
  `Content-Length` (anders als `FSRepository` und der `S3ResponseHeaderImpl`-Default — siehe
  `S3ResponseHeaderImpl.setXamzVersionId`-Flow). Eine PUT-Antwort (200) darf in S3 keine Body
  haben; eine `Content-Length`-Header würde die AWS-SDK zum Warten auf N Bytes Body veranlassen
  (50 s Socket-Timeout, dann `SocketException`) — reproduziert im Boot-IntegTest (158 s statt 9 s).
  `appendHeaderToResponse` schreibt nur `Content-Length`, wenn `header.contentLength != null`;
  `VFSRepository` setzt sie nicht, daher ist die Antwort korrekt.
- **Rationale**: Die Produktion muss mit realen AWS-SDK-kompatibel sein; die FSRepository setzt
  dieselbe Regel (keine Content-Length auf PUT-Antworten), und der Bootstrap-Test beweist es:
  ohne diesen Fix bricht der SDK-Client nach 50 s ab (Socket-Timeout auf dem Read).
