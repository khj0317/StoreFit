package com.luggagestorage.reservation.controller;

import com.luggagestorage.reservation.dto.ReservationCreateRequest;
import com.luggagestorage.reservation.dto.ReservationResponse;
import com.luggagestorage.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody ReservationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createReservation(request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponse>> getMyReservations() {
        return ResponseEntity.ok(reservationService.getMyReservations());
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long reservationId) {
        return ResponseEntity.ok(reservationService.getReservation(reservationId));
    }

    @PatchMapping("/{reservationId}/confirm")
    public ResponseEntity<ReservationResponse> confirmReservation(@PathVariable Long reservationId) {
        return ResponseEntity.ok(reservationService.confirmReservation(reservationId));
    }

    @PatchMapping("/{reservationId}/start")
    public ResponseEntity<ReservationResponse> startReservation(@PathVariable Long reservationId) {
        return ResponseEntity.ok(reservationService.startReservation(reservationId));
    }

    @PatchMapping("/{reservationId}/complete")
    public ResponseEntity<ReservationResponse> completeReservation(@PathVariable Long reservationId) {
        return ResponseEntity.ok(reservationService.completeReservation(reservationId));
    }

    @PatchMapping("/{reservationId}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable Long reservationId) {
        return ResponseEntity.ok(reservationService.cancelReservation(reservationId));
    }
}
