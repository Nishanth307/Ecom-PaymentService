package com.PaymentService.paymentservice.dtos;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyPaymentRequestDto {
    private String paymentId;
    private String orderId;

    @AssertTrue(message = "Either paymentId or orderId must be provided")
    private boolean isValid() {
        return (paymentId != null && !paymentId.isEmpty()) || 
               (orderId != null && !orderId.isEmpty());
    }
}

