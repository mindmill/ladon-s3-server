/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.repository.vfs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Mark-and-sweep garbage collection for the content-addressed {@code data} area:
 * every blob referenced by any version entry of any bucket survives, all other
 * blobs are deleted. Blobs are immutable, so sweeping is safe at any time.
 */
public final class VFSBlobGC {

    private static final Logger logger = LoggerFactory.getLogger(VFSBlobGC.class);

    private final VFSLayout layout;

    public VFSBlobGC(VFSLayout layout) {
        this.layout = layout;
    }

    /**
     * Collects all blob ids ({@code hex(SHA-256)}) referenced by version entries.
     *
     * @return the referenced ids (never null)
     */
    public Set<String> referencedBlobs() {
        Set<String> referenced = new HashSet<>();
        if (!Files.isDirectory(layout.bucketsDir())) {
            return referenced;
        }
        for (Path versionFile : versionFiles()) {
            try (InputStream raw = Files.newInputStream(versionFile);
                 InputStream in = new BufferedInputStream(raw);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                VersionEntry entry = VersionEntry.parse(reader.readLine());
                if (!entry.isDeleteMarker() && !entry.getSha256().isEmpty()) {
                    referenced.add(entry.getSha256());
                }
            } catch (RuntimeException | IOException e) {
                // a corrupted entry must not break the sweep, but must be visible
                logger.warn("skipping unreadable version entry at {}", versionFile, e);
            }
        }
        return referenced;
    }

    /**
     * Deletes all blobs that are not referenced by any version entry.
     *
     * @return the number of deleted blobs
     */
    public int sweep() {
        Path dataDir = layout.dataDir();
        if (!Files.isDirectory(dataDir)) {
            return 0;
        }
        Set<String> referenced = referencedBlobs();
        int removed = 0;
        for (Path blob : blobFiles()) {
            if (Files.isRegularFile(blob) && !referenced.contains(blob.getFileName().toString())) {
                try {
                    if (Files.deleteIfExists(blob)) {
                        removed++;
                    }
                } catch (IOException e) {
                    logger.warn("could not delete unreferenced blob {}", blob, e);
                }
            }
        }
        return removed;
    }

    private List<Path> versionFiles() {
        List<Path> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(layout.bucketsDir())) {
            stream.filter(Files::isRegularFile)
                    .filter(VFSBlobGC::isVersionEntry)
                    .forEach(result::add);
        } catch (IOException e) {
            throw new IllegalStateException("could not enumerate version entries under " + layout.bucketsDir(), e);
        }
        return result;
    }

    private List<Path> blobFiles() {
        List<Path> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(layout.dataDir())) {
            stream.filter(Files::isRegularFile).forEach(result::add);
        } catch (IOException e) {
            throw new IllegalStateException("could not list blob directory " + layout.dataDir(), e);
        }
        return result;
    }

    /**
     * A version entry is a regular file directly inside a {@code versions} folder
     * (i.e. {@code buckets/<bucket>/objects/<keyhash>/versions/<versionId>}).
     */
    private static boolean isVersionEntry(Path path) {
        Path parent = path.getParent();
        return parent != null && VFSLayout.VERSIONS.equals(parent.getFileName().toString());
    }
}
