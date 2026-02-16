# BusyBee

## Overview

BusyBee is a secure task-management web application built with **Java 21** and **Spring Boot**.
The application runs over **HTTPS (port 8443)** and includes authentication, authorization, secure input handling, secure file uploads, and persistent task storage.

---

## Prerequisites

To run the project on any machine, the following are required:

* **Java 21**
* Ability to set environment variables
* A valid TLS keystore configured for HTTPS

The project includes the **Gradle Wrapper**, so no separate Gradle installation is required.

---

## HTTPS / TLS Configuration

The application is configured to run over HTTPS on port **8443**.

Before running the application, the following environment variable must be defined:

* `SSL_KEYSTORE_PASSWORD` – the password used to load the TLS keystore

The keystore path itself is configured in `application.properties`.
The password is intentionally provided via an environment variable and not stored in source code.

---

## Optional Logging Configuration

A custom Java logging configuration can be loaded using the following environment variable:

* `JAVA_TOOL_OPTIONS`

Example:

```
-Djava.util.logging.config.file=<path_to_logging.properties>
```

This path is machine-specific and should be adjusted locally if used.

---

## How to Run

### Run from Terminal (Recommended)

From the project root directory:

**Windows (PowerShell):**

```powershell
$env:SSL_KEYSTORE_PASSWORD="your_keystore_password"
$env:JAVA_TOOL_OPTIONS='-Djava.util.logging.config.file=path\to\logging.properties'   # optional

.\gradlew.bat bootRun
```

**macOS / Linux:**

```bash
export SSL_KEYSTORE_PASSWORD="your_keystore_password"
export JAVA_TOOL_OPTIONS="-Djava.util.logging.config.file=path/to/logging.properties" # optional

./gradlew bootRun
```

---

### Run from an IDE (e.g., VS Code)

The application can also be run directly from an IDE.

1. Open the project in the IDE.
2. Locate the main class:

   * `com.securefromscratch.busybee.Application`
3. Run it as a **Spring Boot / Java Application**.

Required environment variables must still be defined.

---

## Application URL

After successful startup, open:

```
https://localhost:8443/main/main.html
```

If the browser displays a warning due to a self-signed certificate, choose
**Advanced → Continue**.

---

## Authentication & Registration

Users must authenticate in order to access the system.
New users can register through the application and are assigned roles according to the system configuration.

---

## Username & Password Rules

### Username

* Must be non-empty
* Length: 1–30 characters
* Leading and trailing spaces are trimmed
* Must be unique (case-insensitive)

Recommended format:

* Starts with a letter
* Contains only letters, digits, and spaces

Examples:

* Valid: `Noa`, `Or1`, `Ariel Student`
* Not recommended: `_Noa`, `Noa!`, `123Noa`

---

### Password

* Must be non-empty
* Length: 8–72 characters

---

## Creating a New Task

When creating a new task, the following validation rules apply:

### Date & Time

* The task date cannot be in the past.
* A time cannot be provided without a date.
* Providing a date without a time is allowed.

---

### Assigned Users

* At least one additional user must be assigned to the task.
* A task cannot be created with only the creator as the assigned user.

---

## Comments & File Uploads

### File Upload Limits

The application enforces strict multipart upload limits for security and DoS protection:

```properties
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=6MB
```

* Maximum file size: **5MB**
* Maximum total request size: **6MB**

Requests exceeding these limits are rejected.

---

### Comment Validation

* A comment cannot be empty.
* When uploading a file, textual content must also be provided.
* Submitting a file upload with an empty comment is not allowed.

---

## Data Persistence

Tasks are persisted locally using a file named:

```
tasks.dat
```

Behavior:

* If `tasks.dat` exists, tasks are loaded on startup.
* If `tasks.dat` is deleted, the application starts with no previously saved tasks.
* Deleting the file resets the task storage state.

---

## Logs

Application logs are written to:

```
logs/busybee.log
```

Additional logging configuration may be applied if `JAVA_TOOL_OPTIONS` is set.

---

## Portability Notes

The project is portable across machines as long as:

* Java 21 is installed
* Required environment variables are set
* TLS keystore configuration is available

No IDE-specific paths or user-specific directories are required.
