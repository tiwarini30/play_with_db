# JPA and EntityManager in Spring Boot  

**JPA (Java Persistence API)** is a **Java specification/standard** for managing relational data in Java applications. It provides:

- **Object-Relational Mapping (ORM)** framework
- Abstraction layer between Java objects and database tables
- A set of interfaces and classes to interact with persistent data

### Key Points:
- **Not an implementation** - It's a specification/contract
- **Declarative approach** - Uses annotations instead of XML configuration
- **Database agnostic** - Can work with any relational database

---

## Why Use JPA

### 1. **Abstraction from Database Details**
```
Java Application → JPA API → Hibernate/EclipseLink → Database
```
- Write database-agnostic code
- Switch databases without changing business logic

### 2. **Reduces Boilerplate Code**
```java
// Without JPA (Manual JDBC)
Connection conn = DriverManager.getConnection(url, user, password);
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE id = 1");
// Manual mapping...

// With JPA
EntityManager em = ...;
User user = em.find(User.class, 1);
```

### 3. **Automatic Data Mapping**
- Automatically maps database columns to Java object properties
- No need for manual ResultSet-to-Object conversion

### 4. **Transaction Management**
- Built-in transaction handling
- Automatic rollback on errors

### 5. **Lazy Loading & Performance Optimization**
- Load related entities only when needed
- Reduces memory usage

### 6. **Query Language (JPQL)**
- Database-independent query language
- Similar to SQL but works with objects instead of tables

### 7. **Caching Support**
- First-level cache (session-level)
- Second-level cache (application-level)

---

## JPA Use Cases & Concepts

### Use Cases:

1. **CRUD Operations** - Create, Read, Update, Delete records
2. **Complex Queries** - Multi-join queries
3. **Transactions** - Batch processing with rollback support
4. **Relationship Management** - One-to-One, One-to-Many, Many-to-Many
5. **Audit Trails** - Track entity changes automatically

### Core Concepts:

#### 1. **Entity**
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_name", nullable = false)
    private String name;
    
    @Email
    private String email;
}
```

#### 2. **Persistence Context**
- A cache/memory area that tracks managed entities
- Entities within it are synchronized with the database
- Scoped to a transaction or session

#### 3. **Entity States**
```
┌─────────────────────────────────────────────────┐
│         Entity Lifecycle States                  │
├─────────────────────────────────────────────────┤
│ 1. NEW/TRANSIENT - Object created, not saved    │
│ 2. MANAGED - In persistence context, tracked    │
│ 3. DETACHED - Was managed, now detached         │
│ 4. REMOVED - Marked for deletion                │
└─────────────────────────────────────────────────┘
```

#### 4. **Relationships**

**One-to-One:**
```java
@Entity
public class User {
    @Id
    private Long id;
    
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_id")
    private Profile profile;
}
```

**One-to-Many:**
```java
@Entity
public class Department {
    @Id
    private Long id;
    
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private List<Employee> employees;
}
```

**Many-to-Many:**
```java
@Entity
public class Student {
    @Id
    private Long id;
    
    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<Course> courses;
}
```

---

## Hibernate in JPA

### What is Hibernate?

**Hibernate** is the **most popular implementation** of JPA specification. It's an ORM framework that provides:

- Implementation of JPA interfaces
- Additional features beyond JPA specification
- Query language (HQL) similar to SQL
- Caching mechanisms

### Architecture:

```
┌─────────────┐
│ Java Code   │
└──────┬──────┘
       ↓
┌──────────────────┐
│  Hibernate Core  │
│  (JPA Provider)  │
└────────┬─────────┘
         ↓
┌──────────────────┐
│  JPA Provider    │
│  (SessionFactory)│
└────────┬─────────┘
         ↓
┌──────────────────┐
│  JDBC Driver     │
└────────┬─────────┘
         ↓
