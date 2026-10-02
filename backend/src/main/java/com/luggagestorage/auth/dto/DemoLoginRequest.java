package com.luggagestorage.auth.dto;

import com.luggagestorage.member.entity.MemberRole;
import jakarta.validation.constraints.NotNull;

/** 로그인 화면의 "둘러보기": 이용자(USER) 또는 사장님(OWNER) 체험 계정으로 들어간다 */
public record DemoLoginRequest(@NotNull MemberRole role) {
}
