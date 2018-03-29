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
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import com.adyen.constants.ErrorTypeCodes;
import com.adyen.model.marketpay.ErrorFieldType;
import com.adyen.model.marketpay.FieldType;
import com.mirakl.client.mmp.domain.shop.MiraklContactInformation;
import com.mirakl.client.mmp.domain.shop.MiraklShop;

@RunWith(MockitoJUnitRunner.class)
public class InvalidFieldsNotificationServiceTest {

    @Mock
    private MailTemplateService mailTemplateServiceMock;

    @Captor
    private ArgumentCaptor<MiraklShop> miraklShopOperatorArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<String>> errorsOperatorArgumentCaptor;

    @Captor
    private ArgumentCaptor<MiraklShop> miraklShopSellerArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<String>> errorsSellerArgumentCaptor;

    private InvalidFieldsNotificationService invalidFieldsNotificationService;

    @Before
    public void setUp() {
        invalidFieldsNotificationService = new InvalidFieldsNotificationService();
        invalidFieldsNotificationService.setMailTemplateService(mailTemplateServiceMock);

        Mockito.doNothing().when(mailTemplateServiceMock).sendSellerEmailWithErrors(miraklShopSellerArgumentCaptor.capture(), errorsSellerArgumentCaptor.capture());
        Mockito.doNothing().when(mailTemplateServiceMock).sendOperatorEmailWithErrors(miraklShopOperatorArgumentCaptor.capture(), errorsOperatorArgumentCaptor.capture());
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
        error1.setErrorCode(ErrorTypeCodes.EMAIL_INVALID);
        error1.setErrorDescription("Email address invalidemailhere is invalid");
        invalidFields.add(error1);
        invalidFieldsNotificationService.handleErrorsInResponse(miraklShop, invalidFields);

        Mockito.verify(mailTemplateServiceMock).sendSellerEmailWithErrors(miraklShopSellerArgumentCaptor.getValue(), errorsSellerArgumentCaptor.getValue());

        List<String> errors = errorsSellerArgumentCaptor.getValue();
        Assertions.assertThat(errors).hasSize(1);
        Assertions.assertThat(errors).contains("Email address invalidemailhere is invalid");

        Assertions.assertThat(miraklShopSellerArgumentCaptor.getValue()).isEqualTo(miraklShop);
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
        error1.setErrorCode(ErrorTypeCodes.FIELD_MISSING);
        error1.setErrorDescription("Field is missing");
        FieldType fieldType = new FieldType();
        error1.setFieldType(fieldType);
        fieldType.setField("AccountHolderDetails.BankAccountDetails.accountNumber");

        ErrorFieldType error2 = new ErrorFieldType();
        invalidFields.add(error2);
        error2.setErrorCode(ErrorTypeCodes.EMAIL_INVALID);
        error2.setErrorDescription("Email address invalidemailhere is invalid");

        invalidFieldsNotificationService.handleErrorsInResponse(miraklShop, invalidFields);

        Mockito.verify(mailTemplateServiceMock).sendSellerEmailWithErrors(miraklShopSellerArgumentCaptor.getValue(), errorsSellerArgumentCaptor.getValue());
        Mockito.verify(mailTemplateServiceMock).sendOperatorEmailWithErrors(miraklShopOperatorArgumentCaptor.getValue(), errorsOperatorArgumentCaptor.getValue());

        List<String> errorsSeller = errorsSellerArgumentCaptor.getValue();
        Assertions.assertThat(errorsSeller).hasSize(1);
        Assertions.assertThat(errorsSeller).contains("Email address invalidemailhere is invalid");

        List<String> errorsOperator = errorsOperatorArgumentCaptor.getValue();
        Assertions.assertThat(errorsOperator).hasSize(1);
        Assertions.assertThat(errorsOperator).contains("Field is missing: AccountHolderDetails.BankAccountDetails.accountNumber");

        Assertions.assertThat(miraklShopSellerArgumentCaptor.getValue()).isEqualTo(miraklShop);
        Assertions.assertThat(miraklShopOperatorArgumentCaptor.getValue()).isEqualTo(miraklShop);
    }
}