┌──────────────────┐
│   Database       │
└──────────────────┘
```

### Key Hibernate Annotations:

| Annotation | Purpose |
|-----------|---------|
| `@Entity` | Marks class as persistent entity |
| `@Table` | Specifies table name |
| `@Id` | Primary key |
| `@GeneratedValue` | Auto-generate primary key |
| `@Column` | Maps to specific column |
| `@Transient` | Excludes field from persistence |
| `@Temporal` | Date/Time mapping |
| `@Enumerated` | Enum mapping |
| `@Lob` | Large Object (BLOB/CLOB) |

---

## EntityManager - Definition

### What is EntityManager?

**EntityManager** is the **central interface** for managing entities in JPA. It acts as:

- **Bridge between Java objects and database**
- **Manager of the Persistence Context**
- **Provider of CRUD and query operations**

### Key Responsibilities:

1. **Create** - Persist new entities
2. **Read** - Retrieve entities by ID or query
3. **Update** - Modify managed entities
4. **Delete** - Remove entities
5. **Manage Persistence Context** - Track entity state changes
6. **Handle Transactions** - Commit/rollback operations

---

## EntityManager Classes & Methods

### EntityManager Interface

```java
public interface EntityManager {
    // Persistence Operations
    void persist(Object entity);                    // Create
    <T> T find(Class<T> clazz, Object id);         // Read by ID
    void merge(Object entity);                      // Update
    void remove(Object entity);                     // Delete
    void flush();                                   // Force DB sync
    void clear();                                   // Clear persistence context
    void detach(Object entity);                     // Detach entity
    void refresh(Object entity);                    // Reload from DB
    
    // Query Operations
    Query createQuery(String jpql);
    Query createNativeQuery(String sql);
    TypedQuery<T> createQuery(String jpql, Class<T> clazz);
    
    // Transaction
    EntityTransaction getTransaction();
    
    // Persistence Context
    boolean contains(Object entity);
    void lock(Object entity, LockModeType lockMode);
}
```

### Main EntityManager Methods - Detailed

#### 1. **persist(Object entity)** - CREATE

```java
// Marks a transient entity as managed and schedules INSERT
EntityManager em = emf.createEntityManager();
EntityTransaction tx = em.getTransaction();

User user = new User();
user.setName("John");
user.setEmail("john@example.com");

tx.begin();
em.persist(user);  // Scheduled for INSERT, not immediately executed
tx.commit();       // INSERT executed here
em.close();
```

**Characteristics:**
- Throws `EntityExistsException` if entity already exists
- Entity becomes MANAGED
- INSERT executed on commit()

#### 2. **find(Class<T> clazz, Object id)** - READ by ID

```java
EntityManager em = emf.createEntityManager();

// Read with ID = 1
User user = em.find(User.class, 1L);

if (user != null) {
    System.out.println(user.getName());
}

em.close();
```

**Characteristics:**
- Returns null if not found (no exception)
- Checks persistence context first
- If not found in context, queries database
- Returns entity in MANAGED state

#### 3. **merge(Object entity)** - UPDATE

```java
EntityManager em = emf.createEntityManager();
EntityTransaction tx = em.getTransaction();

User detachedUser = new User();
detachedUser.setId(1L);
detachedUser.setName("Updated Name");

tx.begin();
User managedUser = em.merge(detachedUser);  // DETACHED → MANAGED
managedUser.setEmail("new@example.com");
tx.commit();
em.close();
```

**Characteristics:**
- Works with DETACHED entities
- Returns a NEW managed copy
- Original entity remains detached
- UPDATE executed on commit()

#### 4. **remove(Object entity)** - DELETE

```java
EntityManager em = emf.createEntityManager();
EntityTransaction tx = em.getTransaction();

User user = em.find(User.class, 1L);

tx.begin();
em.remove(user);    // Marks as REMOVED
// DELETE executed on commit()
tx.commit();
em.close();
```

**Characteristics:**
- Throws `IllegalArgumentException` if entity not managed
- Entity becomes REMOVED
- DELETE executed on commit()

#### 5. **flush()** - Force Database Synchronization

```java
EntityManager em = emf.createEntityManager();
EntityTransaction tx = em.getTransaction();

User user = em.find(User.class, 1L);

tx.begin();
user.setName("New Name");
em.flush();  // INSERT/UPDATE/DELETE executed immediately
// But transaction not committed yet
tx.commit();
em.close();
```

**Characteristics:**
- Synchronizes persistence context with database
- SQL executed but transaction not committed
- Used for immediate feedback
- Default flush mode: AUTO (on commit/query)

#### 6. **clear()** - Clear Persistence Context

```java
EntityManager em = emf.createEntityManager();

