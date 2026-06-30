# Jakarta EE Hourglass AI Modeler

## Challenge

This is a template project for the Jakarta EE Hourglass AI Modeler challenge.

## Requirements

- Java 23 or later (Java 25 for GlassFish)
- Maven 3.8 or later
- One of the supported Jakarta EE servers:
  - GlassFish 8.0.0 or later
  - Payara 7.2025.2 or later
  - WildFly 39.0.1.Final or later
  - Open Liberty 26.0.0.3-beta or later

## Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd hourglass-template
mvn clean package
```

### 2. Run on Your Preferred Server

#### GlassFish
```bash
mvn clean package cargo:run -Pglassfish
```

#### Payara
```bash
mvn clean package cargo:run -Ppayara
```

#### WildFly
```bash
mvn clean package wildfly:run
```

#### Open Liberty
```bash
mvn clean package liberty:run
```

### 3. Test the Server

The Status endpoint is available at: `http://localhost:8080/duke-knows-me/status´


