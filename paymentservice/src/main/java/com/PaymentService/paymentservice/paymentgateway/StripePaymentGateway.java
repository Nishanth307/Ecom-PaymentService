package com.PaymentService.paymentservice.paymentgateway;

import com.razorpay.RazorpayException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentLink;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentLinkCreateParams;
import com.stripe.param.PaymentIntentRetrieveParams;
import com.stripe.param.RefundCreateParams;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StripePaymentGateway implements PaymentGateway {

    @Override
    public Map<String, String> generatePaymentLink(String orderId, Long amount, String phoneNumber, String email) throws RazorpayException {
        try {
            // Stripe amounts are in smallest currency unit (cents for USD, paise for INR)
            // Assuming amount is already in smallest currency unit (paise for INR)
            long amountInSmallestUnit = amount;
            
            PaymentLinkCreateParams params = PaymentLinkCreateParams.builder()
                    .addLineItem(
                            PaymentLinkCreateParams.LineItem.builder()
                                    .setPriceData(
                                            PaymentLinkCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("inr") // Using INR to match Razorpay
                                                    .setUnitAmount(amountInSmallestUnit)
                                                    .setProductData(
                                                            PaymentLinkCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Order #" + orderId)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .setQuantity(1L)
                                    .build()
                    )
                    .setMetadata(Map.of("order_id", orderId))
                    .setAfterCompletion(
                            PaymentLinkCreateParams.AfterCompletion.builder()
                                    .setType(PaymentLinkCreateParams.AfterCompletion.Type.REDIRECT)
                                    .setRedirect(
                                            PaymentLinkCreateParams.AfterCompletion.Redirect.builder()
                                                    .setUrl("https://example-callback-url.com/")
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            PaymentLink paymentLink = PaymentLink.create(params);

            Map<String, String> response = new HashMap<>();
            response.put("paymentLinkId", paymentLink.getId());
            response.put("paymentLinkUrl", paymentLink.getUrl());
            response.put("orderId", orderId);

            return response;
        } catch (StripeException e) {
            throw new RuntimeException("Stripe payment link creation failed: " + e.getMessage(), e);
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
            // First get the payment intent to find the charge
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentId);
            
            RefundCreateParams.Builder refundParamsBuilder = RefundCreateParams.builder();
            
            if (paymentIntent.getLatestCharge() != null) {
                refundParamsBuilder.setCharge(paymentIntent.getLatestCharge().toString());
            } else if (paymentIntent.getCharges() != null && !paymentIntent.getCharges().getData().isEmpty()) {
                refundParamsBuilder.setCharge(paymentIntent.getCharges().getData().get(0).getId());
            } else {
                throw new RuntimeException("No charge found for payment: " + paymentId);
            }
            
            if (amount != null && amount > 0) {
                refundParamsBuilder.setAmount(amount);
            }

            Refund refund = Refund.create(refundParamsBuilder.build());

            Map<String, String> response = new HashMap<>();
            response.put("refundId", refund.getId());
            response.put("paymentId", paymentId);
            response.put("amount", String.valueOf(refund.getAmount()));
            response.put("status", refund.getStatus());

            return response;
        } catch (StripeException e) {
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
