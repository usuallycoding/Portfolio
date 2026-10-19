# app-theme Specification

## Purpose

Gives every app a light and a dark appearance that follows the device setting by default, with a choice the user can override and that is remembered on that device.

## Requirements

### Requirement: Light and dark themes
Every client and admin app SHALL provide a light theme and a dark theme that cover all screens, dialogs and controls, with readable contrast for text and controls in both.

#### Scenario: Dark theme applied everywhere
- **WHEN** the dark theme is active
- **THEN** every screen and dialog uses dark surfaces with light text, with no white backgrounds left over

#### Scenario: Light theme applied everywhere
- **WHEN** the light theme is active
- **THEN** every screen and dialog uses light surfaces with dark text

### Requirement: Follow the device by default
On first launch, and whenever the choice is System, the app SHALL use the device's light or dark setting and SHALL switch when the device setting changes while the app is open.

#### Scenario: First launch
- **WHEN** the app is launched for the first time on a device set to dark
- **THEN** the dark theme is shown and the choice is System

#### Scenario: Device setting changes
- **WHEN** the choice is System and the device switches from light to dark while the app is open
- **THEN** the app switches to the dark theme without restarting

### Requirement: User theme choice
Every app SHALL let the user choose System, Light or Dark, and SHALL apply the choice immediately.

#### Scenario: Override the device
- **WHEN** the device is set to dark and the user chooses Light
- **THEN** the app shows the light theme immediately

#### Scenario: Back to System
- **WHEN** the user chooses System again
- **THEN** the app follows the device setting again

### Requirement: Remembered choice
The app SHALL remember the user's choice on that device between launches, and SHALL fall back to System if the stored choice is missing or unreadable.

#### Scenario: Restart
- **WHEN** the user chooses Dark and later reopens the app
- **THEN** the dark theme is shown

#### Scenario: Storage unavailable
- **WHEN** the choice cannot be read or written
- **THEN** the app still works and follows the device setting
