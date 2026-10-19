# admin-app Specification

## Purpose

Gives the owner desktop and web apps to log in and edit the profile, social links and projects, including cropping images and uploading many at once.

## Requirements

### Requirement: Admin platforms
The admin apps SHALL run on desktop (JVM) and web, with the same features.

#### Scenario: Same features
- **WHEN** the owner uses either admin app
- **THEN** every editing feature below is available

### Requirement: Login gate
The admin apps SHALL show a login screen first and SHALL show editing screens only after a successful login.

#### Scenario: Unauthenticated start
- **WHEN** the owner opens the app without a valid session
- **THEN** only the login screen is shown

#### Scenario: Expired token
- **WHEN** the server rejects a request with 401
- **THEN** the app returns to login without losing unsaved input where practical

### Requirement: Edit profile
The owner SHALL be able to edit name, description, profile picture and cover image.

#### Scenario: Save
- **WHEN** the owner edits fields and saves
- **THEN** the changes are sent to the server and the saved values are shown

### Requirement: Edit social links
The owner SHALL be able to create, edit, delete, set visibility, set an icon and reorder links.

#### Scenario: Reorder
- **WHEN** the owner moves a link and confirms
- **THEN** the new order is saved

### Requirement: Edit projects
The owner SHALL be able to create, edit, delete, set visibility, set a thumbnail and reorder projects, and manage a project's images and their order.

#### Scenario: Create project
- **WHEN** the owner fills in a project and saves
- **THEN** the project is created with the chosen thumbnail and image order

### Requirement: In-app cropping
Before uploading an image for a field with a fixed ratio, the app SHALL show a crop frame of that ratio (1:1 profile and icon, 3:1 cover, 16:9 thumbnail) with zoom and positioning controls, and SHALL upload only the cropped area.

#### Scenario: Crop
- **WHEN** the owner picks a photo for the cover
- **THEN** a 3:1 frame appears over it, the owner can zoom and move it, and confirming uploads only the framed area

#### Scenario: Cancel
- **WHEN** the owner cancels the crop
- **THEN** nothing is uploaded and the previous image is kept

### Requirement: Downscale before upload
The app SHALL downscale images larger than a maximum edge and encode them as JPEG or PNG before uploading.

#### Scenario: Large photo
- **WHEN** the owner picks a very large photo
- **THEN** the uploaded file is no larger than the maximum edge

### Requirement: Multi-image upload
The owner SHALL be able to select many project images at once. The app SHALL upload them one file per request with limited concurrency, show per-file progress, allow retrying only failed files, and let the owner reorder before saving.

#### Scenario: One file fails
- **WHEN** one of ten uploads fails
- **THEN** nine succeed, the failed one shows an error with retry, and the owner can continue

### Requirement: Unsaved staged uploads are harmless
Leaving a form without saving SHALL leave uploaded images unattached for cleanup.

#### Scenario: Abandon
- **WHEN** the owner closes the project form after uploading
- **THEN** the project is unchanged and the images are later cleaned as orphans
