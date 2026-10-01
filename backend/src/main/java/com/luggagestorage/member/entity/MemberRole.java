package com.luggagestorage.member.entity;

public enum MemberRole {
    /** 짐을 맡기는 이용자 */
    USER,
    /** 보관소를 운영하며 체크인·체크아웃을 처리하는 사장님 */
    OWNER,
    ADMIN;

    /** 회원가입으로 직접 고를 수 있는 역할인지 (ADMIN은 가입으로 만들 수 없다) */
    public boolean isSelfAssignable() {
        return this == USER || this == OWNER;
    }
}
