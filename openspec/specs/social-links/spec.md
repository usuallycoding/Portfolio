# social-links Specification

## Purpose

Manages the owner's social links as an ordered list with per-link visibility and a 1:1 icon, exposing only public links to visitors.

## Requirements

### Requirement: Social link fields
A social link SHALL have a name, an address, an isPublic flag and an optional 1:1 icon image.

#### Scenario: Create
- **WHEN** the owner creates a link with name and address
- **THEN** the link exists, is placed last in order, and is private unless isPublic is set

### Requirement: Manual ordering
The owner SHALL be able to reorder links, and every read SHALL return links in the owner's order.

#### Scenario: Reorder
- **WHEN** the owner submits a new order of link ids
- **THEN** subsequent reads return links in that order

#### Scenario: Incomplete order
- **WHEN** the submitted order omits or repeats an existing link
- **THEN** the server rejects it and keeps the previous order

### Requirement: Public reads exclude private links
The public API SHALL return only links with isPublic true, preserving relative order. The admin API SHALL return all links.

#### Scenario: Private link hidden
- **WHEN** an anonymous client lists links and one is private
- **THEN** the private link is absent

### Requirement: Owner edits and deletes links
The owner SHALL be able to update any field of a link and delete a link.

#### Scenario: Delete
- **WHEN** the owner deletes a link
- **THEN** it no longer appears in any read and its icon becomes eligible for orphan cleanup

### Requirement: Icon is 1:1
A link icon SHALL have a 1:1 aspect ratio, within server tolerance.

#### Scenario: Wrong ratio
- **WHEN** the owner sets an icon whose ratio is outside tolerance
- **THEN** the server rejects the request and keeps the previous icon
