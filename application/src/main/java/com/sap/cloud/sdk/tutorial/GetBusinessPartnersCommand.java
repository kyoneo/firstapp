package com.sap.cloud.sdk.tutorial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceConfiguration;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceDecorator;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceIsolationMode;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceRuntimeException;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataException;
import com.sap.cloud.sdk.s4hana.datamodel.odata.namespaces.businesspartner.BusinessPartner;
import com.sap.cloud.sdk.s4hana.datamodel.odata.namespaces.businesspartner.field.BusinessPartnerField;
import com.sap.cloud.sdk.s4hana.datamodel.odata.services.BusinessPartnerService;
import com.sap.cloud.sdk.s4hana.datamodel.odata.services.DefaultBusinessPartnerService;



public class GetBusinessPartnersCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetBusinessPartnersCommand.class);
    private static final String CATEGORY_PERSON = "1";

    private final BusinessPartnerService businessPartnerService;
    private final ResilienceConfiguration myResilienceConfig;
    private final HttpDestination httpDestination;

    public GetBusinessPartnersCommand(HttpDestination destination) {
        this(destination, new DefaultBusinessPartnerService());
    }

    public GetBusinessPartnersCommand(HttpDestination httpDestination, BusinessPartnerService businessPartnerService) {
        this.businessPartnerService = businessPartnerService;
        this.httpDestination = httpDestination;

        myResilienceConfig = ResilienceConfiguration.of(BusinessPartnerService.class)
                    .isolationMode(ResilienceIsolationMode.TENANT_AND_USER_OPTIONAL)
                    .timeLimiterConfiguration(
                            ResilienceConfiguration.TimeLimiterConfiguration.of()
                                    .timeoutDuration(Duration.ofMillis(10000)))
                    .bulkheadConfiguration(
                            ResilienceConfiguration.BulkheadConfiguration.of()
                                    .maxConcurrentCalls(20));

        final ResilienceConfiguration.CacheConfiguration cacheConfig =
            ResilienceConfiguration.CacheConfiguration
                .of(Duration.ofSeconds(10))
                .withoutParameters();

        myResilienceConfig.cacheConfiguration(cacheConfig);
    }

    public List<BusinessPartner> execute() {
        return ResilienceDecorator.executeSupplier(this::run, myResilienceConfig, e -> {
            logger.warn("Fallback called because of exception.", e);
            return Collections.emptyList();
        });
    }

    private List<BusinessPartner> run() {
        try {
            return businessPartnerService.getAllBusinessPartner()
                    .filter(BusinessPartner.BUSINESS_PARTNER_CATEGORY.eq(CATEGORY_PERSON))
                    .select(BusinessPartner.FIRST_NAME,
                            BusinessPartner.LAST_NAME)
                    .executeRequest(httpDestination);

        } catch (ODataException e) {
            throw new ResilienceRuntimeException(e);
        }
    }
}