package com.PaymentService.paymentservice.paymentgateway;

import com.PaymentService.paymentservice.models.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayFactory {
    
    private final RazorpaymentGateway razorpaymentGateway;
    private final StripePaymentGateway stripePaymentGateway;

    public PaymentGatewayFactory(
            RazorpaymentGateway razorpaymentGateway,
            StripePaymentGateway stripePaymentGateway) {
        this.razorpaymentGateway = razorpaymentGateway;
        this.stripePaymentGateway = stripePaymentGateway;
    }

    public PaymentGateway getGateway(String gatewayType) {
        if (gatewayType == null || gatewayType.isEmpty()) {
            return razorpaymentGateway; // Default to Razorpay
        }
        
        Payment.PaymentGatewayType type = Payment.PaymentGatewayType.valueOf(gatewayType.toUpperCase());
        
        switch (type) {
            case RAZORPAY:
                return razorpaymentGateway;
            case STRIPE:
                return stripePaymentGateway;
            default:
                return razorpaymentGateway;
        }
    }

    public PaymentGateway getGateway(Payment.PaymentGatewayType gatewayType) {
        if (gatewayType == null) {
            return razorpaymentGateway;
        }
        
        switch (gatewayType) {
            case RAZORPAY:
                return razorpaymentGateway;
            case STRIPE:
                return stripePaymentGateway;
            default:
                return razorpaymentGateway;
        }
    }
}

