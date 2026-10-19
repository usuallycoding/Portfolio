# Spec Delta

## Purpose

Gives visitors read-only Android, iOS and web apps to view the profile, public social links and public projects with their images.

## ADDED Requirements

### Requirement: Client platforms and read-only behavior
The client apps SHALL run on Android, iOS and web, and SHALL NOT offer any editing or login.

#### Scenario: No editing
- **WHEN** a visitor uses a client app
- **THEN** no control creates, edits, deletes or uploads content

### Requirement: Profile screen
The client SHALL show the cover image, profile picture, name, description and public social links in the owner's order, with each link's icon and a way to open its address.

#### Scenario: Populated
- **WHEN** the profile has content
- **THEN** all of it is shown, with only public links

#### Scenario: Placeholders
- **WHEN** the profile is still placeholder
- **THEN** the screen renders without errors and shows the placeholder text and no images

### Requirement: Project list
The client SHALL list public projects in the owner's order, each with thumbnail, title and summary.

#### Scenario: List
- **WHEN** the visitor opens the projects section
- **THEN** public projects appear in owner order

#### Scenario: Empty
- **WHEN** there are no public projects
- **THEN** an empty state is shown

### Requirement: Project detail
Selecting a project SHALL open a detail screen with its title, date, summary and all images in the owner's order, and SHALL allow viewing an image at full size.

#### Scenario: Open
- **WHEN** the visitor selects a project
- **THEN** its images are shown in order

#### Scenario: Back
- **WHEN** the visitor navigates back
- **THEN** the project list is shown again

### Requirement: Efficient image loading
The client SHALL use thumbnail variants for grids, icons and avatars and originals for the cover and full-size viewing.

#### Scenario: Grid
- **WHEN** a list of projects is shown
- **THEN** thumbnail variants are requested

### Requirement: Failure handling
The client SHALL show a recoverable error state with a retry action when the server cannot be reached.

#### Scenario: Offline
- **WHEN** the server is unreachable
- **THEN** an error with retry is shown instead of a crash
