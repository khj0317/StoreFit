package com.luggagestorage.verification;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    Optional<PhoneVerification> findFirstByPhoneNumberAndPurposeOrderByIdDesc(String phoneNumber, VerificationPurpose purpose);

    long countByPhoneNumberAndCreatedAtAfter(String phoneNumber, LocalDateTime after);

    long countByRequestIpAndCreatedAtAfter(String requestIp, LocalDateTime after);

    long countByCreatedAtGreaterThanEqual(LocalDateTime from);

    /** 토큰은 한 번만 쓸 수 있으므로, 동시에 두 요청이 같은 토큰을 쓰지 못하게 잠근다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from PhoneVerification v where v.token = :token")
    Optional<PhoneVerification> findByTokenForUpdate(@Param("token") String token);
}
