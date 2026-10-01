package com.luggagestorage.place.repository;

import com.luggagestorage.member.entity.Member;
import com.luggagestorage.place.entity.BranchApplication;
import com.luggagestorage.place.entity.StoragePlace;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BranchApplicationRepository extends JpaRepository<BranchApplication, Long> {

    @Query("select a from BranchApplication a join fetch a.place join fetch a.applicant where a.applicant = :applicant order by a.id desc")
    List<BranchApplication> findByApplicant(@Param("applicant") Member applicant);

    @Query("select a from BranchApplication a join fetch a.place join fetch a.applicant where a.status = :status order by a.id asc")
    List<BranchApplication> findByStatus(@Param("status") BranchApplication.Status status);

    @Query("select a from BranchApplication a join fetch a.place join fetch a.applicant order by a.id desc")
    List<BranchApplication> findAllWithDetails();

    boolean existsByPlaceAndApplicantAndStatus(StoragePlace place, Member applicant, BranchApplication.Status status);

    List<BranchApplication> findByPlaceAndStatus(StoragePlace place, BranchApplication.Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BranchApplication a join fetch a.place join fetch a.applicant where a.id = :id")
    Optional<BranchApplication> findByIdForUpdate(@Param("id") Long id);
}
