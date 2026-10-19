# Spec Delta

## Purpose

Defines the owner's public profile: name, description, a 1:1 profile picture and a 3:1 cover image, always visible with placeholders until filled in.

## ADDED Requirements

### Requirement: Profile is a single always-public record
The system SHALL hold exactly one profile with a name, a description, a profile picture and a cover image. The profile SHALL always be publicly readable.

#### Scenario: Fresh install
- **WHEN** a client requests the profile before the owner has edited anything
- **THEN** the server returns placeholder values for name and description and no images, with success status

#### Scenario: Public read
- **WHEN** an anonymous client requests the profile
- **THEN** the server returns the current values

### Requirement: Owner edits profile text
The system SHALL let the owner update the name and description.

#### Scenario: Update
- **WHEN** the owner saves a new name and description
- **THEN** later reads return the new values

### Requirement: Profile picture is 1:1
The profile picture SHALL be an uploaded image with a 1:1 aspect ratio, within server tolerance.

#### Scenario: Valid picture
- **WHEN** the owner sets the profile picture to a staged 1:1 image
- **THEN** the profile references it and it is served publicly

#### Scenario: Wrong ratio
- **WHEN** the owner sets the profile picture to an image whose ratio is outside tolerance
- **THEN** the server rejects the request and leaves the profile unchanged

### Requirement: Cover image is 3:1
The cover image SHALL be an uploaded image with a 3:1 aspect ratio, within server tolerance.

#### Scenario: Valid cover
- **WHEN** the owner sets the cover to a staged 3:1 image
- **THEN** the profile references it

#### Scenario: Wrong ratio
- **WHEN** the owner sets the cover to an image outside tolerance
- **THEN** the server rejects the request and leaves the profile unchanged

### Requirement: Replacing or removing an image orphans the old one
Replacing or clearing a profile image SHALL detach the previous image so that it becomes eligible for orphan cleanup.

#### Scenario: Replace
- **WHEN** the owner replaces the profile picture
- **THEN** the old image is no longer referenced by the profile
