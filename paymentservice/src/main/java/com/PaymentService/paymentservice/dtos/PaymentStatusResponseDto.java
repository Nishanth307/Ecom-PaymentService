package com.PaymentService.paymentservice.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentStatusResponseDto {
    private String paymentId;
    private String orderId;
    private String status;
    private Long amount;
    private String currency;
    private String method;
}

