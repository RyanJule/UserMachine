package org.machinesystems.UserMachine.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    
    @GetMapping("/some-endpoint")
    public ResponseEntity<String> someEndpoint() {
        return ResponseEntity.ok("Success");
    }
}
