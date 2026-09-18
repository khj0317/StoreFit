package com.luggagestorage.store.repository;

import com.luggagestorage.member.entity.Member;
import com.luggagestorage.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByMemberOrderByCreatedAtDesc(Member member);
}
