package com.mysawit.payroll.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@FeignClient(name = "identity-service", url = "http://localhost:8082", path = "/api/user")
public interface IdentityClient {
    @GetMapping("/{id}")
    Map<String, Object> getUserById(@PathVariable("id") Long id, @RequestHeader("Authorization") String token);
}
