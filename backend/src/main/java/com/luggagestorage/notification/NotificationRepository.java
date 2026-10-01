package com.luggagestorage.notification;

import com.luggagestorage.member.entity.Member;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByOrderByIdDesc(Pageable pageable);

    List<Notification> findByMemberOrderByIdDesc(Member member, Pageable pageable);
}
