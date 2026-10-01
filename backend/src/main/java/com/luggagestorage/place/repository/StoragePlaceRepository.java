package com.luggagestorage.place.repository;

import com.luggagestorage.member.entity.Member;
import com.luggagestorage.place.entity.StoragePlace;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoragePlaceRepository extends JpaRepository<StoragePlace, Long> {

    /** 운영자가 있어 예약을 받을 수 있는 지점 */
    List<StoragePlace> findByOwnerIsNotNullOrderByIdAsc();

    /** 아직 운영자가 없어 새 운영자가 맡을 수 있는 정식 지점 */
    List<StoragePlace> findByOwnerIsNullOrderByIdAsc();

    Optional<StoragePlace> findByCode(String code);

    List<StoragePlace> findByOwnerOrderByIdAsc(Member owner);

    boolean existsByOwner(Member owner);

    /**
     * SELECT ... FOR UPDATE. 같은 보관소에 동시에 들어온 예약 요청이 "남은 자리 확인 → 저장"을
     * 한 번에 하나씩만 하도록 막는다. 락이 없으면 두 요청이 똑같이 "자리 있음"을 보고 둘 다 저장해서
     * 수용량을 넘길 수 있다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from StoragePlace p where p.id = :id")
    Optional<StoragePlace> findByIdForUpdate(@Param("id") Long id);
}
