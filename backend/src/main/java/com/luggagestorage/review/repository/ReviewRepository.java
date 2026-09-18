package com.luggagestorage.review.repository;

import com.luggagestorage.review.entity.Review;
import com.luggagestorage.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByStore(Store store);
}
