# projects Specification

## Purpose

Manages portfolio projects, each with metadata, a 16:9 thumbnail and manually ordered images, with visitors seeing only public projects.

## Requirements

### Requirement: Project fields
A project SHALL have a title, a creation date, an isPublic flag, a summary, a 16:9 thumbnail image and an ordered list of images.

#### Scenario: Create
- **WHEN** the owner creates a project with a title
- **THEN** the project exists, is placed last in project order, and is private unless isPublic is set

### Requirement: Manual project ordering
The owner SHALL be able to reorder projects and every read SHALL return projects in that order, regardless of creation date.

#### Scenario: Reorder
- **WHEN** the owner submits a new order of project ids
- **THEN** reads return projects in that order

#### Scenario: Incomplete order
- **WHEN** the submitted order omits or repeats a project
- **THEN** the server rejects it and keeps the previous order

### Requirement: Public reads exclude private projects
The public API SHALL list and return only projects with isPublic true. Requesting a private project through the public API SHALL behave as not found.

#### Scenario: Private project hidden
- **WHEN** an anonymous client requests a private project by id
- **THEN** the server responds 404

### Requirement: Manual image ordering
A project's images SHALL be ordered by the owner, and reads SHALL return them in that order.

#### Scenario: Attach in order
- **WHEN** the owner saves a project with staged image ids in a given order
- **THEN** reads return the images in that order

#### Scenario: Remove image
- **WHEN** the owner removes an image from a project
- **THEN** it no longer appears and becomes eligible for orphan cleanup

### Requirement: Thumbnail is 16:9
A project thumbnail SHALL have a 16:9 aspect ratio, within server tolerance.

#### Scenario: Wrong ratio
- **WHEN** the owner sets a thumbnail outside tolerance
- **THEN** the server rejects the request and keeps the previous thumbnail

### Requirement: Delete project
The owner SHALL be able to delete a project.

#### Scenario: Delete
- **WHEN** the owner deletes a project
- **THEN** it and its image references are removed and its images become eligible for orphan cleanup

### Requirement: Public project detail
The public API SHALL return a public project's metadata and ordered images.

#### Scenario: Detail
- **WHEN** an anonymous client requests a public project
- **THEN** the response includes title, date, summary, thumbnail and images in owner order
