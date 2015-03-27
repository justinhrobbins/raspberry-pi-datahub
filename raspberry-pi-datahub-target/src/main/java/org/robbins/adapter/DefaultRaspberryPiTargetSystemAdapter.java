package org.robbins.adapter;

import com.hybris.datahub.adapter.AdapterService;
import com.hybris.datahub.api.publication.PublicationException;
import com.hybris.datahub.runtime.domain.TargetSystemPublication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultRaspberryPiTargetSystemAdapter implements AdapterService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRaspberryPiTargetSystemAdapter.class);
    private static final String TARGET_SYSTEM_TYPE = "RaspberryPiTargetSystemAdapter";

    @Override
    public String getTargetSystemType() {
        return TARGET_SYSTEM_TYPE;
    }

    @Override
    public void publish(final TargetSystemPublication targetSystemPublication, final String s) throws PublicationException {
        logger.info(targetSystemPublication.toString());
    }
}
