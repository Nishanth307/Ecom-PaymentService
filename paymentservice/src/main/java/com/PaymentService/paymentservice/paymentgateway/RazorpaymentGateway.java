package com.PaymentService.paymentservice.paymentgateway;

import com.PaymentService.paymentservice.constants.PaymentConstants;
import com.razorpay.PaymentLink;
import com.razorpay.Refund;
import org.json.JSONObject;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class RazorpaymentGateway implements PaymentGateway {
    private static final Logger logger = LoggerFactory.getLogger(RazorpaymentGateway.class);
    private final RazorpayClient razorpayClient;

    public RazorpaymentGateway(RazorpayClient razorpayClient) {
        this.razorpayClient = razorpayClient;
    }

    @Override
    public Map<String, String> generatePaymentLink(String orderId, Long amount, String phoneNumber, String email) throws RazorpayException {
        logger.debug("Generating Razorpay payment link for orderId: {}", orderId);
        JSONObject paymentLinkRequest = new JSONObject();
        paymentLinkRequest.put("amount", amount);
        paymentLinkRequest.put("currency", PaymentConstants.CURRENCY_INR);
        paymentLinkRequest.put("accept_partial", false);
        
        // Set expiry to 7 days from now
        long expiryTime = Instant.now().plusSeconds(PaymentConstants.PAYMENT_LINK_EXPIRY_SECONDS).getEpochSecond();
        paymentLinkRequest.put("expire_by", expiryTime);
        
        paymentLinkRequest.put("reference_id", orderId);
        paymentLinkRequest.put("description", "Payment for order #" + orderId);
        
        JSONObject customer = new JSONObject();
        customer.put("name", phoneNumber);
        customer.put("contact", phoneNumber);
        customer.put("email", email);
        paymentLinkRequest.put("customer", customer);
        
        JSONObject notify = new JSONObject();
        notify.put("sms", true);
        notify.put("email", true);
        paymentLinkRequest.put("notify", notify);
        paymentLinkRequest.put("reminder_enable", true);
        
        JSONObject notes = new JSONObject();
        notes.put("order_id", orderId);
        paymentLinkRequest.put("notes", notes);

        PaymentLink paymentLink = razorpayClient.paymentLink.create(paymentLinkRequest);
        logger.info("Razorpay payment link created successfully - orderId: {}, paymentLinkId: {}", 
                orderId, paymentLink.get("id"));
        
        Map<String, String> response = new HashMap<>();
        response.put("paymentLinkId", paymentLink.get("id"));
        response.put("paymentLinkUrl", paymentLink.get("short_url"));
        response.put("orderId", orderId);
        
        return response;
    }

    @Override
    public Map<String, String> verifyPayment(String paymentId) throws RazorpayException {
        Payment payment = razorpayClient.payments.fetch(paymentId);
        
        Map<String, String> response = new HashMap<>();
        response.put("paymentId", payment.get("id"));
        response.put("status", payment.get("status"));
        response.put("amount", payment.get("amount").toString());
        response.put("currency", payment.get("currency"));
        response.put("orderId", payment.get("order_id"));
        
        return response;
    }

    @Override
    public Map<String, String> refundPayment(String paymentId, Long amount) throws RazorpayException {
        JSONObject refundRequest = new JSONObject();
        if (amount != null && amount > 0) {
            refundRequest.put("amount", amount);
        }
        
        Refund refund = razorpayClient.payments.refund(paymentId, refundRequest);
        
        Map<String, String> response = new HashMap<>();
        response.put("refundId", refund.get("id"));
        response.put("paymentId", paymentId);
        response.put("amount", refund.get("amount").toString());
        response.put("status", refund.get("status"));
        
        return response;
    }

    @Override
    public Map<String, String> getPaymentStatus(String paymentId) throws RazorpayException {
        Payment payment = razorpayClient.payments.fetch(paymentId);
        
        Map<String, String> response = new HashMap<>();
        response.put("paymentId", payment.get("id"));
        response.put("status", payment.get("status"));
        response.put("amount", payment.get("amount").toString());
        response.put("currency", payment.get("currency"));
        response.put("orderId", payment.get("order_id"));
        response.put("method", payment.get("method"));
        
        return response;
    }
}
