package com.billing.notification.advice.exceptions;

public class InternalNotificationErrorException extends RuntimeException {
  public InternalNotificationErrorException(String message) {
    super(message);
  }
}
