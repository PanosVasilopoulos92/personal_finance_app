# Maven Developer Guide

> A practical reference for understanding Maven concepts and using its commands effectively in Spring Boot development.

---

## What Maven Actually Does

Maven is a **build automation and dependency management tool**. It answers three core questions:

1. **What libraries does my project need?** → Dependency management
2. **How do I compile, test, and package my code?** → Build lifecycle
3. **How do I ensure consistent builds across machines?** → Standardized project structure + POM

Without Maven, you'd manually download JARs, manage classpaths, write build scripts, and handle transitive dependencies yourself. Maven automates all of this through a declarative XML file (`pom.xml`).

---

## The POM: Project Object Model

The `pom.xml` is Maven's configuration file. Here's what each section does:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <!-- POM model version (always 4.0.0 for Maven 2+) -->
    <modelVersion>4.0.0</modelVersion>

    <!-- Inherit from Spring Boot's parent POM -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.1</version>
        <relativePath/>
    </parent>

    <!-- Project coordinates (unique identifier) -->
    <groupId>com.viators</groupId>           <!-- Organization/namespace -->
    <artifactId>personal-finance-app</artifactId>  <!-- Project name -->
    <version>0.0.1-SNAPSHOT</version>        <!-- Version -->
    
    <!-- Build settings -->
    <properties>
        <java.version>25</java.version>
    </properties>
    
    <!-- Libraries your project needs -->
    <dependencies>
        <!-- ... -->
    </dependencies>
    
    <!-- Build configuration (plugins, resources) -->
    <build>
        <!-- ... -->
    </build>
</project>
```

### GAV Coordinates

Every Maven artifact is uniquely identified by three coordinates:

| Coordinate | Purpose | Example |
|------------|---------|---------|
| **groupId** | Organization namespace | `org.springframework.boot` |
| **artifactId** | Project/module name | `spring-boot-starter-web` |
| **version** | Release version | `4.0.1` |

When you declare a dependency, you're telling Maven: "Find this exact artifact and add it to my project."

---

## Dependency Management

### Basic Dependency Declaration

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- Version inherited from parent POM -->
</dependency>
```

### Dependency Scopes

Scopes control **when** a dependency is available:

| Scope | Compile | Test | Runtime | Packaged | Use Case |
|-------|---------|------|---------|----------|----------|
| `compile` (default) | ✓ | ✓ | ✓ | ✓ | Most dependencies |
| `runtime` | ✗ | ✓ | ✓ | ✓ | JDBC drivers, implementations |
| `test` | ✗ | ✓ | ✗ | ✗ | JUnit, Mockito, H2 |
| `provided` | ✓ | ✓ | ✗ | ✗ | Servlet API (container provides) |

**Examples from your project:**

```xml
<!-- Compile scope (default): needed everywhere -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Runtime scope: not needed at compile time, only at runtime -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Test scope: only for testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Optional: available at compile time but not required by dependents -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

### Transitive Dependencies

When you add `spring-boot-starter-web`, Maven automatically includes its dependencies (Spring MVC, Jackson, Tomcat, etc.). This is **transitive dependency resolution**.

View what's actually being pulled in:

```bash
mvn dependency:tree
```

Output shows the full hierarchy:

```
[INFO] com.viators:personal-finance-app:jar:0.0.1-SNAPSHOT
[INFO] +- org.springframework.boot:spring-boot-starter-web:jar:4.0.1:compile
[INFO] |  +- org.springframework.boot:spring-boot-starter:jar:4.0.1:compile
[INFO] |  |  +- org.springframework.boot:spring-boot:jar:4.0.1:compile
[INFO] |  |  +- org.springframework:spring-core:jar:7.0.0:compile
```

### Version Management with Parent POM

Spring Boot's parent POM pre-defines versions for hundreds of libraries. That's why you don't specify versions for most Spring dependencies:

```xml
<!-- Version comes from spring-boot-starter-parent -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- External library: you must specify version -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>
```

---

## Build Lifecycle

Maven's build process follows a **lifecycle** with ordered phases. Each phase executes all preceding phases.

### Default Lifecycle Phases

```
validate → compile → test → package → verify → install → deploy
```

| Phase | What Happens |
|-------|--------------|
| `validate` | Validates project structure and POM |
| `compile` | Compiles `src/main/java` → `target/classes` |
| `test` | Runs tests from `src/test/java` |
| `package` | Creates JAR/WAR in `target/` |
| `verify` | Runs integration tests and checks |
| `install` | Copies artifact to `~/.m2/repository` |
| `deploy` | Uploads artifact to remote repository |

**Key insight:** Running `mvn package` executes validate → compile → test → package.

### Clean Lifecycle

```bash
mvn clean    # Deletes the target/ directory
```

Always combine with other phases when you want a fresh build:

```bash
mvn clean package    # Delete target/, then build through package
```

---

## Essential Commands

### Daily Development

```bash
# Compile and run tests
mvn test

