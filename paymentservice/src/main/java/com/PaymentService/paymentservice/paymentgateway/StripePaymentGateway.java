package com.PaymentService.paymentservice.paymentgateway;

import com.PaymentService.paymentservice.constants.PaymentConstants;
import com.razorpay.RazorpayException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentRetrieveParams;
import com.stripe.param.RefundCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StripePaymentGateway implements PaymentGateway {
    private static final Logger logger = LoggerFactory.getLogger(StripePaymentGateway.class);

    @Override
    public Map<String, String> generatePaymentLink(String orderId, Long amount, String phoneNumber, String email) throws RazorpayException {
        try {
            logger.debug("Generating Stripe payment intent for orderId: {}", orderId);
            // Stripe amounts are in smallest currency unit (cents for USD, paise for INR)
            // Assuming amount is already in smallest currency unit (paise for INR)
            long amountInSmallestUnit = amount;
            
            // Create a payment intent directly
            PaymentIntent paymentIntent = PaymentIntent.create(
                    PaymentIntentCreateParams.builder()
                            .setAmount(amountInSmallestUnit)
                            .setCurrency(PaymentConstants.CURRENCY_INR.toLowerCase())
                            .putMetadata("order_id", orderId)
                            .putMetadata("email", email)
                            .putMetadata("phone", phoneNumber)
                            .setAutomaticPaymentMethods(
                                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                            .setEnabled(true)
                                            .build()
                            )
                            .build()
            );
            
            logger.info("Stripe payment intent created successfully - orderId: {}, paymentIntentId: {}", 
                    orderId, paymentIntent.getId());

            Map<String, String> response = new HashMap<>();
            // For payment link, we'll use the client secret which can be used to create a checkout session
            // In a real implementation, you'd create a Checkout Session here
            response.put("paymentLinkId", paymentIntent.getId());
            response.put("paymentLinkUrl", "https://checkout.stripe.com/pay/" + paymentIntent.getClientSecret());
            response.put("orderId", orderId);

            return response;
        } catch (StripeException e) {
            logger.error("Stripe payment creation failed for orderId: {}", orderId, e);
            throw new RuntimeException("Stripe payment creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> verifyPayment(String paymentId) throws RazorpayException {
        try {
            PaymentIntentRetrieveParams params = PaymentIntentRetrieveParams.builder()
                    .addExpand("charges")
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentId, params, null);

            Map<String, String> response = new HashMap<>();
            response.put("paymentId", paymentIntent.getId());
            response.put("status", paymentIntent.getStatus());
            response.put("amount", String.valueOf(paymentIntent.getAmount()));
            response.put("currency", paymentIntent.getCurrency());
            
            if (paymentIntent.getMetadata() != null && paymentIntent.getMetadata().containsKey("order_id")) {
                response.put("orderId", paymentIntent.getMetadata().get("order_id"));
            }

            return response;
        } catch (StripeException e) {
            throw new RuntimeException("Stripe payment verification failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> refundPayment(String paymentId, Long amount) throws RazorpayException {
        try {
            logger.debug("Processing refund for paymentId: {}", paymentId);
            // First get the payment intent to find the charge
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentId);
            
            RefundCreateParams.Builder refundParamsBuilder = RefundCreateParams.builder();
            
            String chargeId = null;
            if (paymentIntent.getLatestCharge() != null) {
                chargeId = paymentIntent.getLatestCharge().toString();
            } else {
                // Try to get charge from payment intent metadata or use payment intent ID
                // In newer Stripe API, we can refund directly using payment intent
                chargeId = paymentIntent.getId();
            }
            
            if (chargeId != null) {
                refundParamsBuilder.setPaymentIntent(paymentId);
            } else {
                throw new RuntimeException("No charge found for payment: " + paymentId);
            }
            
            if (amount != null && amount > 0) {
                refundParamsBuilder.setAmount(amount);
            }

            Refund refund = Refund.create(refundParamsBuilder.build());
            logger.info("Refund processed successfully - paymentId: {}, refundId: {}", paymentId, refund.getId());

            Map<String, String> response = new HashMap<>();
            response.put("refundId", refund.getId());
            response.put("paymentId", paymentId);
            response.put("amount", String.valueOf(refund.getAmount()));
            response.put("status", refund.getStatus());

            return response;
        } catch (StripeException e) {
            logger.error("Stripe refund failed for paymentId: {}", paymentId, e);
            throw new RuntimeException("Stripe refund failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getPaymentStatus(String paymentId) throws RazorpayException {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentId);

            Map<String, String> response = new HashMap<>();
            response.put("paymentId", paymentIntent.getId());
            response.put("status", paymentIntent.getStatus());
            response.put("amount", String.valueOf(paymentIntent.getAmount()));
            response.put("currency", paymentIntent.getCurrency());
            response.put("method", paymentIntent.getPaymentMethodTypes() != null && !paymentIntent.getPaymentMethodTypes().isEmpty() 
                    ? paymentIntent.getPaymentMethodTypes().get(0) : "unknown");
            
            if (paymentIntent.getMetadata() != null && paymentIntent.getMetadata().containsKey("order_id")) {
                response.put("orderId", paymentIntent.getMetadata().get("order_id"));
            }

            return response;
        } catch (StripeException e) {
            throw new RuntimeException("Stripe payment status retrieval failed: " + e.getMessage(), e);
        }
    }
}