User user1 = em.find(User.class, 1L);
User user2 = em.find(User.class, 1L);
System.out.println(user1 == user2);  // true (same object)

em.clear();  // Clears all entities

User user3 = em.find(User.class, 1L);
System.out.println(user1 == user3);  // false (different objects)

em.close();
```

**Characteristics:**
- Detaches all managed entities
- Clears first-level cache
- Useful for batch processing

#### 7. **detach(Object entity)** - Manually Detach

```java
EntityManager em = emf.createEntityManager();

User user = em.find(User.class, 1L);
System.out.println(em.contains(user));  // true

em.detach(user);  // Manually detach
System.out.println(em.contains(user));  // false

em.close();
```

**Characteristics:**
- Removes entity from persistence context
- Changes after detach won't be persisted
- Other entities still managed

#### 8. **refresh(Object entity)** - Reload from Database

```java
EntityManager em = emf.createEntityManager();

User user = em.find(User.class, 1L);
user.setName("Changed Name");  // In memory only

em.refresh(user);  // Reload from DB
System.out.println(user.getName());  // Original DB value

em.close();
```

**Characteristics:**
- Discards in-memory changes
- Reloads current state from database
- Useful after concurrent modifications

#### 9. **contains(Object entity)** - Check if Managed

```java
EntityManager em = emf.createEntityManager();

User user = em.find(User.class, 1L);
System.out.println(em.contains(user));  // true (managed)

em.detach(user);
System.out.println(em.contains(user));  // false (detached)

em.close();
```

#### 10. **createQuery() / createNativeQuery()** - Query Operations

```java
EntityManager em = emf.createEntityManager();

// JPQL Query
TypedQuery<User> query = em.createQuery(
    "SELECT u FROM User u WHERE u.email = :email", 
    User.class
);
query.setParameter("email", "john@example.com");
User user = query.getSingleResult();

// Native SQL Query
Query nativeQuery = em.createNativeQuery(
    "SELECT * FROM users WHERE id = ?1", 
    User.class
);
nativeQuery.setParameter(1, 1L);
User user = (User) nativeQuery.getSingleResult();

em.close();
```

---

## EntityManager Factory

### What is EntityManagerFactory?

**EntityManagerFactory** is a **thread-safe factory** that creates EntityManager instances.

```java
public interface EntityManagerFactory {
    EntityManager createEntityManager();
    void close();
    boolean isOpen();
    CriteriaBuilder getCriteriaBuilder();
    Metamodel getMetamodel();
}
```

### Usage:

```java
// Create factory (expensive, do once)
EntityManagerFactory emf = Persistence.createEntityManagerFactory("unit-name");

// Create EntityManager (lightweight, can create multiple)
EntityManager em = emf.createEntityManager();

// Use em...

em.close();
emf.close();  // Close at application shutdown
```

### In Spring Boot:

```java
@Configuration
public class JpaConfig {
    @Bean
    public EntityManager entityManager(EntityManagerFactory emf) {
        return emf.createEntityManager();
    }
}
```

---

## EntityTransaction Interface

### What is EntityTransaction?

Manages transaction lifecycle for a specific EntityManager.

```java
public interface EntityTransaction {
    void begin();           // Start transaction
    void commit();          // Commit changes
    void rollback();        // Rollback changes
    void setRollbackOnly(); // Mark for rollback
    boolean isActive();     // Check if active
}
```

### Usage:

```java
EntityManager em = emf.createEntityManager();
EntityTransaction tx = em.getTransaction();

