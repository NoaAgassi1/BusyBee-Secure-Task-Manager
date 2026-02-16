package com.securefromscratch.busybee.controllers; 

import org.owasp.untrust.boxedpath.BoxedPath;
import org.owasp.untrust.boxedpath.PathSandbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.securefromscratch.busybee.safety.ImageName; 

@RestController
public class ImageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageController.class);
    
    private static final PathSandbox UPLOADS;
    static {
        Path uploadsRoot = Path.of("uploads").toAbsolutePath().normalize();
        UPLOADS = PathSandbox.boxroot(uploadsRoot.toString());
    }

    @GetMapping("/image")
    @PreAuthorize("@taskAuthorization.imgIsInOwnedOrAssignedTask(#p0, authentication)")
    public ResponseEntity<byte[]> getImage(
            @RequestParam("img") ImageName img, 
            Authentication authentication) throws IOException {

        LOGGER.debug("IMAGE: authName={}, roles={}, img={}",
                authentication.getName(),
                authentication.getAuthorities(),
                img.getName());

        String relativePath = Path.of("uploads", img.getName()).toString();
        BoxedPath boxed = UPLOADS.of(relativePath);

        LOGGER.debug("getImage requested img='{}'", img.getName());

        if (!Files.exists(boxed) || !Files.isRegularFile(boxed) || !Files.isReadable(boxed)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        byte[] bytes = Files.readAllBytes(boxed);
        String ct = Files.probeContentType(boxed);
        MediaType mediaType = (ct != null) ? MediaType.parseMediaType(ct) : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mediaType.toString())
                .body(bytes);
    }
}