package com.luggagestorage.store.service;

import com.luggagestorage.notification.NotificationService;
import com.luggagestorage.notification.NotificationType;
import com.luggagestorage.store.entity.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** 예약 관련 문자·알림톡을 보낸다. 문구에 들어갈 값(지점, 기간, 개수 등)을 한곳에서 만든다 */
@Component
@RequiredArgsConstructor
public class ReservationNotifier {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d");

    private final NotificationService notificationService;

    public void paymentDone(Store store) {
        notificationService.notify(store.getMember(), NotificationType.PAYMENT_DONE, variables(store));
        // 지점 운영자에게도 새 예약을 알린다
        Map<String, String> ownerVariables = variables(store);
        ownerVariables.put("customer", store.getMember().getName());
        notificationService.notify(store.getPlace().getOwner(), NotificationType.NEW_RESERVATION, ownerVariables);
    }

    public void checkedIn(Store store) {
        notificationService.notify(store.getMember(), NotificationType.CHECKED_IN, variables(store));
    }

    public void checkedOut(Store store) {
        notificationService.notify(store.getMember(), NotificationType.CHECKED_OUT, variables(store));
    }

    public void refunded(Store store, int amount) {
        Map<String, String> variables = variables(store);
        variables.put("amount", won(amount));
        notificationService.notify(store.getMember(), NotificationType.REFUNDED, variables);
    }

    public void paymentExpired(Store store) {
        notificationService.notify(store.getMember(), NotificationType.PAYMENT_EXPIRED, variables(store));
    }

    public void noShow(Store store) {
        notificationService.notify(store.getMember(), NotificationType.NO_SHOW, variables(store));
    }

    public void pickupReminder(Store store) {
        notificationService.notify(store.getMember(), NotificationType.PICKUP_REMINDER, variables(store));
    }

    public void overdue(Store store, long days) {
        Map<String, String> variables = variables(store);
        variables.put("days", String.valueOf(days));
        variables.put("dailyFee", won(OverduePolicy.dailyFee(store)));
        notificationService.notify(store.getMember(), NotificationType.OVERDUE, variables);
    }

    private Map<String, String> variables(Store store) {
        Map<String, String> variables = new HashMap<>();
        variables.put("place", store.getPlace().getName());
        variables.put("start", store.getStartDate().format(DATE));
        variables.put("end", store.getEndDate().format(DATE));
        variables.put("period", store.getStartDate().format(DATE) + "~" + store.getEndDate().format(DATE));
        variables.put("count", String.valueOf(store.getLuggageCount()));
        return variables;
    }

    private static String won(long amount) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(amount);
    }
}
