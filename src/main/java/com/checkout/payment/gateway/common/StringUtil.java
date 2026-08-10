package com.checkout.payment.gateway.common;

public class StringUtil {

  public static String getLastFourDigits(String number) {
    return number.substring(number.length() - 4);
  }
}
