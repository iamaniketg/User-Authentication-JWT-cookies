package com.carboncell.User.Authentication.controller;

import com.carboncell.User.Authentication.model.dto.ApiEntry;
import com.carboncell.User.Authentication.model.dto.ApiEntryList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/test")
public class TestController {
    @Value("${carbonCell.app.baseUrl}")
    private String publicApiUrl;

    @Autowired
    private RestTemplate restTemplate;
    @GetMapping("/public-apis")
    public ResponseEntity<List<ApiEntry>> getPublicApis() {
        try {
            ResponseEntity<ApiEntryList> response = restTemplate.getForEntity(publicApiUrl, ApiEntryList.class);
            List<ApiEntry> apiEntries = response.getBody().getEntries();
            return ResponseEntity.ok(apiEntries);
        } catch (HttpClientErrorException ex) {
            // Handle client errors (4xx)
            HttpStatusCode statusCode = ex.getStatusCode();
            return ResponseEntity.status(statusCode).body(Collections.emptyList());
        } catch (Exception ex) {
            // Handle other exceptions
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.emptyList());
        }
    }
    @GetMapping("/public-apis/filters")
    public ResponseEntity<List<ApiEntry>> getPublicApis(@RequestParam(required = false) String category) {
        try {
            ResponseEntity<ApiEntryList> response = restTemplate.getForEntity(publicApiUrl, ApiEntryList.class);
            List<ApiEntry> apiEntries = response.getBody().getEntries(); // Use entries directly

            if (category != null && !category.isEmpty()) {
                // Filter the list based on the category
                apiEntries = apiEntries.stream()
                        .filter(entry -> category.equalsIgnoreCase(entry.getCategory()))
                        .collect(Collectors.toList());
            }

            return ResponseEntity.ok(apiEntries);
        } catch (HttpClientErrorException ex) {
            // Handle client errors (4xx)
            HttpStatusCode statusCode = ex.getStatusCode();
            return ResponseEntity.status(statusCode).body(Collections.emptyList());
        } catch (Exception ex) {
            // Handle other exceptions
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.emptyList());
        }
    }
    @GetMapping("/public-apis/limit")
    public ResponseEntity<List<ApiEntry>> getPublicApis(
            @RequestParam(required = false, defaultValue = "10") int limit) {
        try {
            ResponseEntity<ApiEntryList> response = restTemplate.getForEntity(publicApiUrl, ApiEntryList.class);
            List<ApiEntry> apiEntries = response.getBody().getEntries();

            // Limit the number of entries if limit is specified
            if (limit > 0 && limit < apiEntries.size()) {
                apiEntries = apiEntries.subList(0, limit);
            }

            return ResponseEntity.ok(apiEntries);
        } catch (HttpClientErrorException ex) {
            // Handle client errors (4xx)
            HttpStatusCode statusCode = ex.getStatusCode();
            return ResponseEntity.status(statusCode).body(Collections.emptyList());
        } catch (Exception ex) {
            // Handle other exceptions
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.emptyList());
        }
    }
    @GetMapping("/all")
    public String allAccess() {
        return "Public Content.";
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public String userAccess() {
        return "User Content.";
    }

    @GetMapping("/mod")
    @PreAuthorize("hasRole('MODERATOR')")
    public String moderatorAccess() {
        return "Moderator Board.";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        return "Admin Board.";
    }
}