# Compile without tests (fast feedback on compilation)
mvn compile

# Package without tests (when you know tests pass)
mvn package -DskipTests

# Full clean build with tests
mvn clean verify

# Run Spring Boot application
mvn spring-boot:run
```

### Dependency Commands

```bash
# Show all dependencies (including transitive)
mvn dependency:tree

# Find unused/undeclared dependencies
mvn dependency:analyze

# Download all dependencies (useful for offline work)
mvn dependency:go-offline

# Check for newer versions of dependencies
mvn versions:display-dependency-updates

# Check for newer versions of plugins
mvn versions:display-plugin-updates
```

### Troubleshooting Commands

```bash
# Force re-download of dependencies
mvn clean install -U

# Debug output (very verbose)
mvn clean install -X

# Show effective POM (with inherited settings resolved)
mvn help:effective-pom

# Show effective settings
mvn help:effective-settings

# Describe what a plugin goal does
mvn help:describe -Dplugin=spring-boot -Dgoal=run -Ddetail
```

### Test Commands

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=UserServiceTest

# Run a specific test method
mvn test -Dtest=UserServiceTest#shouldCreateUser

# Run tests matching a pattern
mvn test -Dtest=*Service*

# Skip tests entirely
mvn package -DskipTests

# Skip test compilation AND execution
mvn package -Dmaven.test.skip=true

# Run only integration tests (if configured separately)
mvn verify -DskipUnitTests
```

---

## Common Command Flags

| Flag | Purpose | Example |
|------|---------|---------|
| `-DskipTests` | Skip test execution (still compiles tests) | `mvn package -DskipTests` |
| `-Dmaven.test.skip=true` | Skip test compilation and execution | `mvn package -Dmaven.test.skip=true` |
| `-U` | Force update of snapshots | `mvn clean install -U` |
| `-o` | Offline mode (use cached dependencies) | `mvn compile -o` |
| `-X` | Debug output | `mvn clean install -X` |
| `-q` | Quiet mode (errors only) | `mvn clean install -q` |
| `-P` | Activate profile | `mvn package -Pproduction` |
| `-pl` | Build specific module | `mvn install -pl api-module` |
| `-am` | Also make dependencies | `mvn install -pl api-module -am` |

---

## Profiles

Profiles let you customize builds for different environments:

```xml
<profiles>
    <!-- Development profile (active by default) -->
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <spring.profiles.active>dev</spring.profiles.active>
        </properties>
    </profile>
    
    <!-- Production profile -->
    <profile>
        <id>prod</id>
        <properties>
            <spring.profiles.active>prod</spring.profiles.active>
        </properties>
        <build>
            <plugins>
                <!-- Additional plugins for production -->
            </plugins>
        </build>
    </profile>
    
    <!-- Skip slow tests during rapid development -->
    <profile>
        <id>fast</id>
        <properties>
            <skipITs>true</skipITs>
        </properties>
    </profile>
</profiles>
```

Activate profiles:

```bash
mvn package -Pprod           # Single profile
mvn package -Pprod,fast      # Multiple profiles
mvn package -P!dev           # Deactivate a profile
```

---

## Plugins

Plugins extend Maven's capabilities. The Spring Boot plugin is essential for your project:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <excludes>
                    <exclude>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </exclude>
                </excludes>
            </configuration>
        </plugin>
        
        <!-- Test coverage -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Useful Plugin Commands

