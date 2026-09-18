package com.luggagestorage.store.controller;

import com.luggagestorage.store.dto.StoreCreateRequest;
import com.luggagestorage.store.dto.StoreResponse;
import com.luggagestorage.store.dto.StoreUpdateRequest;
import com.luggagestorage.store.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @PostMapping
    public ResponseEntity<StoreResponse> createStore(@Valid @RequestBody StoreCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.createStore(request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<StoreResponse>> getMyStores() {
        return ResponseEntity.ok(storeService.getMyStores());
    }

    @GetMapping("/{storeId}")
    public ResponseEntity<StoreResponse> getStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(storeService.getStore(storeId));
    }

    @PutMapping("/{storeId}")
    public ResponseEntity<StoreResponse> updateStore(
        @PathVariable Long storeId,
        @Valid @RequestBody StoreUpdateRequest request
    ) {
        return ResponseEntity.ok(storeService.updateStore(storeId, request));
    }

    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> deleteStore(@PathVariable Long storeId) {
        storeService.deleteStore(storeId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{storeId}/complete")
    public ResponseEntity<StoreResponse> completeStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(storeService.completeStore(storeId));
    }

    @PatchMapping("/{storeId}/cancel")
    public ResponseEntity<StoreResponse> cancelStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(storeService.cancelStore(storeId));
    }
}
