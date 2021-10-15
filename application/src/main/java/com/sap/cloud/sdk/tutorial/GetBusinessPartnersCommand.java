package com.sap.cloud.sdk.tutorial;

import java.util.List;

import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceConfiguration;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceDecorator;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceRuntimeException;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataException;

import com.sap.cloud.sdk.s4hana.datamodel.odata.namespaces.businesspartner.BusinessPartner;
import com.sap.cloud.sdk.s4hana.datamodel.odata.services.BusinessPartnerService;
import com.sap.cloud.sdk.s4hana.datamodel.odata.services.DefaultBusinessPartnerService;

public class GetBusinessPartnersCommand {

    private final BusinessPartnerService businessPartnerService;
    private final HttpDestination httpDestination;

    public GetBusinessPartnersCommand(HttpDestination destination) {
        this(destination, new DefaultBusinessPartnerService());
    }

    public GetBusinessPartnersCommand(HttpDestination httpDestination, BusinessPartnerService businessPartnerService) {
        this.businessPartnerService = businessPartnerService;
        this.httpDestination = httpDestination;
    }

    public List<BusinessPartner> execute() {
        return ResilienceDecorator.executeSupplier(this::run, ResilienceConfiguration.of(GetBusinessPartnersCommand.class));
    }

    private List<BusinessPartner> run() {

        try {

            return businessPartnerService.getAllBusinessPartner()
                    .filter(BusinessPartner.CUSTOMER.ne(""))
                    .select(BusinessPartner.FIRST_NAME,
                            BusinessPartner.LAST_NAME)
                    .executeRequest(httpDestination);

        } catch (ODataException e) {
            throw new ResilienceRuntimeException(e);
        }
    }
}