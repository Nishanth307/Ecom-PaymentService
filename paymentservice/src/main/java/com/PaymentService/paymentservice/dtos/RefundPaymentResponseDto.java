package com.PaymentService.paymentservice.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundPaymentResponseDto {
    private String refundId;
    private String paymentId;
    private Long amount;
    private String status;
    private String message;
}