try {
    tx.begin();
    
    User user = new User();
    user.setName("John");
    em.persist(user);
    
    tx.commit();  // Save changes
} catch (Exception e) {
    if (tx.isActive()) {
        tx.rollback();  // Undo changes
    }
} finally {
    em.close();
}
```

---

## JPA Working Process

### Complete Workflow Diagram:

```
┌─────────────────────────────────────────────────────────────────┐
│                    JPA COMPLETE WORKFLOW                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. Application Layer                                            │
│     ↓                                                             │
│     Create EntityManagerFactory (from persistence.xml)          │
│     ↓                                                             │
│  2. Create EntityManager                                         │
│     ↓                                                             │
│  3. Begin Transaction                                            │
│     ↓                                                             │
│  4. Perform Operations                                           │
│     - persist() / merge() / remove() / find()                    │
│     - Changes tracked in Persistence Context                     │
│     ↓                                                             │
│  5. Dirty Checking (Automatic)                                   │
│     - Detect changes in managed entities                         │
│     ↓                                                             │
│  6. Flush                                                        │
│     - Generate SQL (INSERT/UPDATE/DELETE)                        │
│     - Execute SQL to database                                    │
│     ↓                                                             │
│  7. Commit Transaction                                           │
│     - Confirm changes in database                                │
│     ↓                                                             │
│  8. Close EntityManager                                          │
│     - Release resources                                          │
│     - Detach all entities                                        │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### Step-by-Step Example:

```java
// Step 1: Configuration
String persistenceUnitName = "myPersistenceUnit";
EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnitName);

// Step 2: Create EntityManager
EntityManager em = emf.createEntityManager();

// Step 3: Begin Transaction
EntityTransaction tx = em.getTransaction();
tx.begin();

// Step 4: Create Entity
User user = new User();  // Transient state
user.setName("John Doe");
user.setEmail("john@example.com");

// Step 5: Persist Entity
em.persist(user);  // Managed state, in persistence context
// SQL INSERT is NOT generated yet

// Step 6: Modify Entity
user.setName("John Updated");  // Change tracked automatically

// Step 7: Another Operation
User user2 = em.find(User.class, 1L);  // Retrieve from DB
em.persist(user2);  // Multiple operations queued

// Step 8: Flush (Explicit)
em.flush();
// SQL Generated & Executed:
// INSERT INTO users (name, email) VALUES ('John Updated', 'john@example.com')
// UPDATE users SET name = 'John Updated' WHERE id = ...

// Step 9: Commit
tx.commit();  // Confirm transaction at DB level

// Step 10: Close
em.close();  // Entities become detached
emf.close();
```

### Entity State Transitions:

```
┌──────────────┐
│  NEW/TRANSIENT  │ (Object created, not in DB)
└────────┬────────┘
         │ persist()
         ↓
┌──────────────────┐
│  MANAGED         │ (In persistence context, tracked)
└────────┬────────┬─────────────┐
         │        │             │
  merge()│   detach()      remove()
    /merge  /close()      
         ↓        ↓             ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  DETACHED    │  │  DETACHED    │  │  REMOVED     │
│              │  │              │  │              │
└──────────────┘  └──────────────┘  └──────────────┘
   (Was managed,        (Session       (Marked for
    now separate)       closed)        deletion)
```

---

## Spring Boot JPA Integration

### Spring Boot Configuration

#### 1. **Dependencies (pom.xml)**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

#### 2. **Application Properties**

```properties
# application.properties

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.use_sql_comments=true

# Logging
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql=TRACE
```

#### 3. **Entity Definition**

```java
package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "email", unique = true, nullable = false)
    private String email;
    
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

#### 4. **Repository Interface**

```java
package com.example.repository;

import com.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Custom query methods
    Optional<User> findByEmail(String email);
    
    List<User> findByNameContaining(String name);
    
    // JPQL Custom Query
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findUserByEmail(@Param("email") String email);
    
    // Native SQL Query
    @Query(value = "SELECT * FROM users WHERE name LIKE %:name%", nativeQuery = true)
    List<User> searchUsersByName(@Param("name") String name);
}
```

#### 5. **Service Layer (Using EntityManager Directly)**

```java
package com.example.service;

