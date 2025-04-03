package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.dto.DocumentResponse;
import AssetManagement.AssetManagement.entity.Asset;
import AssetManagement.AssetManagement.entity.AssetDocument;
import AssetManagement.AssetManagement.repository.AssetDocumentRepository;
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
@RequestMapping("/api/asset-documents")
public class AssetDocumentController {

    private static final String UPLOAD_DIR = "document_uploads/";

    @Autowired
    private AssetDocumentRepository assetDocumentRepository;

    @Autowired
    private AssetRepository assetRepository;

    /**
     * Upload one or multiple documents for a specific asset
     */
    @PostMapping("/{assetTag}/upload-documents")
    public ResponseEntity<String> uploadDocuments(@PathVariable String assetTag, @RequestParam("files") MultipartFile[] files) {
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

                    AssetDocument document = new AssetDocument();
                    document.setDocumentUrl(fileName);
                    document.setAsset(asset);
                    document.setAddedAt(LocalDateTime.now());
                    document.setAddedBy(AuthUtils.getAuthenticatedUsername());
                    assetDocumentRepository.save(document);

                    uploadedFiles.append(fileName).append("\n");
                }
            }
            return ResponseEntity.ok("Documents uploaded successfully:\n" + uploadedFiles);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to save files: " + e.getMessage());
        }
    }

    /**
     * Fetch all document filenames for a given asset
     */
    @GetMapping("/{assetTag}/documents")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByAssetTag(@PathVariable String assetTag) {
        Asset asset = assetRepository.findByAssetTag(assetTag)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        List<DocumentResponse> documentList = assetDocumentRepository.findByAsset(asset).stream()
                .map(doc -> new DocumentResponse(doc.getDocumentUrl(), doc.getAddedBy(), doc.getAddedAt()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(documentList);
    }

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Determine the content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream"; // Fallback type
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType)) // ✅ Set proper MIME type
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"") // ✅ Force download
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.status(500).body(null);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(null);
        }
    }

//    @GetMapping("/all-documents")
//    public ResponseEntity<List<String>> getAllDocuments() {
//        List<String> allDocumentFilenames = assetDocumentRepository.findAll().stream()
//                .map(AssetDocument::getDocumentUrl)
//                .collect(Collectors.toList());
//
//        return ResponseEntity.ok(allDocumentFilenames);
//    }
}
