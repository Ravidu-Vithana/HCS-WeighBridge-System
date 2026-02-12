# Weighbridge Management System

**Weighbridge Management System** is a production-grade desktop application built with **Java 8** and **JavaFX**, designed as a stable and maintainable replacement for legacy weighbridge software (including Windows 7) while remaining compatible with modern Windows environments.  

This system provides real-time serial device integration, structured weighment workflow management, secure authentication, receipt printing, and automated database backup functionality. Ideal for **industrial weighbridge operations** and **dedicated weighbridge terminals**.

---

## Overview

The **Weighbridge Management System** is built for **dedicated industrial weighbridge computers**, providing a reliable, secure, and maintainable workflow for managing all weighment operations.  

Key benefits include:

- **Operational stability** for industrial and commercial weighing environments
- **Secure and maintainable architecture** with centralized configuration
- **Clear separation of concerns** using MVC-based JavaFX design
- **Ease of deployment** with automatic startup and controlled shutdown
- **Scalable design** for future enhancements such as cloud synchronization or multi-terminal support

---

## Core Features

### Authentication & Security
- **Secure login system** with password hashing using BCrypt
- **AES encryption** for sensitive data
- **Environment-based secret management** using `.env`
- Centralized configuration for database and serial port credentials

### Weighment Workflow
- **Real-time weight display** from connected industrial scales
- **Two-step weighing workflow** (First Weight / Second Weight)
- Automatic **net weight calculation**
- Input validation and structured record storage
- Persistent storage of weighment data in MySQL database

### Serial Port Integration
- Live weight reading from industrial weighbridge devices
- Configurable **serial port settings** (baud rate, port selection)
- Continuous device listening for real-time updates
- Dynamic serial configuration stored in database

### Database & Backup Management
- MySQL 5.7+ database integration for **secure, persistent storage**
- Layered architecture: `Controller → Service → DAO → DatabaseConfig`
- Exception propagation for better **UI-level feedback**
- Automatic and manual backups (Daily / Weekly / Monthly)
- Easy restoration for operational continuity

### Receipt Printing
- **JasperReports**-based receipt generation
- Structured, **professional printable receipt layout**
- Configurable print modes for different business needs

### Dedicated System Mode
- Optimized for computers dedicated solely to weighbridge operations
- Optional **automatic application startup**
- Controlled **system shutdown via Exit action**
- Minimizes operator misuse and ensures workflow consistency

### Logging & Diagnostics
- **Log4j integration** for detailed logging
- Structured error reporting
- Traceability across Controller, Service, and DAO layers

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
