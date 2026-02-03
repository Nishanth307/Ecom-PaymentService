package com.PaymentService.paymentservice.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitiatePaymentResponseDto {
    private String orderId;
    private String paymentLinkId;
    private String paymentLinkUrl;
    private String message;
}
