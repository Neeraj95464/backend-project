package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.entity.Asset;
import AssetManagement.AssetManagement.entity.AssetPhoto;
import AssetManagement.AssetManagement.repository.AssetPhotoRepository;
import AssetManagement.AssetManagement.repository.AssetRepository;

import AssetManagement.AssetManagement.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/asset-photos")
public class AssetPhotoController {

    private static final String UPLOAD_DIR = "uploads/";

    @Autowired
    private AssetPhotoRepository assetPhotoRepository;

    @Autowired
    private AssetRepository assetRepository;

    /**
     * Upload one or multiple photos for a specific asset
     */
    @PostMapping("/{assetTag}/upload-photos")
    public ResponseEntity<String> uploadPhotos(@PathVariable String assetTag, @RequestParam("files") MultipartFile[] files) {
        Asset asset = assetRepository.findByAssetTag(assetTag)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        try {
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            StringBuilder uploadedFiles = new StringBuilder();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileName = assetTag + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
                    Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName);
                    Files.write(filePath, file.getBytes());

                    AssetPhoto photo = new AssetPhoto();
                    photo.setPhotoUrl(fileName);
                    photo.setAsset(asset);
                    photo.setAddedAt(LocalDateTime.now());
                    photo.setAddedBy(AuthUtils.getAuthenticatedUsername());
                    assetPhotoRepository.save(photo);

                    uploadedFiles.append(fileName).append("\n");
                }
            }
            return ResponseEntity.ok("Photos uploaded successfully:\n" + uploadedFiles);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to save files: " + e.getMessage());
        }
    }

    /**
     * Fetch all photo filenames for a given asset
     */
    @GetMapping("/{assetTag}/photos")
    public ResponseEntity<List<String>> getPhotosByAssetTag(@PathVariable String assetTag) {
        Asset asset = assetRepository.findByAssetTag(assetTag)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        List<String> photoFilenames = assetPhotoRepository.findByAsset(asset).stream()
                .map(AssetPhoto::getPhotoUrl)
                .collect(Collectors.toList());

        return ResponseEntity.ok(photoFilenames);
    }

    /**
     * Serve image files dynamically
     */
    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG) // Adjust for different image types if needed
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/all-photos")
    public ResponseEntity<List<String>> getAllPhotos() {
        List<String> allPhotoFilenames = assetPhotoRepository.findAll().stream()
                .map(AssetPhoto::getPhotoUrl)
                .collect(Collectors.toList());

        return ResponseEntity.ok(allPhotoFilenames);
    }

}
