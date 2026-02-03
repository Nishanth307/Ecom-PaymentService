package com.PaymentService.paymentservice.paymentgateway;

import com.razorpay.RazorpayException;

import java.util.Map;

public interface PaymentGateway {
    Map<String, String> generatePaymentLink(String orderId, Long amount, String phoneNumber, String email) throws RazorpayException;
    
    Map<String, String> verifyPayment(String paymentId) throws RazorpayException;
    
    Map<String, String> refundPayment(String paymentId, Long amount) throws RazorpayException;
    
    Map<String, String> getPaymentStatus(String paymentId) throws RazorpayException;
}
