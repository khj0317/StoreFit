package com.luggagestorage.member.service;

import com.luggagestorage.auth.security.SecurityUtil;
import com.luggagestorage.auth.service.RefreshTokenService;
import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.dto.ChangePasswordRequest;
import com.luggagestorage.member.dto.ChangePhoneRequest;
import com.luggagestorage.member.dto.DeleteAccountRequest;
import com.luggagestorage.member.dto.FindUsernameRequest;
import com.luggagestorage.member.dto.FindUsernameResponse;
import com.luggagestorage.member.dto.MemberProfileResponse;
import com.luggagestorage.member.dto.ResetPasswordRequest;
import com.luggagestorage.member.dto.SignupRequest;
import com.luggagestorage.member.dto.SignupResponse;
import com.luggagestorage.member.dto.UpdateProfileRequest;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.entity.MemberRole;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.payment.repository.PaymentRepository;
import com.luggagestorage.place.repository.StoragePlaceRepository;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreStatus;
import com.luggagestorage.store.repository.StoreImageRepository;
import com.luggagestorage.store.repository.StoreRepository;
import com.luggagestorage.verification.PhoneVerificationService;
import com.luggagestorage.verification.VerificationPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;
    private final StoreImageRepository storeImageRepository;
    private final PaymentRepository paymentRepository;
    private final StoragePlaceRepository storagePlaceRepository;
    private final PhoneVerificationService phoneVerificationService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (memberRepository.existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }

        MemberRole role = request.role() == null ? MemberRole.USER : request.role();
        if (!role.isSelfAssignable()) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }

        String phone = phoneVerificationService.consume(request.verificationToken(), VerificationPurpose.SIGNUP, request.phoneNumber());
        if (memberRepository.existsByPhoneNumber(phone)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
        }

        Member member = new Member(
            request.username(),
            passwordEncoder.encode(request.password()),
            request.name(),
            null,
            phone,
            role
        );

        return SignupResponse.from(memberRepository.save(member));
    }

    @Transactional
    public FindUsernameResponse findUsername(FindUsernameRequest request) {
        String phone = phoneVerificationService.consume(request.verificationToken(), VerificationPurpose.FIND_USERNAME, request.phoneNumber());
        Member member = memberRepository.findByPhoneNumber(phone)
            .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_NOT_REGISTERED));

        return new FindUsernameResponse(member.getUsername());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String phone = phoneVerificationService.consume(request.verificationToken(), VerificationPurpose.RESET_PASSWORD, request.phoneNumber());
        Member member = memberRepository.findByUsername(request.username())
            .filter(candidate -> phone.equals(candidate.getPhoneNumber()))
            .orElseThrow(() -> new BusinessException(ErrorCode.IDENTITY_NOT_VERIFIED));

        member.changePassword(passwordEncoder.encode(request.newPassword()));
        // 비밀번호를 잃어버렸다는 건 다른 기기의 로그인도 믿을 수 없다는 뜻이라 모두 끊는다
        refreshTokenService.revokeAll(member);
    }

    public MemberProfileResponse getMyProfile() {
        return MemberProfileResponse.from(getCurrentMember());
    }

    @Transactional
    public MemberProfileResponse updateProfile(UpdateProfileRequest request) {
        Member member = getCurrentMember();
        member.rename(request.name());
        return MemberProfileResponse.from(member);
    }

    @Transactional
    public MemberProfileResponse changePhone(ChangePhoneRequest request) {
        Member member = getCurrentMember();
        String phone = phoneVerificationService.consume(request.verificationToken(), VerificationPurpose.CHANGE_PHONE, request.phoneNumber());
        if (memberRepository.existsByPhoneNumber(phone)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
        }
        member.changePhone(phone);
        return MemberProfileResponse.from(member);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Member member = getCurrentMember();
        if (!passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        member.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void deleteAccount(DeleteAccountRequest request) {
        Member member = getCurrentMember();
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        // 다른 사람의 예약이 걸린 지점을 남겨둔 채 운영자만 사라지면 예약을 처리할 사람이 없어진다
        if (storagePlaceRepository.existsByOwner(member)) {
            throw new BusinessException(ErrorCode.OWNER_HAS_PLACES);
        }

        List<Store> stores = storeRepository.findByMemberOrderByCreatedAtDesc(member);
        // 결제했거나 맡겨둔 짐이 있는데 계정이 사라지면 환불도, 짐 찾기도 할 수 없다
        boolean hasActive = stores.stream().anyMatch(store -> store.getStatus() == StoreStatus.IN_USE
            || (store.getStatus() == StoreStatus.PENDING
                && paymentRepository.findByStore(store).map(payment -> payment.isPaid()).orElse(false)));
        if (hasActive) {
            throw new BusinessException(ErrorCode.ACTIVE_RESERVATION_EXISTS);
        }

        for (Store store : stores) {
            paymentRepository.findByStore(store).ifPresent(paymentRepository::delete);
            storeImageRepository.deleteByStore(store);
            storeRepository.delete(store);
        }
        memberRepository.delete(member);
    }

    private Member getCurrentMember() {
        String username = SecurityUtil.getCurrentUsername();
        return memberRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
