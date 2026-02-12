# Weighbridge Management System

A production-grade desktop Weighbridge Management System built using Java and JavaFX.  
Designed as a stable and maintainable replacement for legacy weighbridge systems (including those running on Windows 7), while remaining compatible with modern Windows environments.

This system provides real-time serial device integration, structured weighment workflow management, secure authentication, receipt printing, and automated database backup functionality.

---

## Overview

The application is optimized for deployment on **dedicated weighbridge computers** used exclusively for weighment operations. It focuses on:

- Operational stability
- Clean architecture
- Security best practices
- Maintainability
- Ease of deployment in industrial environments

---

## Core Features

### Authentication & Security
- Secure login system
- Password hashing using BCrypt
- AES encryption utilities
- Environment-based secret management using `.env`
- Externalized configuration for sensitive credentials

---

### Weighment Workflow
- Real-time weight display from connected scale
- First Weight / Second Weight workflow
- Automatic net weight calculation
- Record validation before persistence
- Structured record storage

---

### Serial Port Integration
- Live weight reading from weighbridge device
- Configurable serial port settings (baud rate, port selection, etc.)
- Continuous device listening support
- Serial configuration stored in database

---

### Database & Backup Management
- MySQL-based persistent storage
- Layered architecture:
Controller → Service → DAO → DatabaseConfig
- Clean exception propagation to UI layer
- Automatic backups (Daily / Weekly / Monthly)
- Manual and automatic restore support

---

### Receipt Printing
- JasperReports-based receipt generation
- Structured printable receipt layout
- Configurable print modes

---

### Dedicated System Mode

Designed for weighbridge terminals that operate solely for this application.

- Optional automatic application startup on system boot
- Controlled system shutdown via Exit action
- Reduced operator misuse
- Optimized for single-terminal environments

---

### Logging & Diagnostics
- Log4j integration
- Structured error logging
- Improved exception traceability across layers

---

## Architecture

- JavaFX-based MVC structure
- Clear separation of concerns
- Service and DAO abstraction layers
- Centralized configuration handling
- Modular and extendable design

The system is structured for long-term maintainability and future scalability (e.g., cloud sync or multi-terminal support).

---

## Technology Stack

- Java 8
- JavaFX
- MySQL 5.7+
- JasperReports
- Log4j
- BCrypt
- AES Encryption
- Dotenv configuration loader
- Launch4j (Windows packaging)

---

## Installation (Runtime Deployment)

### Requirements
- JRE 8
- MySQL Server 5.7+

### Setup Steps
1. Download the compiled `.exe` from the Releases section.
2. Create a `/keys` directory in the application root.
3. Place `.env` inside `/keys`.
4. Place `dbconfig.properties` in the application root.
5. Update configuration values accordingly.
6. Ensure MySQL service is running.

---

## Developer Setup (Building from Source)

### Requirements
- JDK 8
- MySQL Server
- IDE (IntelliJ IDEA / Eclipse recommended)

### Build Steps (Maven example)

If using Maven:

```bash
mvn clean install
```

If using Gradle:

```bash
gradle build
```

The compiled JAR will be generated inside the target/ or build/ directory.

## Packaging to Windows EXE

This repository includes: `launch4j-config.xml`. This file contains a preconfigured Launch4j setup for generating a Windows executable. It includes:
- Default JVM options
- JRE binding configuration
- Executable metadata configuration
- Simplified packaging workflow

To generate the .exe:

1. Open Launch4j.
2. Load launch4j-config.xml.
3. Adjust paths if necessary. 
4. Build the executable.

This removes the need to manually configure Launch4j each time.

## Deployment Model

Current version is optimized for:
- Single-system deployment
- Dedicated weighbridge terminals
- Local database architecture

Future versions may include distributed and cloud-based capabilities.

## Roadmap
- Print preview support
- Manual printer selection
- Full record history management
- Cloud synchronization
- Multi-terminal deployment support
- Modern OS optimized replica

## License
This project is licensed under the **MIT License**.  
You are free to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of this software, provided that the original copyright notice and this permission notice are included in all copies or substantial portions of the software.  
The software is provided "as is", without warranty of any kind.
