# owner-auth Specification

## Purpose

Protects all editing operations behind a single-owner password login, so only the portfolio owner can change content while anyone can view public content.

## Requirements

### Requirement: Owner login
The system SHALL let the owner log in with a password and receive a bearer token. There SHALL be exactly one owner and no user registration.

#### Scenario: Correct password
- **WHEN** a client posts the correct password to the login endpoint
- **THEN** the server responds with a token valid for admin requests

#### Scenario: Wrong password
- **WHEN** a client posts an incorrect password
- **THEN** the server responds 401 and issues no token

### Requirement: Admin routes require a token
Every route that creates, updates, deletes, reorders or uploads content, and every route that returns non-public content, SHALL require a valid token.

#### Scenario: Missing token
- **WHEN** a request to an admin route has no token
- **THEN** the server responds 401 and changes nothing

#### Scenario: Invalid or expired token
- **WHEN** a request to an admin route carries an unknown or expired token
- **THEN** the server responds 401 and changes nothing

### Requirement: Public routes need no token
Read-only routes that return public portfolio content and public image files SHALL NOT require a token.

#### Scenario: Anonymous read
- **WHEN** a client without a token requests the public profile
- **THEN** the server returns it

### Requirement: Login attempt throttling
The system SHALL throttle repeated failed login attempts.

#### Scenario: Repeated failures
- **WHEN** many consecutive login attempts fail in a short period
- **THEN** further attempts are rejected with 429 until the throttle window passes

### Requirement: Password is configured outside the data store
The owner password SHALL be supplied through server configuration and SHALL NOT be stored in clear text or exposed through any API.

#### Scenario: Server start without a password
- **WHEN** the server starts with no owner password configured
- **THEN** it refuses to start with a clear error
