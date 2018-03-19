package com.adyen.mirakl.service;

import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.adyen.mirakl.config.MailTemplateService;
import com.adyen.model.marketpay.ErrorFieldType;
import com.adyen.model.marketpay.FieldType;
import com.mirakl.client.mmp.domain.shop.MiraklContactInformation;
import com.mirakl.client.mmp.domain.shop.MiraklShop;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class InvalidFieldsNotificationServiceTest {

    @Mock
    private MailTemplateService mailTemplateServiceMock;

    @Captor
    private ArgumentCaptor<MiraklShop> miraklShopArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<String>> errorsArgumentCaptor;

    private InvalidFieldsNotificationService invalidFieldsNotificationService;

    @Before
    public void setUp() {
        invalidFieldsNotificationService = new InvalidFieldsNotificationService();
        invalidFieldsNotificationService.setMailTemplateService(mailTemplateServiceMock);

        doNothing().when(mailTemplateServiceMock).sendGenericSellerEmail(miraklShopArgumentCaptor.capture());
        doNothing().when(mailTemplateServiceMock).sendSellerEmailWithErrors(miraklShopArgumentCaptor.capture(), errorsArgumentCaptor.capture());
        doNothing().when(mailTemplateServiceMock).sendOperatorEmailWithErrors(miraklShopArgumentCaptor.capture(), errorsArgumentCaptor.capture());
    }

    @Test
    public void testCreateAccountHolderResponse() {
        final MiraklShop miraklShop = new MiraklShop();
        final MiraklContactInformation miraklContactInformation = new MiraklContactInformation();
        miraklShop.setContactInformation(miraklContactInformation);

        miraklContactInformation.setCivility("Mr");
        miraklContactInformation.setFirstname("John");
        miraklContactInformation.setLastname("Doe");
        miraklContactInformation.setEmail("adyen-mirakl-cb966314-55c3-40e6-91f7-db6d8f0be825@mailinator.com");

        List<ErrorFieldType> invalidFields = new ArrayList<>();
        ErrorFieldType error1 = new ErrorFieldType();
        error1.setErrorCode(2);
        error1.setErrorDescription("Email address invalidemailhere is invalid");
        invalidFields.add(error1);
        invalidFieldsNotificationService.handleErrorsInResponse(miraklShop, invalidFields);

        verify(mailTemplateServiceMock).sendSellerEmailWithErrors(miraklShopArgumentCaptor.getValue(), errorsArgumentCaptor.getValue());

        List<String> errors = errorsArgumentCaptor.getValue();
        Assertions.assertThat(errors).hasSize(1);
        Assertions.assertThat(errors).contains("Email address invalidemailhere is invalid");
    }

    @Test
    public void testCreateAccountHolderResponseWithInternalErrors() {
        final MiraklShop miraklShop = new MiraklShop();
        final MiraklContactInformation miraklContactInformation = new MiraklContactInformation();
        miraklShop.setContactInformation(miraklContactInformation);

        miraklContactInformation.setCivility("Mr");
        miraklContactInformation.setFirstname("John");
        miraklContactInformation.setLastname("Doe");
        miraklContactInformation.setEmail("adyen-mirakl-cb966314-55c3-40e6-91f7-db6d8f0be825@mailinator.com");

        List<ErrorFieldType> invalidFields = new ArrayList<>();

        ErrorFieldType error1 = new ErrorFieldType();
        invalidFields.add(error1);
        error1.setErrorCode(1);
        error1.setErrorDescription("Field is missing");
        FieldType fieldType = new FieldType();
        error1.setFieldType(fieldType);
        fieldType.setField("AccountHolderDetails.BankAccountDetails.accountNumber");

        ErrorFieldType error2 = new ErrorFieldType();
        invalidFields.add(error2);
        error2.setErrorCode(2);
        error2.setErrorDescription("Email address invalidemailhere is invalid");

        invalidFieldsNotificationService.handleErrorsInResponse(miraklShop, invalidFields);

        verify(mailTemplateServiceMock).sendGenericSellerEmail(miraklShopArgumentCaptor.getValue());
        verify(mailTemplateServiceMock).sendOperatorEmailWithErrors(miraklShopArgumentCaptor.getValue(), errorsArgumentCaptor.getValue());

        List<String> errors = errorsArgumentCaptor.getValue();
        Assertions.assertThat(errors).hasSize(2);
        Assertions.assertThat(errors).contains("Field is missing: AccountHolderDetails.BankAccountDetails.accountNumber").contains("Email address invalidemailhere is invalid");
    }
}
