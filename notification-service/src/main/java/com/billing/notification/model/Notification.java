package com.billing.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private String from;
    private String to;
    private NotificationTemplate template;
    private Object body;
}
