package com.luggagestorage.notification;

import java.util.Map;

/**
 * 보내는 알림 종류와 문구. 문구 안의 #{이름}은 카카오 알림톡 템플릿 변수와 같은 형식이라,
 * 알림톡 템플릿을 등록할 때 이 문구를 그대로 쓰면 된다 (문자로 보낼 때는 값을 채워 넣는다).
 */
public enum NotificationType {

    VERIFICATION_CODE("[스토어핏] 인증번호 [#{code}]를 입력해주세요. (3분 안에 입력)"),
    PAYMENT_DONE("[스토어핏] #{place} 예약이 확정됐어요.\n#{period} · 짐 #{count}개\n맡기는 날 앱에서 체크인 QR을 보여주세요."),
    NEW_RESERVATION("[스토어핏] #{place}에 새 예약이 들어왔어요.\n#{customer}님 · 짐 #{count}개 · #{start} 입고 예정"),
    CHECKED_IN("[스토어핏] #{place}에서 짐 #{count}개를 받았어요. #{end}까지 안전하게 보관할게요."),
    CHECKED_OUT("[스토어핏] 짐을 돌려받으셨어요. 이용해주셔서 감사합니다!"),
    PICKUP_REMINDER("[스토어핏] 내일(#{end})은 #{place} 보관 마지막 날이에요. 잊지 말고 찾아가세요."),
    OVERDUE("[스토어핏] #{place} 보관 기간이 #{days}일 지났어요. 연체료(하루 #{dailyFee}원)가 쌓이고 있으니 빨리 찾아가주세요."),
    REFUNDED("[스토어핏] #{place} 예약이 취소됐어요. #{amount}원이 환불됩니다."),
    PAYMENT_EXPIRED("[스토어핏] 30분 안에 결제하지 않아 #{place} 예약이 자동 취소됐어요."),
    NO_SHOW("[스토어핏] #{start}에 체크인하지 않아 #{place} 예약이 노쇼 처리됐어요. (환불 불가)"),
    BRANCH_APPROVED("[스토어핏] #{place} 운영 신청이 승인됐어요. 이제 예약을 받을 수 있어요."),
    BRANCH_REJECTED("[스토어핏] #{place} 운영 신청이 반려됐어요. 사유: #{reason}");

    private final String template;

    NotificationType(String template) {
        this.template = template;
    }

    public String getTemplate() {
        return template;
    }

    /** 발송 기록에 남길 문구. 인증번호는 DB에 남기지 않는다 (인증 테이블에도 해시만 저장) */
    public String renderForLog(Map<String, String> variables) {
        if (this != VERIFICATION_CODE) {
            return render(variables);
        }
        Map<String, String> masked = new java.util.HashMap<>(variables);
        masked.put("code", "******");
        return render(masked);
    }

    public String render(Map<String, String> variables) {
        String text = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            text = text.replace("#{" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }
}
