Feature: As an Operator I would like to be notified when a Seller Payout Status is changed so I can take the necessary action

    @ADY-25
    Scenario: Email is sent to seller upon PayoutState set to true
        Given a shop has been created in Mirakl for an Individual with mandatory KYC data
            | city   | bank name | iban                   | bankOwnerName | lastName |
            | PASSED | testBank  | GB26TEST40051512347366 | TestData      | TestData |
        And the connector processes the data and pushes to Adyen
        And the accountHolder receives balance
            | transfer amount |
            | 1000            |
        And a notification will be sent in relation to the payout state change
            | eventType                    | oldPayoutState | newPayoutState |
            | ACCOUNT_HOLDER_STATUS_CHANGE | false          | true           |
        And the notification is sent to the Connector
        Then a payout email will be sent to the seller
        """
        Payout for your account is now allowed
        """
        When the accountHolders balance is increased beyond the tier level
            | transfer amount |
            | 60000           |
        And a notification will be sent in relation to the payout state change
            | eventType                    | oldPayoutState | newPayoutState |
            | ACCOUNT_HOLDER_STATUS_CHANGE | true           | false          |
        And the notification is sent to the Connector
        Then a payout email will be sent to the seller
        """
        Payout for your account is no longer allowed
        """
