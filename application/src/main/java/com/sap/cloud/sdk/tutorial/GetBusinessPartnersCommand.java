package com.sap.cloud.sdk.tutorial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceConfiguration;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceDecorator;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceIsolationMode;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceRuntimeException;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataException;
import com.sap.cloud.sdk.datamodel.odata.helper.Order;

import com.sap.cloud.sdk.s4hana.connectivity.ErpHttpDestination;
import com.sap.cloud.sdk.s4hana.datamodel.odata.namespaces.businesspartner.AddressEmailAddress;
import com.sap.cloud.sdk.s4hana.datamodel.odata.namespaces.businesspartner.BusinessPartner;
import com.sap.cloud.sdk.s4hana.datamodel.odata.namespaces.businesspartner.BusinessPartnerAddress;
import com.sap.cloud.sdk.s4hana.datamodel.odata.services.BusinessPartnerService;
import com.sap.cloud.sdk.s4hana.datamodel.odata.services.DefaultBusinessPartnerService;

public class GetBusinessPartnersCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetBusinessPartnersCommand.class);
    // TODO: uncomment the lines below and insert your API key, if you are using the sandbox service
    // private static final String APIKEY_HEADER = "apikey";
    // private static final String SANDBOX_APIKEY = "";
    private static final String CATEGORY_PERSON = "1";
    private final ErpHttpDestination destination;

    private final BusinessPartnerService businessPartnerService;
    private final ResilienceConfiguration myResilienceConfig;

    public GetBusinessPartnersCommand(ErpHttpDestination destination) {
        this(destination, new DefaultBusinessPartnerService());
    }

    public GetBusinessPartnersCommand(ErpHttpDestination destination, BusinessPartnerService service) {
        this.destination = destination;
        businessPartnerService = service;

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
            return businessPartnerService
                    .getAllBusinessPartner()
                    .select(BusinessPartner.BUSINESS_PARTNER,
                            BusinessPartner.LAST_NAME,
                            BusinessPartner.FIRST_NAME,
                            BusinessPartner.IS_MALE,
                            BusinessPartner.IS_FEMALE,
                            BusinessPartner.CREATION_DATE,
                            BusinessPartner.TO_BUSINESS_PARTNER_ADDRESS
                                    .select(BusinessPartnerAddress.CITY_NAME,
                                            BusinessPartnerAddress.COUNTRY,
                                            BusinessPartnerAddress.TO_EMAIL_ADDRESS
                                                    .select(AddressEmailAddress.EMAIL_ADDRESS)
                                    )
                    )
                    .filter(BusinessPartner.BUSINESS_PARTNER_CATEGORY.eq(CATEGORY_PERSON))
                    .orderBy(BusinessPartner.LAST_NAME, Order.ASC)
                    .top(200)
                    // TODO: uncomment the line below, if you are using the sandbox service
                    // .withHeader(APIKEY_HEADER, SANDBOX_APIKEY)
                    .executeRequest(destination);
        } catch (ODataException e) {
            throw new ResilienceRuntimeException(e);
        }
    }
}