Feature: Payment creation
  As a Cashi user
  I want to send money to someone by their email address
  So that the payment reaches the right recipient

  Background:
    Given the user is signed in to Cashi

  Scenario: Sending a valid payment
    When the user sends "30" "USD" to "ahmad@gmail.com" with description "Money transfer"
    Then the payment is accepted
    And the backend received a payment of "30.00" "USD" for "ahmad@gmail.com"
    And the transaction history starts with a payment of "30.00" "USD" to "ahmad@gmail.com"

  Scenario: Sending a payment without a description
    When the user sends "12.5" "JOD" to "sara.k@example.com" with description ""
    Then the payment is accepted
    And the backend received a payment of "12.50" "JOD" for "sara.k@example.com"

  Scenario Outline: Invalid payments are rejected before anything is sent
    When the user sends "<amount>" "<currency>" to "<email>" with description "<description>"
    Then the payment is rejected with a "<field>" error "<message>"
    And no payment is sent to the backend

    Examples:
      | amount | currency | email           | description | field           | message                          |
      |        | USD      | ahmad@gmail.com |             | AMOUNT          | Amount is required               |
      | 0      | USD      | ahmad@gmail.com |             | AMOUNT          | Amount must be greater than zero |
      | -5     | JOD      | ahmad@gmail.com |             | AMOUNT          | Amount must be greater than zero |
      | 10.999 | USD      | ahmad@gmail.com |             | AMOUNT          | Up to 2 decimal places           |
      | ten    | USD      | ahmad@gmail.com |             | AMOUNT          | Enter a valid amount             |
      | 10     | GBP      | ahmad@gmail.com |             | CURRENCY        | Choose a supported currency      |
      | 10     | USD      |                 |             | RECIPIENT_EMAIL | Recipient email is required      |
      | 10     | USD      | ahmad@          |             | RECIPIENT_EMAIL | Enter a valid email address      |

  Scenario: The backend refuses the recipient
    Given the backend refuses payments to "blocked@example.com" with "Recipient cannot receive payments"
    When the user sends "5" "JOD" to "blocked@example.com" with description "Gift"
    Then the payment is rejected with a "RECIPIENT_EMAIL" error "Recipient cannot receive payments"
    And the transaction history is empty

  Scenario: An expired session is refreshed without bothering the user
    Given the access token has expired
    When the user sends "75" "USD" to "ahmad@gmail.com" with description "Rent share"
    Then the payment is accepted
    And the session was refreshed 1 time

  Scenario: The server cannot be reached
    Given the backend is unreachable
    When the user sends "30" "USD" to "ahmad@gmail.com" with description "Money transfer"
    Then the payment fails with a network error
