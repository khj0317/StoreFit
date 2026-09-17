package com.luggagestorage.store.repository;

import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreImageRepository extends JpaRepository<StoreImage, Long> {

    List<StoreImage> findByStoreOrderBySortOrderAsc(Store store);

    void deleteByStore(Store store);
}
