package com.PaymentService.paymentservice.constants;

public class PaymentConstants {
    private PaymentConstants() {
        // Utility class
    }

    // Payment Status Constants
    public static final String STATUS_CAPTURED = "captured";
    public static final String STATUS_PAID = "paid";
    public static final String STATUS_SUCCEEDED = "succeeded";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_CANCELED = "canceled";

    // Currency
    public static final String CURRENCY_INR = "INR";
    public static final String CURRENCY_USD = "USD";

    // Gateway Types
    public static final String GATEWAY_RAZORPAY = "RAZORPAY";
    public static final String GATEWAY_STRIPE = "STRIPE";

    // Payment Link Expiry (7 days in seconds)
    public static final long PAYMENT_LINK_EXPIRY_SECONDS = 7 * 24 * 60 * 60;

    // Messages
    public static final String MESSAGE_PAYMENT_LINK_GENERATED = "Payment link generated successfully";
    public static final String MESSAGE_PAYMENT_LINK_EXISTS = "Payment link already exists";
    public static final String MESSAGE_PAYMENT_VERIFIED = "Payment verified successfully";
    public static final String MESSAGE_REFUND_PROCESSED = "Refund processed successfully";
}


