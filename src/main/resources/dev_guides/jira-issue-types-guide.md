# Jira Issue Types Guide

A practical guide for understanding Jira's issue hierarchy with Java/Angular examples.

---

## The Hierarchy

```
📦 Epic (Months)
├─── 📋 Story/Feature (Sprint)
│    ├─── ✓ Task (Days)
│    │    └─── ✓ Sub-task (Hours)
│    └─── 🐛 Bug (Variable)
```

---

## Issue Types Explained

### 🎯 Epic
**What:** Large body of work spanning multiple sprints (2-3 months)  
**Purpose:** Represents a major business initiative or system capability  
**Example:** "User Management System", "Payment Integration", "Reporting Dashboard"

**When to use:**
- Building an entire module or major feature set
- Work that takes multiple sprints to complete
- Strategic business initiatives

---

### 📋 Story (User Story)
**What:** A complete user-facing feature deliverable in one sprint  
**Purpose:** Delivers value to the end user  
**Format:** "As a [role], I want [feature] so that [benefit]"

**Examples:**
- "As a new user, I want to register an account so that I can access the system"
- "As a logged-in user, I want to edit my profile so that I can keep my information current"
- "As an admin, I want to view all users so that I can manage accounts"

**Acceptance Criteria:**
- User can fill registration form
- Email validation works
- Password meets security requirements
- Confirmation email is sent

**When to use:**
- Complete user-facing functionality
- Something that provides standalone value
- Can be completed within a sprint

---

### ✅ Task
**What:** Technical implementation work needed to complete a Story  
**Purpose:** Break down Stories into actionable development work  
**Assigned to:** Developers

**Examples (for "User Registration" Story):**
- Create User Entity and Repository (Java)
- Implement Registration REST API Endpoint
- Build Registration Form Component (Angular)
- Integrate Email Service for Confirmation
- Write Unit Tests for Registration Service

**When to use:**
- Technical implementation steps
- Work that doesn't independently deliver user value
- Breaking down a Story into manageable pieces

---

### 🔧 Sub-task
**What:** Further breakdown of a complex Task  
**Purpose:** Granular work items within a Task  
**Timeframe:** Hours, not days

**Examples (for "Implement Registration REST API" Task):**
- Create UserRegistrationDTO
- Implement controller method with @PostMapping
- Add Bean Validation annotations
- Handle exceptions and error responses
- Add API documentation

**When to use:**
- A Task is too complex and needs further breakdown
- Tracking very granular progress
- Multiple developers working on one Task

---

### 🐛 Bug
**What:** Defect or issue in existing functionality  
**Purpose:** Track and fix broken features  
**Priority:** Varies (Critical, High, Medium, Low)

**Example:**
- **Title:** "Registration fails when email contains special characters"
- **Priority:** High
- **Linked to:** Story: User Registration
- **Steps to reproduce:** Enter email with '+' symbol
- **Expected:** Registration succeeds
- **Actual:** Server returns 500 error

**When to use:**
- Something that should work doesn't work
- Regression in existing functionality
- Production issues

---

## Real-World Example: User Management System

### Epic: User Management System
Large initiative encompassing all user-related functionality (2-3 months)

#### Story 1: User Registration
As a new user, I want to register an account so that I can access the system

**Tasks:**
- Create User entity with Lombok annotations (Java)
  ```java
  @Entity
  @Table(name = "users")
  @Data
  public class User {
      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;
      
      @Column(unique = true, nullable = false)
      private String email;
      // ...
  }
  ```

- Implement UserController with registration endpoint (Spring Boot)
  ```java
  @RestController
  @RequestMapping("/api/users")
  public class UserController {
      
      @PostMapping("/register")
      public ResponseEntity<UserDto> register(@Valid @RequestBody UserRegistrationDto dto) {
          // Implementation
      }
  }
  ```

- Build registration component with Angular Material
  ```typescript
  @Component({
    selector: 'app-registration',
    templateUrl: './registration.component.html'
  })
  export class RegistrationComponent {
      registrationForm: FormGroup;
      // Implementation
  }
  ```

