//package AssetManagement.AssetManagement.controller;
//
//import AssetManagement.AssetManagement.repository.AssetRepository;
//import AssetManagement.AssetManagement.util.AssetCodeUtil;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/assets")
//@RequiredArgsConstructor
//public class AssetCodeController {
//
//    private final AssetRepository repo;
//
//    @GetMapping("/{assetTag}/qr")
//    public ResponseEntity<byte[]> getQr(@PathVariable String assetTag) throws Exception {
//        System.out.println("QR requested for assetTag: " + assetTag);
//
//        var asset = repo.findByAssetTag(assetTag)
//                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetTag));
//
//        // Example: you can put asset details inside QR OR URL
//        String qrData = "ASSET:" + asset.getAssetTag();
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_TYPE, "image/png")
//                .body(AssetCodeUtil.generateQrPng(qrData, 200));
//    }
//
//    @GetMapping("/{assetTag}/barcode")
//    public ResponseEntity<byte[]> getBarcode(@PathVariable String assetTag) throws Exception {
//        System.out.println("Barcode requested for assetTag: " + assetTag);
//
//        var asset = repo.findByAssetTag(assetTag)
//                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetTag));
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_TYPE, "image/png")
//                .body(AssetCodeUtil.generateBarcodePng(asset.getAssetTag(), 400, 120));
//    }
//
//}


package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.repository.AssetRepository;
import AssetManagement.AssetManagement.util.AssetCodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetCodeController {

    private final AssetRepository repo;

    @GetMapping(value = "/{assetTag}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQr(@PathVariable String assetTag) throws Exception {
        System.out.println("QR requested for assetTag: " + assetTag);

        var asset = repo.findByAssetTag(assetTag)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetTag));

        // You can also put full URL instead of ASSET:...
        String qrData = "ASSET:" + asset.getAssetTag();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .body(AssetCodeUtil.generateQrPng(qrData, 200));
    }

    @GetMapping(value = "/{assetTag}/barcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getBarcode(@PathVariable String assetTag) throws Exception {
        System.out.println("Barcode requested for assetTag: " + assetTag);

        var asset = repo.findByAssetTag(assetTag)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetTag));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .body(AssetCodeUtil.generateBarcodePng(asset.getAssetTag(), 400, 120));
    }
}
