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

import com.hybris.datahub.api.event.DataHubEventListener;
import com.hybris.datahub.api.event.DataLoadingCompletedEvent;
import com.hybris.datahub.api.event.InitiateCompositionEvent;
import com.hybris.datahub.service.EventPublicationService;
import com.hybris.datahub.service.impl.AbstractPoolEventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

public class DataLoadedEventListener extends AbstractPoolEventListener
		implements DataHubEventListener<DataLoadingCompletedEvent>
{
	private static final Logger logger = LoggerFactory.getLogger(DataLoadedEventListener.class);

	private EventPublicationService eventPublicationService;

	@Override
	public void handleEvent(final DataLoadingCompletedEvent event)
	{
		final String poolName = getPoolNameFromId(event.getPoolId());
		if (RaspberryPiEventDrivenPoolConstants.POOL_NAME.equals(poolName))
		{
			logger.debug("Handling data loaded event for pool : {}", RaspberryPiEventDrivenPoolConstants.POOL_NAME);
			final InitiateCompositionEvent composeEvent = new InitiateCompositionEvent(event.getPoolId());
			eventPublicationService.publishEvent(composeEvent);
		}
	}

	@Override
	public Class<DataLoadingCompletedEvent> getEventClass()
	{
		return DataLoadingCompletedEvent.class;
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
