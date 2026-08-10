# Payment Gateway — Implementation Journey

## Introduction

This report describes the process I followed to complete the payment gateway take-home
exercise. It presents the work chronologically, from understanding the requirements and
defining the architecture to implementing, testing, and reviewing the final solution.

The purpose is to explain not only what I built, but also how the solution evolved and
why I made each decision along the way.

Reference: [Checkout.com Engineering Assessment](https://github.com/cko-recruitment/)

## Step 1 — Understand the Requirements

I started by reading the assessment and translating the requirements into a small set of
observable behaviors.

The gateway needed to:

- accept and validate a payment request;
- send valid payment details to the acquiring-bank simulator;
- return an `Authorized`, `Declined`, or `Rejected` result;
- store the result of a processed payment; and
- retrieve a previous payment without exposing its full card number.

At this stage, I also identified the main constraints: the solution should remain simple,
compile successfully, include automated tests, and focus on the requested functionality
without unnecessary production infrastructure.

### Outcome

I had a clear list of use cases, validation rules, external interactions, and expected
responses to guide the implementation.

## Step 2 — Establish a Basic Solution and Define the Architecture

Before implementing the complete behavior, I reviewed the supplied Spring Boot skeleton
and established a basic layered structure:

- the **controller** owns the HTTP contract;
- the **service** coordinates the payment workflow;
- the **repository** stores and retrieves processed payments into Postgres;
- the **bank client** communicates with the supplied simulator; and
- the **exception handler** converts known failures into consistent API responses.

I chose this structure because it keeps HTTP concerns, business flow, storage, and
external communication separate while remaining small enough for the scope of the
exercise.

The architectural decisions and component relationships are described in more detail in
[Architecture](architecture.md).

### Outcome

I had a minimal foundation on which each part of the payment flow could be implemented
and tested independently.

## Step 3 — Define the API Contract

With the main responsibilities separated, I defined the API operations and their request
and response models.

The intended operations were:

- `POST /payment` to process a new payment; and
- `GET /payment/{id}` to retrieve an existing payment.

I used UUIDs for payment identifiers and designed the responses to expose only the last
four card digits. I also considered which HTTP status codes should represent invalid
input, missing payments, and downstream failures.

### Outcome

The external behavior of the application was defined before the complete internal flow
was implemented.

## Step 4 — Implement Payment Retrieval First

I implemented the simpler retrieval path first. This connected the controller, service,
and in-memory repository and provided an initial end-to-end vertical slice of the
application.

The repository uses in-memory storage, which is explicitly allowed by the assessment.
The service retrieves a payment by UUID and reports a known error when the identifier is
not present. The shared exception handler then maps that error to an HTTP response.

### Outcome

The application could retrieve a stored payment and return a not-found response for an
unknown identifier. This also verified that the initial layering worked as expected.

## Step 5 — Implement Payment Processing

<!-- Complete this section while implementing the processing flow. Describe the order in
which validation, bank communication, response mapping, and persistence were added. -->

The next step was to implement the main payment-processing workflow:

1. receive the merchant's request;
2. validate the card, expiry date, currency, amount, and CVV;
3. reject invalid input before contacting the bank;
4. send valid requests to the bank simulator;
5. translate the bank response into the gateway's payment status; and
6. store and return the processed payment.

### Outcome

_To be completed once the processing flow is implemented._

## Step 6 — Add Validation and Failure Handling

<!-- Record the validation mechanism, supported currencies, error response format, and
how simulator timeouts or 503 responses are handled. -->

After establishing the happy path, I addressed invalid requests and failure scenarios.
The important distinction was between:

- **Rejected:** the gateway refused invalid merchant input without calling the bank;
- **Declined:** the bank processed the request but did not authorize the payment; and
- **Downstream failure:** the bank could not provide a valid decision.

I kept these outcomes separate so that API consumers could understand whether they
should correct the request, accept the bank's decision, or retry later.

### Outcome

_To be completed with the final error-handling behavior._

## Step 7 — Build the Automated Test Suite

I added tests incrementally as each behavior became available. The first controller
tests covered successful retrieval and retrieval of an unknown payment.

<!-- Extend this list as tests are added. -->

The final test suite should demonstrate:

- retrieval of an existing payment;
- retrieval of an unknown payment;
- authorized and declined payment processing;
- rejection of each invalid input category;
- the fact that rejected input never reaches the bank;
- handling of bank errors and timeouts; and
- masking of sensitive card data.

### Outcome

The tests provide executable evidence of the required behavior and protect the main
decisions made during implementation.

## Step 8 — Review Security-Sensitive Behavior

Because this project handles card information, I reviewed how sensitive values move
through the application. In particular, I checked that full card numbers and CVVs were
not returned, persisted unnecessarily, or written to logs.

For the scope of this exercise, responses and stored payment records should contain only
the last four card digits. In a production system, this would be supported by stronger
controls such as tokenization, encryption, access controls, and PCI DSS processes.

### Outcome

_To be completed after a final review of request models, persistence, and logging._

## Step 9 — Document and Demonstrate the Final Solution

Once the implementation and tests are complete, I will perform a final review against
the original requirements and prepare the following demonstration:

1. start the bank simulator with `docker-compose up`;
2. start the gateway with `./gradlew bootRun`;
3. run the automated tests with `./gradlew test`;
4. inspect the API through Swagger UI at
   [http://localhost:8090/swagger-ui/index.html](http://localhost:8090/swagger-ui/index.html);
5. process authorized, declined, and rejected payment examples; and
6. retrieve a processed payment by its identifier.

### Outcome

_To be completed with the final test result and demonstration notes._

