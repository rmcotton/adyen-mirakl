@cucumber
Feature: As an Operator I would like to be notified when a Seller Payout Status is changed so I can take the necessary action

    @ADY-25
    Scenario: Email is sent to seller upon PayoutState set to true
        Given a shop has been created in Mirakl for an Individual with mandatory KYC data
            | city   | bank name | iban                   | bankOwnerName | lastName |
            | PASSED | testBank  | GB26TEST40051512347366 | TestData      | TestData |
        And we process the data and push to Adyen
        And the accountHolder receives balance
            | transfer amount |
            | 1000            |
        And the PayoutState allowPayout changes from false to true
        And a notification will be sent in relation to the balance change
            | eventType                    | reason              | previousPayoutState |
            | ACCOUNT_HOLDER_STATUS_CHANGE | Description: Passed | false               |
        And the notification is sent to the Connector
        Then an email will be sent to the seller
        """
        Payout for your account is now allowed
        """
        When the accountHolders balance is increased beyond the tier level
            | transfer amount |
            | 60000           |
        And the PayoutState allowPayout changes from true to false
        And a notification will be sent in relation to the balance change
            | eventType                    | reason                   | previousPayoutState |
            | ACCOUNT_HOLDER_STATUS_CHANGE | transfer has been booked | true                |
        And the notification is sent to the Connector
        Then an email will be sent to the seller
        """
        Payout for your account is no longer allowed
        """
