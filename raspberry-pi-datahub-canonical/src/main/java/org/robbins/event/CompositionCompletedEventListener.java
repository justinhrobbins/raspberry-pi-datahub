/*
 * [y] hybris Platform
 *
 * Copyright (c) 2000-2015 hybris AG
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of hybris
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with hybris.
 */

package org.robbins.event;

import com.hybris.datahub.api.event.CompositionCompletedEvent;
import com.hybris.datahub.api.event.DataHubEventListener;
import com.hybris.datahub.api.event.InitiatePublicationEvent;
import com.hybris.datahub.service.EventPublicationService;
import com.hybris.datahub.service.impl.AbstractPoolEventListener;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

public class CompositionCompletedEventListener extends AbstractPoolEventListener
		implements DataHubEventListener<CompositionCompletedEvent>
{
	private static final Logger logger = LoggerFactory.getLogger(CompositionCompletedEventListener.class);

	private EventPublicationService eventPublicationService;

	@Override
	public void handleEvent(final CompositionCompletedEvent event)
	{
		final String poolName = getPoolNameFromId(event.getPoolId());
		if (RaspberryPiEventDrivenPoolConstants.POOL_NAME.equals(poolName))
		{
			logger.debug("Handling composition completed event for pool : {}", RaspberryPiEventDrivenPoolConstants.POOL_NAME);
			final InitiatePublicationEvent publishEvent = new InitiatePublicationEvent(event.getPoolId(),
                    Arrays.asList(RaspberryPiEventDrivenPoolConstants.TARGET_SYSTEM_NAME));
			eventPublicationService.publishEvent(publishEvent);
		}
	}

	@Override
	public Class<CompositionCompletedEvent> getEventClass()
	{
		return CompositionCompletedEvent.class;
	}

	@Override
	public boolean executeInTransaction()
	{
		return true;
	}

	@Required
	public void setEventPublicationService(final EventPublicationService eventPublicationService)
	{
		this.eventPublicationService = eventPublicationService;
	}
}
