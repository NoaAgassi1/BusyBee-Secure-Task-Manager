package com.securefromscratch.busybee.controllers;

import com.securefromscratch.busybee.safety.ImageName;
import org.owasp.untrust.boxedpath.BoxedPath;
import org.owasp.untrust.boxedpath.PathSandbox;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class AttachmentController {

    private static final PathSandbox UPLOADS;
    static {
        Path uploadsRoot = Path.of("uploads").toAbsolutePath().normalize();
        UPLOADS = PathSandbox.boxroot(uploadsRoot.toString());
    }

    @GetMapping("/attachment")
    @PreAuthorize("@taskAuthorization.attachmentIsInOwnedOrAssignedTask(#p0, authentication)")
    public ResponseEntity<byte[]> getAttachment(@RequestParam("file") ImageName file) throws IOException {

        String relativePath = Path.of("uploads", file.getName()).toString();
        BoxedPath boxed = UPLOADS.of(relativePath);

        if (!Files.exists(boxed) || !Files.isRegularFile(boxed) || !Files.isReadable(boxed)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        byte[] bytes = Files.readAllBytes(boxed);

        String ct = Files.probeContentType(boxed);
        if (ct == null) ct = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct))
       
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getName().replace("\"", "") + "\"")
                .body(bytes);
    }
}
