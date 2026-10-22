package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.command.SubscribeToPushCommand;
import com.github.angellariosacosta.bookingapp.application.command.UnsubscribeFromPushCommand;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PushSubscriptionRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PushUnsubscribeRequest;

@Component
public class PushRestMapper {

	private static final int USER_AGENT_MAX_LENGTH = 255;

	public SubscribeToPushCommand toCommand(PushSubscriptionRequest request, Long userId, String userAgent) {
		return new SubscribeToPushCommand(userId, request.endpoint(), request.keys().p256dh(), request.keys().auth(),
				truncate(userAgent));
	}

	public UnsubscribeFromPushCommand toCommand(PushUnsubscribeRequest request, Long userId) {
		return new UnsubscribeFromPushCommand(userId, request.endpoint());
	}

	private static String truncate(String value) {
		return value == null || value.length() <= USER_AGENT_MAX_LENGTH ? value : value.substring(0, USER_AGENT_MAX_LENGTH);
	}
}