import com.example.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    @Autowired
    private EntityManager em;
    
    // CREATE - using persist()
    public User createUser(User user) {
        em.persist(user);  // INSERT on transaction commit
        return user;
    }
    
    // READ - using find()
    public Optional<User> getUserById(Long id) {
        User user = em.find(User.class, id);
        return Optional.ofNullable(user);
    }
    
    // UPDATE - using merge()
    public User updateUser(User user) {
        return em.merge(user);  // UPDATE on transaction commit
    }
    
    // DELETE - using remove()
    public void deleteUser(Long id) {
        User user = em.find(User.class, id);
        if (user != null) {
            em.remove(user);  // DELETE on transaction commit
        }
    }
    
    // QUERY - using createQuery()
    public List<User> getAllUsers() {
        return em.createQuery("SELECT u FROM User u", User.class)
                .getResultList();
    }
    
    // QUERY with Parameter
    public Optional<User> getUserByEmail(String email) {
        return em.createQuery(
                "SELECT u FROM User u WHERE u.email = :email", 
                User.class
        )
        .setParameter("email", email)
        .getResultStream()
        .findFirst();
    }
    
    // Native SQL Query
    public List<User> searchUsers(String keyword) {
        Query query = em.createNativeQuery(
            "SELECT * FROM users WHERE name LIKE ?1", 
            User.class
        );
        query.setParameter(1, "%" + keyword + "%");
        return query.getResultList();
    }
    
    // Flush Example
    public void batchUpdateUsers(List<User> users) {
        int count = 0;
        for (User user : users) {
            em.merge(user);
            count++;
            
            if (count % 100 == 0) {
                em.flush();  // Execute batch every 100 records
                em.clear();  // Clear memory
            }
        }
    }
    
    // Refresh Example
    public void refreshUserData(Long id) {
        User user = em.find(User.class, id);
        user.setName("Changed Locally");
        em.refresh(user);  // Reload from DB, discard local changes
    }
}
```

#### 6. **Controller Usage**

```java
package com.example.controller;

import com.example.model.User;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    // Create
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User created = userService.createUser(user);
        return ResponseEntity.ok(created);
    }
    
    // Read by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }
    
    // Read all
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    // Update
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        User updated = userService.updateUser(user);
        return ResponseEntity.ok(updated);
    }
    
    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    // Search
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String keyword) {
        List<User> users = userService.searchUsers(keyword);
        return ResponseEntity.ok(users);
    }
}
```

---

## Common Interview Questions & Answers

### Q1: Difference between persist() and merge()?

| Feature | persist() | merge() |
|---------|-----------|---------|
| **Entity State** | Transient/New | Detached |
| **Throws Exception** | Yes (if exists) | No (creates copy) |
| **Return Value** | void | Returns managed copy |
| **Use Case** | Create new records | Update detached records |
| **Example** | `em.persist(newUser)` | `em.merge(detachedUser)` |

### Q2: What is Persistence Context?

**Answer:** It's a cache that tracks managed entities between EntityManager creation and closing. All changes to managed entities are automatically detected and synchronized with the database on flush/commit.

### Q3: Difference between find() and getReference()?

| Feature | find() | getReference() |
|---------|--------|---|
| **Loading** | Eager (immediate) | Lazy (proxy) |
| **Exception** | Returns null if not found | Throws exception on access if not found |
| **Performance** | Extra DB query | No DB query initially |
| **Use Case** | Need data immediately | Establish relationships |

### Q4: What is LazyInitializationException?

**Answer:** Occurs when accessing a lazy-loaded collection after EntityManager is closed.

```java
User user = em.find(User.class, 1L);  // User fetched
em.close();
user.getOrders().size();  // ERROR: LazyInitializationException
```

### Q5: How to prevent LazyInitializationException?

**Solutions:**
1. Use eager fetching: `@OneToMany(fetch = FetchType.EAGER)`
2. Access collection before closing EntityManager
3. Use JOIN FETCH in JPQL: `SELECT u FROM User u JOIN FETCH u.orders`
4. Use Hibernate.initialize(): `Hibernate.initialize(user.getOrders())`

---

 
## Conclusion

JPA and EntityManager provide a powerful abstraction layer for database operations in Spring Boot. Key takeaways:

- **JPA** is a specification; **Hibernate** is an implementation
- **EntityManager** manages entity lifecycle through CRUD operations
- **Persistence Context** tracks changes automatically (dirty checking)
- **Transactions** ensure data consistency
- **Spring Boot** simplifies JPA configuration and integration
- Understanding entity states and lifecycle is crucial for effective ORM usage

---

## References

- [Java Persistence API Documentation](https://docs.oracle.com/javaee/7/api/javax/persistence/)
- [Hibernate Official Documentation](https://hibernate.org/orm/documentation/)
- [Spring Data JPA Reference](https://spring.io/projects/spring-data-jpa)
- [JPA Best Practices](https://www.baeldung.com/jpa-guide)