- Configure Spring Mail for confirmation emails
- Write unit and integration tests

#### Story 2: User Login & Authentication
As a registered user, I want to log in securely so that I can access my account

**Tasks:**
- Implement JWT token generation and validation
- Create Login API endpoint with Spring Security
- Build login component (Angular)
- Add session management with HttpOnly cookies
- Implement "Remember Me" functionality

#### Story 3: User Profile Management
As a logged-in user, I want to view and edit my profile

#### Story 4: Password Reset Flow
As a user who forgot their password, I want to reset it via email

#### Bug: Registration Email Validation Issue
Email with special characters causes server error

---

## Quick Decision Guide

| What are you doing? | Use this |
|---------------------|----------|
| Building an entire system/module (months) | **Epic** |
| Delivering a complete user feature (1 sprint) | **Story** |
| Technical implementation work | **Task** |
| Breaking down a complex task | **Sub-task** |
| Fixing something broken | **Bug** |

---

## Best Practices

### Writing Good Stories
✅ **DO:**
- Use user-centric language: "As a [role], I want [feature]..."
- Include clear acceptance criteria
- Keep it small enough for one sprint
- Focus on the "what" and "why", not the "how"

❌ **DON'T:**
- Make them too technical: "Implement JPA repository"
- Make them too large: "Build entire admin panel"
- Forget acceptance criteria

### Writing Good Tasks
✅ **DO:**
- Be specific about the technical work
- Include technology/framework: "Create Angular component with Material"
- Make them independently testable
- Estimate effort (hours/story points)

❌ **DON'T:**
- Make them user-facing (use Story instead)
- Make them too vague: "Fix the backend"
- Mix frontend and backend work

### Linking Issues
- Tasks should be **linked to** or **part of** a Story
- Stories should be **linked to** an Epic
- Bugs can be linked to Stories or standalone
- Use Jira's "Epic Link" field to connect Stories to Epics

---

## Creating Issues in Jira

### Create an Epic
1. Go to **Backlog**
2. Look for **Epics panel** (left side)
3. Click **"+ Create Epic"**
4. Fill in:
   - Epic Name: "User Management System"
   - Epic Summary: Detailed description
   - Epic Color: Choose for visual identification
5. Click **Create**

### Create a Story
1. Click **"Create"** button
2. Select **"Story"** as Issue Type
3. Fill in:
   - Summary: "User can register an account"
   - Description: User story with acceptance criteria
   - Epic Link: Select your Epic
4. Click **Create**

### Create a Task
1. Open the Story you want to add Tasks to
2. Click **"Create subtask"** or **"Link issue"**
3. Select **"Task"** as Issue Type
4. Fill in technical details
5. Link to parent Story

---

## Java/Angular Development Context

### Backend Tasks (Java/Spring)
- Create entity and repository
- Implement service layer with business logic
- Build REST API controllers
- Add security configuration
- Write unit tests (JUnit, Mockito)
- Add database migrations (Flyway/Liquibase)

### Frontend Tasks (Angular)
- Create Angular components
- Build reactive forms with validation
- Design UI with Angular Material
- Implement services for API calls
- Add routing and guards
- Write component tests (Jasmine/Karma)

### Full-Stack Tasks
- Integration testing
- API contract definition (OpenAPI/Swagger)
- Documentation updates
- Performance optimization
- Security hardening

---

## Summary

**Think of it this way:**
- **Epic** = A book (the whole story)
- **Story** = A chapter (complete narrative)
- **Task** = A paragraph (technical detail)
- **Sub-task** = A sentence (smallest unit)
- **Bug** = An error that needs correction

The key is starting big (Epic) and breaking down into smaller, manageable pieces (Stories → Tasks → Sub-tasks) that your team can work on within a sprint.

---

**Remember:** The goal is to organize work effectively, not to create perfect categorization. When in doubt, err on the side of simplicity and adjust as your team learns what works best.
