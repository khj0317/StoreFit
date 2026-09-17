package com.luggagestorage.reservation.repository;

import com.luggagestorage.member.entity.Member;
import com.luggagestorage.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByMemberOrderByCreatedAtDesc(Member member);

    List<Reservation> findByStore_HostOrderByCreatedAtDesc(Member host);
}
