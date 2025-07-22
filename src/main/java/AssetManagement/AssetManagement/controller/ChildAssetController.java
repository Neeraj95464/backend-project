package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.dto.ChildAssetDTO;
import AssetManagement.AssetManagement.service.ChildAssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/child-assets")
public class ChildAssetController {

    @Autowired
    private ChildAssetService childAssetService;

    // Fetch small child assets by assetTag
//    @GetMapping("/{assetTag}")
//    public ResponseEntity<List<ChildAssetDTO>> getSmallChildAssets(@PathVariable String assetTag) {
//        try {
//            List<ChildAssetDTO> smallChildAssets = childAssetService
//                    .getSmallChildAssetsByAssetTag(assetTag)
//                    .stream()
//                    .map(childAsset -> new ChildAssetDTO(
//                            childAsset.getName(),
//                            childAsset.getWarranty(),
//                            childAsset.getPurchaseFrom(),
//                            childAsset.getChildAssetNote()))
//                    .toList();
//
//            return ResponseEntity.ok(smallChildAssets);
//        } catch (Exception e) {
//            return ResponseEntity.status(404).body(null);
//        }
//    }

    @GetMapping("/{assetTag}")
    public ResponseEntity<List<ChildAssetDTO>> getSmallChildAssets(@PathVariable String assetTag) {
        try {
            List<ChildAssetDTO> smallChildAssets = childAssetService
                    .getSmallChildAssetsByAssetTag(assetTag)
                    .stream()
                    .map(childAsset -> new ChildAssetDTO(
                            childAsset.getName(),
                            childAsset.getWarranty(),
                            childAsset.getPurchaseFrom(),
                            childAsset.getChildAssetNote())) // Make sure this returns correct value
                    .toList();

            return ResponseEntity.ok(smallChildAssets);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(null);
        }
    }


    // Create a small child asset for a given assetTag
    @PostMapping("/create/{assetTag}")
    public ResponseEntity<ChildAssetDTO> createSmallChildAsset(
            @PathVariable String assetTag,
            @RequestBody ChildAssetDTO childAssetDTO) {
        try {
            ChildAssetDTO created = childAssetService.createSmallChildAsset(assetTag, childAssetDTO);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(null);
        }
    }
}
