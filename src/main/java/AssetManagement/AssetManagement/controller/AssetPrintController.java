package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags/assets")
@RequiredArgsConstructor
public class AssetPrintController {

    private final AssetRepository repo;

    // ---------------------- PRINT ALL ASSETS ----------------------
    @GetMapping("/print")
    public List<Map<String, String>> getAllForPrint() {
        return repo.findAll().stream()
                .map(asset -> Map.of(
                        "assetTag", asset.getAssetTag(),
                        "qrUrl", "/assets/" + asset.getAssetTag() + "/qr",
                        "barcodeUrl", "/assets/" + asset.getAssetTag() + "/barcode"
                ))
                .toList();
    }

    // ---------------------- PRINT SINGLE ASSET ----------------------
    @GetMapping("/print/{assetTag}")
    public Map<String, String> getOneForPrint(@PathVariable String assetTag) {

        var asset = repo.findByAssetTag(assetTag)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetTag));

        return Map.of(
                "assetTag", asset.getAssetTag(),
                "qrUrl", "/assets/" + asset.getAssetTag() + "/qr",
                "barcodeUrl", "/assets/" + asset.getAssetTag() + "/barcode"
        );
    }
}
