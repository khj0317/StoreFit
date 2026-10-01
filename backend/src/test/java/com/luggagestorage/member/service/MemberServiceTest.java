package com.luggagestorage.member.service;

import com.luggagestorage.auth.service.RefreshTokenService;
import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.dto.ChangePasswordRequest;
import com.luggagestorage.member.dto.DeleteAccountRequest;
import com.luggagestorage.member.dto.ResetPasswordRequest;
import com.luggagestorage.member.dto.SignupRequest;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.entity.MemberRole;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.payment.entity.Payment;
import com.luggagestorage.payment.repository.PaymentRepository;
import com.luggagestorage.place.entity.StoragePlace;
import com.luggagestorage.place.repository.StoragePlaceRepository;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreCategory;
import com.luggagestorage.store.repository.StoreImageRepository;
import com.luggagestorage.store.repository.StoreRepository;
import com.luggagestorage.verification.PhoneVerificationService;
import com.luggagestorage.verification.VerificationPurpose;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    private static final String PHONE = "01012345678";

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreImageRepository storeImageRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private StoragePlaceRepository storagePlaceRepository;
    @Mock
    private PhoneVerificationService phoneVerificationService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepository, storeRepository, storeImageRepository, paymentRepository,
            storagePlaceRepository, phoneVerificationService, refreshTokenService, passwordEncoder);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Member loginAs(Long id, String username) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, null));
        Member member = Member.createUser(username, "encoded", "이름", null, PHONE);
        ReflectionTestUtils.setField(member, "id", id);
        when(memberRepository.findByUsername(username)).thenReturn(Optional.of(member));
        return member;
    }

    private SignupRequest signup(String username, MemberRole role) {
        return new SignupRequest(username, "password123", "이름", "010-1234-5678", "token", role);
    }

    // ───────── 가입 ─────────

    @Test
    void signup_duplicateUsername_throws() {
        when(memberRepository.existsByUsername("dup")).thenReturn(true);

        assertThatThrownBy(() -> memberService.signup(signup("dup", null)))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.DUPLICATE_USERNAME);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void signup_requiresVerifiedPhoneToken() {
        when(memberRepository.existsByUsername("newuser")).thenReturn(false);
        when(phoneVerificationService.consume("token", VerificationPurpose.SIGNUP, "010-1234-5678"))
            .thenThrow(new BusinessException(ErrorCode.PHONE_NOT_VERIFIED));

        assertThatThrownBy(() -> memberService.signup(signup("newuser", null)))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.PHONE_NOT_VERIFIED);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void signup_success_savesVerifiedPhoneAndEncodedPassword() {
        when(memberRepository.existsByUsername("newuser")).thenReturn(false);
        when(phoneVerificationService.consume("token", VerificationPurpose.SIGNUP, "010-1234-5678")).thenReturn(PHONE);
        when(memberRepository.existsByPhoneNumber(PHONE)).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(memberRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        memberService.signup(signup("newuser", null));

        verify(memberRepository).save(argThat(member -> "encodedPassword".equals(member.getPassword())
            && PHONE.equals(member.getPhoneNumber())
            && member.getEmail() == null
            && member.getRole() == MemberRole.USER));
    }

    @Test
    void signup_asOwner_savesOwnerRole() {
        when(memberRepository.existsByUsername("owner1")).thenReturn(false);
        when(phoneVerificationService.consume(any(), any(), any())).thenReturn(PHONE);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(memberRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        memberService.signup(signup("owner1", MemberRole.OWNER));

        verify(memberRepository).save(argThat(member -> member.getRole() == MemberRole.OWNER));
    }

    @Test
    void signup_asAdmin_isRejected() {
        when(memberRepository.existsByUsername("sneaky")).thenReturn(false);

        assertThatThrownBy(() -> memberService.signup(signup("sneaky", MemberRole.ADMIN)))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_ROLE);
        verify(memberRepository, never()).save(any());
    }

    // ───────── 비밀번호 ─────────

    @Test
    void resetPassword_phoneOfDifferentAccount_isRejected() {
        Member member = Member.createUser("user1", "encoded", "이름", null, "01099998888");
        when(phoneVerificationService.consume("token", VerificationPurpose.RESET_PASSWORD, PHONE)).thenReturn(PHONE);
        when(memberRepository.findByUsername("user1")).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.resetPassword(new ResetPasswordRequest("user1", PHONE, "token", "newPassword1")))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.IDENTITY_NOT_VERIFIED);
    }

    @Test
    void resetPassword_success_logsOutEverywhere() {
        Member member = Member.createUser("user1", "encoded", "이름", null, PHONE);
        when(phoneVerificationService.consume("token", VerificationPurpose.RESET_PASSWORD, PHONE)).thenReturn(PHONE);
        when(memberRepository.findByUsername("user1")).thenReturn(Optional.of(member));
        when(passwordEncoder.encode("newPassword1")).thenReturn("newEncoded");

        memberService.resetPassword(new ResetPasswordRequest("user1", PHONE, "token", "newPassword1"));

        verify(refreshTokenService).revokeAll(member);
    }

    @Test
    void changePassword_wrongCurrentPassword_throws() {
        Member member = loginAs(1L, "user1");
        when(passwordEncoder.matches("wrong", member.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> memberService.changePassword(new ChangePasswordRequest("wrong", "newPassword1")))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    // ───────── 탈퇴 ─────────

    @Test
    void deleteAccount_wrongPassword_throws() {
        Member member = loginAs(1L, "user1");
        when(passwordEncoder.matches("wrong", member.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> memberService.deleteAccount(new DeleteAccountRequest("wrong")))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_PASSWORD);
        verify(memberRepository, never()).delete(any());
    }

    @Test
    void deleteAccount_withUnpaidReservations_cascades() {
        Member member = loginAs(1L, "user1");
        when(passwordEncoder.matches("correct", member.getPassword())).thenReturn(true);

        StoragePlace place = new StoragePlace(Member.createOwner("boss", "encoded", "사장님", null, null), "지점", "주소", null, 10);
        Store store = new Store(member, place, "짐", null, StoreCategory.LIGHT, 1, LocalDate.now(), LocalDate.now(), 3000);
        when(storeRepository.findByMemberOrderByCreatedAtDesc(member)).thenReturn(List.of(store));
        when(paymentRepository.findByStore(store)).thenReturn(Optional.empty());

        memberService.deleteAccount(new DeleteAccountRequest("correct"));

        verify(storeImageRepository).deleteByStore(store);
        verify(storeRepository).delete(store);
        verify(memberRepository).delete(member);
    }

    @Test
    void deleteAccount_withPaidReservation_isBlocked() {
        Member member = loginAs(1L, "user1");
        when(passwordEncoder.matches("correct", member.getPassword())).thenReturn(true);

        StoragePlace place = new StoragePlace(Member.createOwner("boss", "encoded", "사장님", null, null), "지점", "주소", null, 10);
        Store store = new Store(member, place, "짐", null, StoreCategory.LIGHT, 1, LocalDate.now(), LocalDate.now(), 3000);
        Payment payment = new Payment(store, "order-1", 3000);
        payment.approve("key", "카드", LocalDateTime.now());
        when(storeRepository.findByMemberOrderByCreatedAtDesc(member)).thenReturn(List.of(store));
        when(paymentRepository.findByStore(store)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> memberService.deleteAccount(new DeleteAccountRequest("correct")))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ACTIVE_RESERVATION_EXISTS);
        verify(memberRepository, never()).delete(any());
    }

    @Test
    void deleteAccount_ownerWithPlaces_isBlocked() {
        Member owner = loginAs(5L, "owner1");
        when(passwordEncoder.matches("correct", owner.getPassword())).thenReturn(true);
        when(storagePlaceRepository.existsByOwner(owner)).thenReturn(true);

        assertThatThrownBy(() -> memberService.deleteAccount(new DeleteAccountRequest("correct")))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.OWNER_HAS_PLACES);
        verify(memberRepository, never()).delete(any());
    }
}
