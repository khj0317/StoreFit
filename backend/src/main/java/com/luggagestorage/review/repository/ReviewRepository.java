package com.luggagestorage.review.repository;

import com.luggagestorage.reservation.entity.Reservation;
import com.luggagestorage.review.entity.Review;
import com.luggagestorage.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByStoreOrderByCreatedAtDesc(Store store);

    boolean existsByReservation(Reservation reservation);
}
