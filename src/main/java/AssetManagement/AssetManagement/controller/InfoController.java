package AssetManagement.AssetManagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/info")
public class InfoController {

    @GetMapping
    public ResponseEntity<String> getMessage(){
        return ResponseEntity.ok("Welcome, to Mahavir Group Assets");
    }

}