```bash
# Run Spring Boot app
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run with debug port
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

# Generate test coverage report
mvn test jacoco:report
# Report at: target/site/jacoco/index.html

# Generate project site/documentation
mvn site
```

---

## Local Repository

Maven caches downloaded artifacts in `~/.m2/repository`. This structure mirrors GAV coordinates:

```
~/.m2/repository/
└── org/
    └── springframework/
        └── boot/
            └── spring-boot-starter-web/
                └── 4.0.1/
                    ├── spring-boot-starter-web-4.0.1.jar
                    └── spring-boot-starter-web-4.0.1.pom
```

### Repository Troubleshooting

```bash
# Corrupted dependency? Delete and re-download
rm -rf ~/.m2/repository/org/springframework/boot/spring-boot-starter-web/4.0.1
mvn clean install

# Nuclear option: clear entire cache
rm -rf ~/.m2/repository
mvn clean install   # Re-downloads everything
```

---

## Maven Wrapper

The Maven Wrapper (`mvnw`) ensures everyone uses the same Maven version:

```bash
# Unix/Mac
./mvnw clean install

# Windows
mvnw.cmd clean install
```

**Why use it?**
- Guarantees consistent Maven version across team
- CI/CD doesn't need Maven pre-installed
- New developers don't need to install Maven manually

Generate wrapper for your project:

```bash
mvn wrapper:wrapper -Dmaven=3.9.6
```

---

## Troubleshooting Guide

### "Could not resolve dependencies"

```bash
# 1. Check if Maven Central is reachable
mvn dependency:tree

# 2. Force update from remote repositories
mvn clean install -U

# 3. Clear local cache for problematic dependency
rm -rf ~/.m2/repository/com/problematic/library
mvn clean install
```

### "Cannot find symbol" but dependency exists

```bash
# Check if dependency has correct scope
mvn dependency:tree | grep library-name

# Verify it's compile scope, not test/runtime
# If using IDE, refresh/reimport Maven project
```

### Build works locally but fails in CI

```bash
# Replicate CI environment
mvn clean verify -U    # Force fresh dependencies

# Check for environment-specific configs
mvn help:effective-pom # See resolved POM
```

### Tests pass individually but fail together

```bash
# Run tests in isolation to find interference
mvn test -Dtest=TestClass1
mvn test -Dtest=TestClass2

# Force test isolation
mvn test -DreuseForks=false -DforkCount=1
```

### Out of memory during build

```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx1024m -XX:MaxMetaspaceSize=256m"
mvn clean install
```

---

## Quick Reference Card

### Build Commands

| Command | Purpose |
|---------|---------|
| `mvn compile` | Compile source code |
| `mvn test` | Run unit tests |
| `mvn package` | Create JAR/WAR |
| `mvn install` | Install to local repo |
| `mvn clean` | Delete target/ |
| `mvn clean verify` | Full clean build |
| `mvn spring-boot:run` | Run application |

### Dependency Commands

| Command | Purpose |
|---------|---------|
| `mvn dependency:tree` | Show dependency hierarchy |
| `mvn dependency:analyze` | Find unused dependencies |
| `mvn versions:display-dependency-updates` | Check for updates |

### Common Flags

| Flag | Purpose |
|------|---------|
| `-DskipTests` | Skip tests |
| `-U` | Force update |
| `-o` | Offline mode |
| `-P<profile>` | Activate profile |
| `-X` | Debug output |

---

## VS Code Integration

For VS Code, install the **Extension Pack for Java** which includes Maven support:

- View `pom.xml` dependencies in Explorer
- Right-click to run Maven goals
- Integrated terminal for commands
- Dependency management UI

Common tasks in VS Code:
1. **Ctrl+Shift+P** → "Maven: Execute Commands" → Select goal
2. Or use the Maven panel in Explorer sidebar
3. Terminal: `./mvnw spring-boot:run`

---

## Further Learning

- [Maven Official Documentation](https://maven.apache.org/guides/)
- [Spring Boot Maven Plugin Reference](https://docs.spring.io/spring-boot/docs/current/maven-plugin/reference/html/)
- Run `mvn help:describe -Dplugin=<plugin-name> -Ddetail` to explore any plugin
