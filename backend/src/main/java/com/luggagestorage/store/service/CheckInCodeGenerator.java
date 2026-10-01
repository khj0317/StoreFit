package com.luggagestorage.store.service;

import com.luggagestorage.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * QR에 담을 체크인 코드. 카메라가 안 될 때 운영자가 직접 입력할 수도 있게 짧게 만들고,
 * 헷갈리는 글자(0/O, 1/I/L)는 뺐다. 32^8 ≈ 1조 가지라 추측하기 어렵고,
 * 코드를 알아도 그 보관소의 운영자만 처리할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class CheckInCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();
    private final StoreRepository storeRepository;

    public String generate() {
        String code;
        do {
            code = randomCode();
        } while (storeRepository.existsByCheckInCode(code));
        return code;
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
