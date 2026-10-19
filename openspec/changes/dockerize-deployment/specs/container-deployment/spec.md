# Spec Delta

## Purpose

Lets the owner run the server and both web apps as containers from one compose file, configured only by environment variables, with the public site unable to reach owner-only endpoints.

## ADDED Requirements

### Requirement: Web apps use their own origin
The client and admin web apps SHALL send their requests to the origin that served them, so one built copy works at any address. When served by the development server on port 8080, they SHALL use `http://localhost:8090`.

#### Scenario: Deployed address
- **WHEN** a web app is served from `https://example.com` or `http://192.168.1.20:8081`
- **THEN** its requests go to that same origin, with no cross-origin configuration

#### Scenario: Development server
- **WHEN** a web app is served by `./kotlin run` on `localhost:8080`
- **THEN** its requests go to `http://localhost:8090`

### Requirement: Public client cannot reach owner endpoints
The client container SHALL forward only the public read API and image paths to the server and SHALL answer every `/admin` path with 404. The admin container SHALL forward the full API.

#### Scenario: Public read
- **WHEN** a visitor loads the client site
- **THEN** the profile, links, projects and images load through the client container

#### Scenario: Admin path through the client
- **WHEN** a request for `/admin/login` or any other `/admin` path reaches the client container
- **THEN** it receives 404 and never reaches the server

#### Scenario: Admin through the admin container
- **WHEN** the owner logs in through the admin container
- **THEN** login and editing work as they do when run from source

### Requirement: Server is not directly exposed
The compose file SHALL NOT publish the server's port to the host. The server SHALL be reachable only from the other containers in the stack.

#### Scenario: No host port
- **WHEN** the stack is running
- **THEN** nothing on the host or LAN can connect to the server except through the client or admin container

### Requirement: Environment-file configuration
The compose file SHALL read the owner password, the host port of the client, the host port of the admin, an optional bind address for the admin, the image tag and the server's JVM memory cap from environment variables, and an `.env.example` SHALL list them. Starting without the owner password SHALL fail with an error naming the variable.

#### Scenario: Custom ports
- **WHEN** `CLIENT_PORT` and `ADMIN_PORT` are set to other values
- **THEN** the client and the admin are reachable on those host ports and nowhere else

#### Scenario: Missing password
- **WHEN** the stack is started without the owner password
- **THEN** it does not start and the error names the missing variable

#### Scenario: LAN-only admin
- **WHEN** the admin bind address is set to the host's LAN address
- **THEN** the admin port is published only on that address

### Requirement: Data persists across restarts
The server's database and uploaded images SHALL be stored in a named volume, so recreating or updating the containers keeps the profile, links, projects and images.

#### Scenario: Container recreated
- **WHEN** the stack is brought down and up again, or the images are updated
- **THEN** all previously saved content is still present

### Requirement: Self-contained stack definition
The compose file SHALL need no files from the repository other than itself and its environment values, so it can be pasted into Portainer as a stack. It SHALL NOT include the Cloudflare tunnel, which runs as a separate stack and reaches the client through its published host port.

#### Scenario: Portainer stack
- **WHEN** the compose file and the environment values are pasted into a Portainer stack on a host that can pull the images
- **THEN** the stack starts without any bind-mounted configuration

#### Scenario: Existing tunnel
- **WHEN** a tunnel in another stack targets the client's host address and port
- **THEN** the public site is served through it

### Requirement: Published multi-architecture images
A GitHub Actions workflow SHALL build the server, client and admin images for linux/amd64 and linux/arm64 and push them to GitHub Container Registry, so a Raspberry Pi and an x86 machine can pull the same tags.

#### Scenario: Release build
- **WHEN** the workflow runs on a release tag
- **THEN** images for both architectures are published under that tag and `latest`

#### Scenario: Raspberry Pi
- **WHEN** the stack is started on an arm64 host
- **THEN** the arm64 images are pulled and run
