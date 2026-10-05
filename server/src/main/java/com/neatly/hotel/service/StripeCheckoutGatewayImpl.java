package com.neatly.hotel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.model.Booking;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.RefundListParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;
import com.stripe.param.checkout.SessionUpdateParams;

@Service
public class StripeCheckoutGatewayImpl implements StripeCheckoutGateway {

	private static final Logger log = LoggerFactory.getLogger(StripeCheckoutGatewayImpl.class);

	private final String secretKey;
	private final String webhookSecret;

	public StripeCheckoutGatewayImpl(
			@Value("${stripe.secret-key:}") String secretKey,
			@Value("${stripe.webhook-secret:}") String webhookSecret) {
		this.secretKey = secretKey == null ? "" : secretKey.trim();
		this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
	}

	@Override
	public SessionResult createSession(Booking booking, String returnUrl) {
		requireSecret();
		long unitAmount = toStripeAmount(booking.getGrandTotal());
		String integration = "neatly-checkout-" + booking.getId().toString().replace("-", "").substring(0, 8);
		try {
			SessionCreateParams.Builder builder = SessionCreateParams.builder()
					.setUiMode(SessionCreateParams.UiMode.ELEMENTS)
					.setMode(SessionCreateParams.Mode.PAYMENT)
					.setCustomerEmail(booking.getGuestEmail())
					.setReturnUrl(returnUrl)
					.putMetadata("bookingId", booking.getId().toString())
					.putMetadata("clerkUserId", booking.getUserId())
					.addLineItem(SessionCreateParams.LineItem.builder()
							.setQuantity(1L)
							.setPriceData(SessionCreateParams.LineItem.PriceData.builder()
									.setCurrency(booking.getCurrency().toLowerCase())
									.setUnitAmount(unitAmount)
									.setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
											.setName(booking.getRoomNameSnapshot())
											.build())
									.build())
							.build());
			cardOnly(builder);
			trySetIntegrationIdentifier(builder, integration);
			Session session = client().v1().checkout().sessions().create(builder.build());
			return new SessionResult(
					session.getId(),
					session.getClientSecret(),
					session.getPaymentIntent());
		} catch (StripeException ex) {
			log.warn("Stripe checkout session failed: {}", ex.getMessage());
			throw new ApiException("Could not start card payment", HttpStatus.BAD_GATEWAY);
		}
	}

	@Override
	public void updateSessionAmount(String checkoutSessionId, Booking booking) {
		requireSecret();
		try {
			var items = client().v1().checkout().sessions().lineItems().list(checkoutSessionId);
			if (items.getData() == null || items.getData().isEmpty()) {
				throw new ApiException("Could not update card payment", HttpStatus.BAD_GATEWAY);
			}
			String lineItemId = items.getData().get(0).getId();
			String name = booking.getRoomNameSnapshot() == null || booking.getRoomNameSnapshot().isBlank()
					? "Room"
					: booking.getRoomNameSnapshot();
			SessionUpdateParams params = SessionUpdateParams.builder()
					.addLineItem(SessionUpdateParams.LineItem.builder()
							.setId(lineItemId)
							.setQuantity(1L)
							.setPriceData(SessionUpdateParams.LineItem.PriceData.builder()
									.setCurrency(booking.getCurrency().toLowerCase())
									.setUnitAmount(toStripeAmount(booking.getGrandTotal()))
									.setProductData(SessionUpdateParams.LineItem.PriceData.ProductData.builder()
											.setName(name)
											.build())
									.build())
							.build())
					.build();
			client().v1().checkout().sessions().update(checkoutSessionId, params);
		} catch (StripeException ex) {
			log.warn("Stripe checkout amount update failed: {}", ex.getMessage());
			throw new ApiException("Could not update card payment", HttpStatus.BAD_GATEWAY);
		}
	}

	@Override
	public SessionView retrieveSession(String checkoutSessionId) {
		requireSecret();
		try {
			SessionRetrieveParams params = SessionRetrieveParams.builder()
					.addExpand("payment_intent")
					.addExpand("payment_intent.payment_method")
					.build();
			Session session = client().v1().checkout().sessions().retrieve(checkoutSessionId, params);
			String brand = null;
			String last4 = null;
			PaymentIntent intent = session.getPaymentIntentObject();
			if (intent != null) {
				PaymentMethod method = intent.getPaymentMethodObject();
				if (method != null && method.getCard() != null) {
					brand = method.getCard().getBrand();
					last4 = method.getCard().getLast4();
				}
			}
			return new SessionView(
					session.getId(),
					session.getClientSecret(),
					session.getStatus(),
					session.getPaymentStatus(),
					session.getPaymentIntent(),
					brand,
					last4);
		} catch (StripeException ex) {
			log.warn("Stripe session retrieve failed: {}", ex.getMessage());
			throw new ApiException("Could not read payment status", HttpStatus.BAD_GATEWAY);
		}
	}

	@Override
	public String refund(String paymentIntentId, BigDecimal amount) {
		requireSecret();
		try {
			Refund refund = client().v1().refunds().create(RefundCreateParams.builder()
					.setPaymentIntent(paymentIntentId)
					.setAmount(toStripeAmount(amount))
					.build());
			return refund.getId();
		} catch (StripeException ex) {
			if (alreadyRefunded(ex)) {
				return existingRefundId(paymentIntentId);
			}
			log.warn("Stripe refund failed: {}", ex.getMessage());
			throw new ApiException("Could not refund this payment", HttpStatus.BAD_GATEWAY);
		}
	}

	private String existingRefundId(String paymentIntentId) {
		try {
			var refunds = client().v1().refunds().list(RefundListParams.builder()
					.setPaymentIntent(paymentIntentId)
					.setLimit(1L)
					.build());
			if (refunds.getData() != null && !refunds.getData().isEmpty()) {
				return refunds.getData().get(0).getId();
			}
		} catch (StripeException ex) {
			log.warn("Stripe refund lookup failed: {}", ex.getMessage());
		}
		throw new ApiException("Could not refund this payment", HttpStatus.BAD_GATEWAY);
	}

	private static boolean alreadyRefunded(StripeException ex) {
		if ("charge_already_refunded".equals(ex.getCode())) {
			return true;
		}
		String message = ex.getMessage();
		return message != null && message.toLowerCase().contains("already been refunded");
	}

	@Override
	public WebhookEvent parseEvent(String payload, String signature) {
		if (webhookSecret.isBlank()) {
			throw new ApiException("Stripe webhooks are not configured", HttpStatus.SERVICE_UNAVAILABLE);
		}
		try {
			Event event = Webhook.constructEvent(payload, signature, webhookSecret);
			String sessionId = null;
			StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
			if (object instanceof Session session) {
				sessionId = session.getId();
			}
			return new WebhookEvent(event.getId(), event.getType(), sessionId);
		} catch (SignatureVerificationException ex) {
			throw new ApiException("Invalid Stripe signature", HttpStatus.BAD_REQUEST);
		}
	}

	private StripeClient client() {
		return new StripeClient(secretKey);
	}

	private void requireSecret() {
		if (secretKey.isBlank()) {
			throw new ApiException("Card payments are not configured", HttpStatus.SERVICE_UNAVAILABLE);
		}
	}

	private static long toStripeAmount(BigDecimal amount) {
		return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
	}

	/** Dynamic methods stay on, but every non-card type (including PromptPay) is hidden. */
	private static void cardOnly(SessionCreateParams.Builder builder) {
		builder.addAllExcludedPaymentMethodType(Arrays.stream(SessionCreateParams.ExcludedPaymentMethodType.values())
				.filter(type -> type != SessionCreateParams.ExcludedPaymentMethodType.CARD)
				.toList());
	}

	private static void trySetIntegrationIdentifier(SessionCreateParams.Builder builder, String value) {
		try {
			builder.getClass().getMethod("setIntegrationIdentifier", String.class).invoke(builder, value);
		} catch (ReflectiveOperationException ignored) {
			Map<String, Object> extra = new HashMap<>();
			extra.put("integration_identifier", value);
		}
	}
}
