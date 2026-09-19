package com.luggagestorage.member.service;

import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.dto.ChangePasswordRequest;
import com.luggagestorage.member.dto.DeleteAccountRequest;
import com.luggagestorage.member.dto.SignupRequest;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.payment.repository.PaymentRepository;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.repository.StoreImageRepository;
import com.luggagestorage.store.repository.StoreRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreImageRepository storeImageRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(
            memberRepository, storeRepository, storeImageRepository, paymentRepository, passwordEncoder
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username, null)
        );
    }

    private Member memberWithId(Long id, String username) {
        Member member = Member.createUser(username, "encoded", "이름", null, null);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    @Test
    void signup_duplicateUsername_throws() {
        SignupRequest request = new SignupRequest("dup", "password123", "이름", null, null);
        when(memberRepository.existsByUsername("dup")).thenReturn(true);

        assertThatThrownBy(() -> memberService.signup(request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.DUPLICATE_USERNAME);

        verify(memberRepository, never()).save(any());
    }

    @Test
    void signup_duplicateEmail_throws() {
        SignupRequest request = new SignupRequest("newuser", "password123", "이름", "dup@test.com", null);
        when(memberRepository.existsByUsername("newuser")).thenReturn(false);
        when(memberRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.signup(request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void signup_success_encodesPasswordAndSaves() {
        SignupRequest request = new SignupRequest("newuser", "rawPassword", "이름", null, null);
        when(memberRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(memberRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        memberService.signup(request);

        verify(passwordEncoder).encode("rawPassword");
        verify(memberRepository).save(argThatPasswordIs("encodedPassword"));
    }

    private Member argThatPasswordIs(String expected) {
        return org.mockito.ArgumentMatchers.argThat(member -> expected.equals(member.getPassword()));
    }

    @Test
    void changePassword_wrongCurrentPassword_throws() {
        loginAs("user1");
        Member member = memberWithId(1L, "user1");
        when(memberRepository.findByUsername("user1")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong", member.getPassword())).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("wrong", "newPassword1");

        assertThatThrownBy(() -> memberService.changePassword(request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    @Test
    void changePassword_correctCurrentPassword_updatesPassword() {
        loginAs("user1");
        Member member = memberWithId(1L, "user1");
        when(memberRepository.findByUsername("user1")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("current", member.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPassword1")).thenReturn("newEncoded");

        memberService.changePassword(new ChangePasswordRequest("current", "newPassword1"));

        assertThat(member.getPassword()).isEqualTo("newEncoded");
    }

    @Test
    void deleteAccount_wrongPassword_throws() {
        loginAs("user1");
        Member member = memberWithId(1L, "user1");
        when(memberRepository.findByUsername("user1")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong", member.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> memberService.deleteAccount(new DeleteAccountRequest("wrong")))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_PASSWORD);

        verify(memberRepository, never()).delete(any());
    }

    @Test
    void deleteAccount_correctPassword_cascadesStoresPaymentsAndImages() {
        loginAs("user1");
        Member member = memberWithId(1L, "user1");
        when(memberRepository.findByUsername("user1")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("correct", member.getPassword())).thenReturn(true);

        Store store = new Store(member, "짐", null, "주소", com.luggagestorage.store.entity.StoreCategory.LIGHT,
            1, java.time.LocalDate.now(), java.time.LocalDate.now(), 3000);
        when(storeRepository.findByMemberOrderByCreatedAtDesc(member)).thenReturn(List.of(store));
        when(paymentRepository.findByStore(store)).thenReturn(Optional.empty());

        memberService.deleteAccount(new DeleteAccountRequest("correct"));

        verify(storeImageRepository).deleteByStore(store);
        verify(storeRepository).delete(store);
        verify(memberRepository).delete(member);
    }
}
