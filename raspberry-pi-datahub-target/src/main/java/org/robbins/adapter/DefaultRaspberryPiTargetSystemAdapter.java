package org.robbins.adapter;

import com.hybris.datahub.adapter.AdapterService;
import com.hybris.datahub.api.publication.PublicationException;
import com.hybris.datahub.dto.item.ErrorData;
import com.hybris.datahub.dto.publication.PublicationResult;
import com.hybris.datahub.model.TargetItem;
import com.hybris.datahub.paging.DataHubPage;
import com.hybris.datahub.paging.DataHubPageable;
import com.hybris.datahub.paging.DefaultDataHubPageRequest;
import com.hybris.datahub.runtime.domain.PublicationType;
import com.hybris.datahub.runtime.domain.TargetSystemPublication;
import com.hybris.datahub.service.PublicationActionService;
import org.apache.commons.collections.CollectionUtils;
import org.robbins.domain.PiScheduleTargetItem;
import org.robbins.raspberry.pi.client.PiScheduleClient;
import org.robbins.raspberry.pi.model.PiSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.List;

public class DefaultRaspberryPiTargetSystemAdapter implements AdapterService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRaspberryPiTargetSystemAdapter.class);
    private static final String TARGET_SYSTEM_TYPE = "RaspberryPiTargetSystemAdapter";
    private static final int PAGE_SIZE = 10;
    private static final String SCHEDULE_NAME = "scheduleName";
    private static final String ACTION_NAME = "actionName";
    private static final String ACTION_VALUE = "actionValue";
    private static final String CRON_TRIGGER = "cronTrigger";

    private PublicationActionService publicationActionService;
    private PiScheduleClient piScheduleClient;

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
        logger.info("Publishing {}", targetSystemPublication);

        final List<ErrorData> errors = new ArrayList<>();
        targetSystemPublication.getTargetSystem().getTargetItemMetadata().stream()
                .forEach(itemMetadata -> {
                    final Class<? extends TargetItem> targetItemType = TargetItem.getItemClass(itemMetadata.getItemType());
                    int pageNumber = 0;
                    List<? extends TargetItem> items;
                    do
                    {
                        final DataHubPageable pageable = new DefaultDataHubPageRequest(pageNumber, PAGE_SIZE);
                        DataHubPage<? extends TargetItem> page = publicationActionService.findByPublication(targetSystemPublication.getPublicationId(), targetItemType, pageable);
                        items = page.getContent();
                        items.forEach(targetItem -> sendTargetItem(targetItem, errors));
                        pageNumber ++;
                    } while(CollectionUtils.isNotEmpty(items));
                });
        return errors;
    }

    private void sendTargetItem(final TargetItem targetItem, final List<ErrorData> errors)
    {
        try {
            if (targetItem instanceof PiScheduleTargetItem)
            {
                PiScheduleTargetItem scheduleTargetItem = (PiScheduleTargetItem)targetItem;

                PublicationType publicationType = targetItem.getTargetSystemPublication().getPublicationType();
                if (publicationType == PublicationType.INSERT) {
                    createSchedule(scheduleTargetItem);
                }
                else if (publicationType == PublicationType.DELETE) {
                    deleteSchedule(scheduleTargetItem);
                }
                else {
                    throw new IllegalArgumentException("Unrecognized PublicationType: " + publicationType);
                }
            }
        }
        catch (Exception e) {
            logger.error("Failed to publish target item: " + targetItem.toString());
            errors.add(buildPublicationError(targetItem, e));
        }
    }

    private boolean isSuccessfulBuild(PiScheduleTargetItem targetItem) {
        return targetItem.getField("buildStatus").equals("SUCCESS");
    }

    private void createSchedule(final PiScheduleTargetItem targetItem) {
        PiSchedule piSchedule = createScheduleFromTargetItem(targetItem);
        piScheduleClient.createSchedule(piSchedule);
    }

    private void deleteSchedule(final PiScheduleTargetItem targetItem) {
        PiSchedule piSchedule = createScheduleFromTargetItem(targetItem);
        piScheduleClient.deleteSchedule(piSchedule.getScheduleName());
    }

    private PiSchedule createScheduleFromTargetItem(final PiScheduleTargetItem targetItem)
    {
        PiSchedule piSchedule = new PiSchedule();
        piSchedule.setScheduleName(targetItem.getField(SCHEDULE_NAME).toString());
        piSchedule.setActionName(targetItem.getField(ACTION_NAME).toString());
        piSchedule.setActionValue(targetItem.getField(ACTION_VALUE).toString());
        piSchedule.setTrigger(targetItem.getField(CRON_TRIGGER).toString());

        logger.debug(piSchedule.toString());
        return piSchedule;
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

    public void setPiScheduleClient(PiScheduleClient piScheduleClient) {
        this.piScheduleClient = piScheduleClient;
    }
}
