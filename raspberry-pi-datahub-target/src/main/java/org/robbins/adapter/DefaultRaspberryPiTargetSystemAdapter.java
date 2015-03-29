package org.robbins.adapter;

import com.hybris.datahub.adapter.AdapterService;
import com.hybris.datahub.api.publication.PublicationException;
import com.hybris.datahub.domain.TargetItemMetadata;
import com.hybris.datahub.dto.item.ErrorData;
import com.hybris.datahub.dto.publication.PublicationResult;
import com.hybris.datahub.model.TargetItem;
import com.hybris.datahub.paging.DataHubPage;
import com.hybris.datahub.paging.DataHubPageable;
import com.hybris.datahub.paging.DefaultDataHubPageRequest;
import com.hybris.datahub.runtime.domain.TargetSystemPublication;
import com.hybris.datahub.service.PublicationActionService;
import org.apache.commons.collections.CollectionUtils;
import org.robbins.domain.RaspberryPiCommandTargetItem;
import org.robbins.raspberry.pi.client.StatusClient;
import org.robbins.raspberry.pi.model.PiAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.List;

public class DefaultRaspberryPiTargetSystemAdapter implements AdapterService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRaspberryPiTargetSystemAdapter.class);
    private static final String TARGET_SYSTEM_TYPE = "RaspberryPiTargetSystemAdapter";
    private static final int PAGE_SIZE = 10;
    private static final String ACTION_NAME = "name";
    private static final String ACTION_VALUE = "value";

    private PublicationActionService publicationActionService;
    private StatusClient statusClient;

    @Override
    public String getTargetSystemType() {
        return TARGET_SYSTEM_TYPE;
    }

    @Override
    public void publish(final TargetSystemPublication targetSystemPublication, final String s) throws PublicationException {
        List<ErrorData> errors = publishTargetItems(targetSystemPublication);
        completePublication(targetSystemPublication, errors);
    }

    private List<ErrorData> publishTargetItems(final TargetSystemPublication targetSystemPublication)
    {
        final List<ErrorData> errors = new ArrayList<>();
        for (TargetItemMetadata itemMetadata : targetSystemPublication.getTargetSystem().getTargetItemMetadata())
        {
            final Class<? extends TargetItem> targetItemType = TargetItem.getItemClass(itemMetadata.getItemType());
            int pageNumber = 0;
            List<? extends TargetItem> items;
            do
            {
                final DataHubPageable pageable = new DefaultDataHubPageRequest(pageNumber, PAGE_SIZE);
                DataHubPage<? extends TargetItem> page = publicationActionService.findByPublication(targetSystemPublication.getPublicationId(), targetItemType, pageable);
                items = page.getContent();
                for (final TargetItem targetItem : items)
                {
                    sendTargetItem(targetItem, errors);
                }
                pageNumber ++;
            } while(CollectionUtils.isNotEmpty(items));
        }
        return errors;
    }

    private void sendTargetItem(final TargetItem targetItem, final List<ErrorData> errors)
    {
        try {
            if (targetItem instanceof RaspberryPiCommandTargetItem)
            {
                PiAction piAction = createPiActionFromTargetItem((RaspberryPiCommandTargetItem)targetItem);

                statusClient.getStatus();
            }
        }
        catch (Exception e) {
            logger.error("Failed to publish target item: " + targetItem.toString());
            errors.add(buildPublicationError(targetItem, e));
        }
    }

    private PiAction createPiActionFromTargetItem(final RaspberryPiCommandTargetItem targetItem)
    {
        PiAction piAction = new PiAction();
        piAction.setName((String)targetItem.getField(ACTION_NAME));
        piAction.setValue((String)targetItem.getField(ACTION_VALUE));
        logger.debug(piAction.toString());
        return piAction;
    }

    private ErrorData buildPublicationError(final TargetItem targetItem, final Exception e)
    {
        final ErrorData errorData = new ErrorData();
        errorData.setCanonicalItemId(targetItem.getCanonicalItem().getId());
        errorData.setCode("publication failure");
        errorData.setMessage(e.getMessage());
        return errorData;
    }

    private void completePublication(final TargetSystemPublication targetSystemPublication, final List<ErrorData> errors) {
        final PublicationResult publicationResult = new PublicationResult();
        publicationResult.setExportErrorDatas(errors);
        publicationActionService.completeTargetSystemPublication(targetSystemPublication.getPublicationId(), publicationResult);
    }

    @Required
    public void setPublicationActionService(PublicationActionService publicationActionService) {
        this.publicationActionService = publicationActionService;
    }

    @Required
    public void setStatusClient(StatusClient statusClient) {
        this.statusClient = statusClient;
    }
}
