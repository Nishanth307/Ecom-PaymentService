package com.PaymentService.paymentservice.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundPaymentRequestDto {
    @NotBlank(message = "Payment ID is required")
    private String paymentId;

    @Min(value = 1, message = "Refund amount must be greater than 0")
    private Long amount; // Optional: if null, full refund
}

