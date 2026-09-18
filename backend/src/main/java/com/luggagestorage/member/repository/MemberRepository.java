package com.luggagestorage.member.repository;

import com.luggagestorage.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);
}
