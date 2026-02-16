package com.securefromscratch.busybee.storage;
import org.owasp.untrust.boxedpath.BoxedPath;
import org.owasp.untrust.boxedpath.PathSandbox;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class FileStorage {

    public enum FileType {
        IMAGE,
        PDF,
        OTHER
    }

    private static final long DISK_RESERVE_BYTES = 1024; // 1KB reserve

    private static final Set<String> ALLOWED_EXT = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".webp", ".pdf"
    );

    private final Path m_storagebox;
    private final Path m_uploadsDir;
    private final PathSandbox m_sandbox;

    public FileStorage(Path storageDirectory) throws IOException {
        m_storagebox = storageDirectory.toAbsolutePath();
        if (!Files.exists(m_storagebox)) {
            Files.createDirectories(m_storagebox);
        }

        m_uploadsDir = m_storagebox; // storageDirectory IS the uploads dir
        if (!Files.exists(m_uploadsDir)) {
            Files.createDirectories(m_uploadsDir);
        }
        m_sandbox = PathSandbox.boxroot(m_uploadsDir.toString());

    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }

        String original = file.getOriginalFilename();
        String extension = extractExtension(original).toLowerCase(Locale.ROOT); // includes dot

        if (!ALLOWED_EXT.contains(extension)) {
            throw new IllegalArgumentException("File type not allowed");
        }

        long size = file.getSize();
        FileStore store = Files.getFileStore(m_uploadsDir);
        long usable = store.getUsableSpace();
        if (usable < size + DISK_RESERVE_BYTES) {
            throw new IllegalArgumentException("Not enough disk space");
        }

        try (InputStream raw = file.getInputStream();
             BufferedInputStream in = new BufferedInputStream(raw)) {

            in.mark(64);
            byte[] header = readHeader(in, 32);
            in.reset();

            verifyMagicBytes(extension, header);

            // Unique filename to prevent overwrite
            String storedFilename = UUID.randomUUID() + extension;

            // destination is sandbox-rooted: just filename (no "uploads/" needed)
            String relativePath = Path.of("uploads", storedFilename).toString();
            BoxedPath destinationFile = m_sandbox.of(relativePath);

            Files.copy(in, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return storedFilename;
        }
    }

    public byte[] getBytes(String filename) throws IOException {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Empty filename");
        }

        // Allow only files that look like "uuid.ext" and ext is allowed
        String ext = extractExtension(filename).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("File type not allowed");
        }

        // Basic UUID format check (optional but useful hardening)
        String base = filename.substring(0, filename.length() - ext.length());
        try {
            UUID.fromString(base);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid filename");
        }

        BoxedPath p = m_sandbox.of(filename);
        return Files.readAllBytes(p);
    }

    public static FileType identifyTypeFromStoredName(String storedFilename) {
        String ext = extractExtension(storedFilename).toLowerCase(Locale.ROOT);
        if (ext.equals(".pdf")) return FileType.PDF;
        if (ext.equals(".png") || ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".gif") || ext.equals(".webp")) {
            return FileType.IMAGE;
        }
        return FileType.OTHER;
    }


    private static String extractExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) return "";
        return filename.substring(lastDot); 
    }

    /* ---------------- magic-bytes helpers ---------------- */

    private static byte[] readHeader(InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int off = 0;
        while (off < n) {
            int r = in.read(buf, off, n - off);
            if (r == -1) break;
            off += r;
        }
        if (off == n) return buf;
        byte[] trimmed = new byte[off];
        System.arraycopy(buf, 0, trimmed, 0, off);
        return trimmed;
    }

    private static void verifyMagicBytes(String extWithDot, byte[] header) {
        boolean ok = switch (extWithDot) {
            case ".png" -> isPng(header);
            case ".jpg", ".jpeg" -> isJpeg(header);
            case ".gif" -> isGif(header);
            case ".webp" -> isWebp(header);
            case ".pdf" -> isPdf(header);
            default -> false;
        };

        if (!ok) {
            throw new IllegalArgumentException("File content does not match extension");
        }
    }

    private static boolean startsWith(byte[] data, byte[] sig) {
        if (data.length < sig.length) return false;
        for (int i = 0; i < sig.length; i++) {
            if (data[i] != sig[i]) return false;
        }
        return true;
    }

    private static boolean isJpeg(byte[] h) {
        return h.length >= 3
                && (h[0] & 0xFF) == 0xFF
                && (h[1] & 0xFF) == 0xD8
                && (h[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] h) {
        return startsWith(h, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
    }

    private static boolean isGif(byte[] h) {
        return startsWith(h, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                || startsWith(h, "GIF89a".getBytes(StandardCharsets.US_ASCII));
    }

    private static boolean isPdf(byte[] h) {
        return startsWith(h, "%PDF-".getBytes(StandardCharsets.US_ASCII));
    }

    private static boolean isWebp(byte[] h) {
        if (h.length < 12) return false;
        return h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
    }
}

