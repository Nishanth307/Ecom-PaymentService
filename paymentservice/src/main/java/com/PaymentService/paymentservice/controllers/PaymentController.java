package com.PaymentService.paymentservice.controllers;

import com.PaymentService.paymentservice.constants.PaymentConstants;
import com.PaymentService.paymentservice.dtos.*;
import com.PaymentService.paymentservice.services.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/")
    public ResponseEntity<InitiatePaymentResponseDto> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequestDto requestDto) {
        logger.info("Initiating payment for orderId: {}", requestDto.getOrderId());
        String gatewayType = requestDto.getGatewayType() != null 
                ? requestDto.getGatewayType() 
                : PaymentConstants.GATEWAY_RAZORPAY;
        InitiatePaymentResponseDto response = paymentService.initiatePayment(
                requestDto.getOrderId(),
                requestDto.getAmount(),
                requestDto.getPhoneNumber(),
                requestDto.getEmail(),
                gatewayType
        );
        logger.info("Payment initiated successfully for orderId: {}", requestDto.getOrderId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyPaymentResponseDto> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequestDto requestDto) {
        logger.info("Verifying payment - paymentId: {}, orderId: {}", 
                requestDto.getPaymentId(), requestDto.getOrderId());
        VerifyPaymentResponseDto response = paymentService.verifyPayment(
                requestDto.getPaymentId(),
                requestDto.getOrderId()
        );
        logger.info("Payment verification completed - status: {}", response.getStatus());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund")
    public ResponseEntity<RefundPaymentResponseDto> refundPayment(
            @Valid @RequestBody RefundPaymentRequestDto requestDto) {
        logger.info("Processing refund for paymentId: {}, amount: {}", 
                requestDto.getPaymentId(), requestDto.getAmount());
        RefundPaymentResponseDto response = paymentService.refundPayment(
                requestDto.getPaymentId(),
                requestDto.getAmount()
        );
        logger.info("Refund processed successfully - refundId: {}", response.getRefundId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{paymentId}")
    public ResponseEntity<PaymentStatusResponseDto> getPaymentStatus(@PathVariable String paymentId) {
        logger.debug("Getting payment status for paymentId: {}", paymentId);
        PaymentStatusResponseDto response = paymentService.getPaymentStatus(paymentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/order/{orderId}")
    public ResponseEntity<PaymentStatusResponseDto> getPaymentStatusByOrderId(@PathVariable String orderId) {
        logger.debug("Getting payment status for orderId: {}", orderId);
        PaymentStatusResponseDto response = paymentService.getPaymentStatusByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload, 
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String razorpaySignature) {
        logger.info("Webhook received - signature present: {}", razorpaySignature != null);
        // TODO: Implement webhook signature verification and payment status update
        // For Razorpay: verify using razorpaySignature
        // For Stripe: verify using Stripe webhook signature
        return ResponseEntity.ok("Webhook received successfully");
    }
}
