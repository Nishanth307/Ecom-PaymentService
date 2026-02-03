package com.PaymentService.paymentservice.services;

import com.PaymentService.paymentservice.constants.PaymentConstants;
import com.PaymentService.paymentservice.dtos.InitiatePaymentResponseDto;
import com.PaymentService.paymentservice.dtos.PaymentStatusResponseDto;
import com.PaymentService.paymentservice.dtos.RefundPaymentResponseDto;
import com.PaymentService.paymentservice.dtos.VerifyPaymentResponseDto;
import com.PaymentService.paymentservice.exceptions.InvalidPaymentStateException;
import com.PaymentService.paymentservice.exceptions.PaymentNotFoundException;
import com.PaymentService.paymentservice.exceptions.PaymentProcessingException;
import com.PaymentService.paymentservice.models.Payment;
import com.PaymentService.paymentservice.paymentgateway.PaymentGateway;
import com.PaymentService.paymentservice.paymentgateway.PaymentGatewayFactory;
import com.PaymentService.paymentservice.repositories.PaymentRepository;
import com.razorpay.RazorpayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentGatewayFactory paymentGatewayFactory;
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentGatewayFactory paymentGatewayFactory, PaymentRepository paymentRepository) {
        this.paymentGatewayFactory = paymentGatewayFactory;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public InitiatePaymentResponseDto initiatePayment(String orderId, Long amount, String phoneNumber, String email, String gatewayType) {
        // Check if payment already exists for this order
        Payment existingPayment = paymentRepository.findByOrderId(orderId)
                .orElse(null);

        if (existingPayment != null && existingPayment.getStatus() == Payment.PaymentStatus.PENDING) {
            logger.info("Existing pending payment found for orderId: {}", orderId);
            InitiatePaymentResponseDto response = new InitiatePaymentResponseDto();
            response.setOrderId(existingPayment.getOrderId());
            response.setPaymentLinkId(existingPayment.getPaymentLinkId());
            response.setPaymentLinkUrl(existingPayment.getPaymentLinkUrl());
            response.setMessage(PaymentConstants.MESSAGE_PAYMENT_LINK_EXISTS);
            return response;
        }

        try {
            PaymentGateway gateway = paymentGatewayFactory.getGateway(gatewayType);
            Map<String, String> gatewayResponse = gateway.generatePaymentLink(orderId, amount, phoneNumber, email);

            Payment payment = Payment.builder()
                    .orderId(orderId)
                    .amount(amount)
                    .currency(PaymentConstants.CURRENCY_INR)
                    .phoneNumber(phoneNumber)
                    .email(email)
                    .status(Payment.PaymentStatus.PENDING)
                    .gatewayType(Payment.PaymentGatewayType.valueOf(gatewayType.toUpperCase()))
                    .paymentLinkId(gatewayResponse.get("paymentLinkId"))
                    .paymentLinkUrl(gatewayResponse.get("paymentLinkUrl"))
                    .build();

            payment = paymentRepository.save(payment);
            logger.info("Payment link created successfully for orderId: {}, paymentLinkId: {}", 
                    orderId, payment.getPaymentLinkId());

            InitiatePaymentResponseDto response = new InitiatePaymentResponseDto();
            response.setOrderId(payment.getOrderId());
            response.setPaymentLinkId(payment.getPaymentLinkId());
            response.setPaymentLinkUrl(payment.getPaymentLinkUrl());
            response.setMessage(PaymentConstants.MESSAGE_PAYMENT_LINK_GENERATED);
            return response;
        } catch (RazorpayException e) {
            logger.error("Gateway error while creating payment link for orderId: {}", orderId, e);
            throw new PaymentProcessingException("Failed to generate payment link: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid gateway type: {}", gatewayType);
            throw new PaymentProcessingException("Invalid gateway type: " + gatewayType);
        }
    }

    @Transactional
    public VerifyPaymentResponseDto verifyPayment(String paymentId, String orderId) {
        Payment payment = null;
        
        if (paymentId != null && !paymentId.isEmpty()) {
            payment = paymentRepository.findByPaymentId(paymentId).orElse(null);
        } else if (orderId != null && !orderId.isEmpty()) {
            payment = paymentRepository.findByOrderId(orderId).orElse(null);
        }

        if (payment == null) {
            logger.warn("Payment not found - paymentId: {}, orderId: {}", paymentId, orderId);
            throw new PaymentNotFoundException("Payment not found");
        }

        try {
            PaymentGateway gateway = paymentGatewayFactory.getGateway(payment.getGatewayType());
            String paymentIdToVerify = payment.getPaymentId() != null ? payment.getPaymentId() : paymentId;
            Map<String, String> gatewayResponse = gateway.verifyPayment(paymentIdToVerify);

            // Update payment status
            String status = gatewayResponse.get("status");
            if (PaymentConstants.STATUS_CAPTURED.equals(status) || 
                PaymentConstants.STATUS_PAID.equals(status) || 
                PaymentConstants.STATUS_SUCCEEDED.equals(status)) {
                payment.setStatus(Payment.PaymentStatus.SUCCESS);
                if (payment.getPaymentId() == null) {
                    payment.setPaymentId(gatewayResponse.get("paymentId"));
                }
            } else if (PaymentConstants.STATUS_FAILED.equals(status) || 
                       PaymentConstants.STATUS_CANCELED.equals(status)) {
                payment.setStatus(Payment.PaymentStatus.FAILED);
            }

            payment = paymentRepository.save(payment);
            logger.info("Payment verified - paymentId: {}, status: {}", payment.getPaymentId(), payment.getStatus());

            VerifyPaymentResponseDto response = new VerifyPaymentResponseDto();
            response.setPaymentId(payment.getPaymentId());
            response.setOrderId(payment.getOrderId());
            response.setStatus(payment.getStatus().toString());
            response.setAmount(Long.parseLong(gatewayResponse.get("amount")));
            response.setCurrency(gatewayResponse.get("currency"));
            response.setMessage(PaymentConstants.MESSAGE_PAYMENT_VERIFIED);
            return response;
        } catch (RazorpayException e) {
            logger.error("Gateway error while verifying payment: {}", paymentId, e);
            throw new PaymentProcessingException("Failed to verify payment: " + e.getMessage(), e);
        }
    }

    @Transactional
    public RefundPaymentResponseDto refundPayment(String paymentId, Long amount) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> {
                    logger.warn("Payment not found for refund: {}", paymentId);
                    return new PaymentNotFoundException("Payment not found");
                });

        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS) {
            logger.warn("Invalid payment state for refund - paymentId: {}, status: {}", 
                    paymentId, payment.getStatus());
            throw new InvalidPaymentStateException("Only successful payments can be refunded");
        }

        try {
            PaymentGateway gateway = paymentGatewayFactory.getGateway(payment.getGatewayType());
            Map<String, String> gatewayResponse = gateway.refundPayment(paymentId, amount);

            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            payment = paymentRepository.save(payment);
            logger.info("Refund processed successfully - paymentId: {}, refundId: {}", 
                    paymentId, gatewayResponse.get("refundId"));

            RefundPaymentResponseDto response = new RefundPaymentResponseDto();
            response.setRefundId(gatewayResponse.get("refundId"));
            response.setPaymentId(paymentId);
            response.setAmount(Long.parseLong(gatewayResponse.get("amount")));
            response.setStatus(gatewayResponse.get("status"));
            response.setMessage(PaymentConstants.MESSAGE_REFUND_PROCESSED);
            return response;
        } catch (RazorpayException e) {
            logger.error("Gateway error while processing refund: {}", paymentId, e);
            throw new PaymentProcessingException("Failed to process refund: " + e.getMessage(), e);
        }
    }

    public PaymentStatusResponseDto getPaymentStatus(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElse(null);

        if (payment == null) {
            logger.warn("Payment not found in database, attempting to fetch from gateway: {}", paymentId);
            // Try to get status from gateway directly (default to Razorpay)
            try {
                PaymentGateway gateway = paymentGatewayFactory.getGateway(PaymentConstants.GATEWAY_RAZORPAY);
                Map<String, String> gatewayResponse = gateway.getPaymentStatus(paymentId);
                PaymentStatusResponseDto response = new PaymentStatusResponseDto();
                response.setPaymentId(gatewayResponse.get("paymentId"));
                response.setOrderId(gatewayResponse.get("orderId"));
                response.setStatus(gatewayResponse.get("status"));
                response.setAmount(Long.parseLong(gatewayResponse.get("amount")));
                response.setCurrency(gatewayResponse.get("currency"));
                response.setMethod(gatewayResponse.get("method"));
                return response;
            } catch (RazorpayException e) {
                logger.error("Failed to fetch payment status from gateway: {}", paymentId, e);
                throw new PaymentNotFoundException("Payment not found: " + paymentId);
            }
        }

        // Get latest status from gateway
        if (payment.getPaymentId() != null) {
            try {
                PaymentGateway gateway = paymentGatewayFactory.getGateway(payment.getGatewayType());
                Map<String, String> gatewayResponse = gateway.getPaymentStatus(payment.getPaymentId());
                
                PaymentStatusResponseDto response = new PaymentStatusResponseDto();
                response.setPaymentId(payment.getPaymentId());
                response.setOrderId(payment.getOrderId());
                response.setStatus(gatewayResponse.get("status"));
                response.setAmount(Long.parseLong(gatewayResponse.get("amount")));
                response.setCurrency(gatewayResponse.get("currency"));
                response.setMethod(gatewayResponse.get("method"));
                return response;
            } catch (RazorpayException e) {
                logger.error("Failed to fetch payment status from gateway: {}", payment.getPaymentId(), e);
                // Fall back to database status
            }
        }

        PaymentStatusResponseDto response = new PaymentStatusResponseDto();
        response.setPaymentId(payment.getPaymentId());
        response.setOrderId(payment.getOrderId());
        response.setStatus(payment.getStatus().toString());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        return response;
    }

    public PaymentStatusResponseDto getPaymentStatusByOrderId(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    logger.warn("Payment not found for orderId: {}", orderId);
                    return new PaymentNotFoundException("Payment not found for order: " + orderId);
                });

        PaymentStatusResponseDto response = new PaymentStatusResponseDto();
        response.setPaymentId(payment.getPaymentId());
        response.setOrderId(payment.getOrderId());
        response.setStatus(payment.getStatus().toString());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        return response;
    }
}
