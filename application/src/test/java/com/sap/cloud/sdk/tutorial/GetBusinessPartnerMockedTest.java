package com.sap.cloud.sdk.tutorial;

import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.cloudplatform.resilience.ResilienceRuntimeException;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataException;

import com.google.common.collect.Lists;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import com.sap.cloud.sdk.s4hana.datamodel.odata.namespaces.businesspartner.BusinessPartner;
import com.sap.cloud.sdk.s4hana.datamodel.odata.namespaces.businesspartner.selectable.BusinessPartnerSelectable;
import com.sap.cloud.sdk.s4hana.datamodel.odata.services.BusinessPartnerService;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class GetBusinessPartnerMockedTest {
    private static BusinessPartner alice;
    private static BusinessPartner bob;

    @BeforeClass
    public static void beforeClass() throws Exception {
        alice = new BusinessPartner();
        alice.setFirstName("Alice");

        bob = new BusinessPartner();
        bob.setFirstName("Bob");
    }

    @Test
    public void testGetAnyBusinessPartner() throws Exception {
        final BusinessPartnerService service = Mockito.mock(BusinessPartnerService.class, RETURNS_DEEP_STUBS);
        final HttpDestination httpDestination = Mockito.mock(HttpDestination.class);

        when(service.getAllBusinessPartner()
                .filter(any())
                .select(any())
                .executeRequest(any()))
                .thenReturn(Lists.newArrayList(alice, bob));

        final List<BusinessPartner> businessPartnerList = new GetBusinessPartnersCommand(httpDestination, service).execute();

        assertEquals(2, businessPartnerList.size());
        assertEquals("Alice", businessPartnerList.get(0).getFirstName());
        assertEquals("Bob", businessPartnerList.get(1).getFirstName());
    }

    @Test
    public void testGetSpecificBusinessPartner() throws Exception {
        final BusinessPartnerService service = Mockito.mock(BusinessPartnerService.class, RETURNS_DEEP_STUBS);
        final HttpDestination httpDestination = Mockito.mock(HttpDestination.class);
        when(service.getAllBusinessPartner()
                .filter(BusinessPartner.CUSTOMER.ne(""))
                .select(any(BusinessPartnerSelectable.class))
                .executeRequest(any(HttpDestination.class)))
                .thenReturn(Lists.newArrayList(alice));

        final List<BusinessPartner> businessPartnerList = new GetBusinessPartnersCommand(httpDestination, service).execute();

        assertEquals(1, businessPartnerList.size());
        assertEquals("Alice", businessPartnerList.get(0).getFirstName());
    }

    @Test(expected = ResilienceRuntimeException.class)
    public void testGetBusinessPartnerFailure() throws Exception {
        final BusinessPartnerService service = Mockito.mock(BusinessPartnerService.class, RETURNS_DEEP_STUBS);
        final HttpDestination httpDestination = Mockito.mock(HttpDestination.class);

        when(service.getAllBusinessPartner()
                .filter(any())
                .select(any())
                .executeRequest(any()))
                .thenThrow(new ODataException(null, "Something went wrong", null));

        new GetBusinessPartnersCommand(httpDestination, service).execute();
    }
}