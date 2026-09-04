# Hibernate ORM in Spring Boot
## 1. Definition

- Hibernate: an Object-Relational Mapping (ORM) framework for Java that maps Java classes to database tables and provides data query and retrieval facilities.
- JPA (Java Persistence API): a standardized specification for ORM in Java. Hibernate is a widely used implementation of JPA.


## 2. Why use Hibernate / ORM?

- Productivity: write less boilerplate SQL and JDBC plumbing code.
- Maintainability: work with Java objects rather than SQL strings sprinkled throughout the codebase.
- Portability: JPQL/HQL and the JPA API allow switching databases or providers with fewer code changes.
- Caching: built-in first-level (session) cache and optional second-level cache for performance.
- Associations: easier mapping of relationships (OneToOne, OneToMany, ManyToOne, ManyToMany).
- Transaction management: integrates with container/Framework (Spring) transactions.


## 3. Key Concepts / Terminology

- Entity: a Java class mapped to a database table using annotations like `@Entity` and `@Table`.
- Session (Hibernate) / EntityManager (JPA): main API for interacting with persistence context.
- SessionFactory / EntityManagerFactory: thread-safe factory for Sessions/EntityManagers.
- Persistence Context: set of entity instances in which for any persistent entity identity there is a unique entity instance.
- Transaction: group of operations treated as a single unit of work; typically managed by Spring or manually via API.
- Lazy vs Eager fetching: when associated data is loaded.
- Cascading: configure operations (PERSIST, MERGE, REMOVE, REFRESH) to propagate to associations.
- Entity states: Transient, Persistent, Detached, Removed.


## 4. Entity lifecycle (brief)

- Transient: created but not associated with a session/EntityManager; not in database.
- Persistent: associated with session/EntityManager and synchronized with DB on flush/commit.
- Detached: was persistent but session closed or entity evicted; changes not automatically synchronized.
- Removed: scheduled for deletion from DB on commit.


## 5. Important Hibernate / JPA classes and methods

- Session (org.hibernate.Session)
  - openSession(), get(), load(), save(), saveOrUpdate(), update(), merge(), delete(), createQuery(), beginTransaction(), getTransaction(), close().
- SessionFactory (org.hibernate.SessionFactory)
  - openSession(), getCurrentSession().
- EntityManager (javax.persistence.EntityManager)
  - persist(), find(), merge(), remove(), createQuery(), getTransaction() (if not container-managed).
- EntityManagerFactory (javax.persistence.EntityManagerFactory)
  - createEntityManager().
- Query / TypedQuery
  - setParameter(), getResultList(), getSingleResult(), executeUpdate().
- Criteria API (javax.persistence.criteria)
  - programmatic type-safe query construction.

In Spring Data JPA you usually use:
- JpaRepository<T, ID>
  - save(), findById(), findAll(), deleteById(), etc.


## 6. How Hibernate works (high-level flow)

1. Application obtains a Session/EntityManager from SessionFactory/EntityManagerFactory (in Spring Boot this is managed automatically).
2. Begin a transaction.
3. Perform operations on entities (persist, find, merge, remove, queries).
4. Hibernate tracks changes in the persistence context (first-level cache).
5. On flush/commit, Hibernate generates SQL statements and sends them to the database through JDBC.
6. Transaction is committed; optional caching updates happen; session may be closed.


## 7. Spring Boot + Hibernate typical setup

- Dependencies (Maven):

```xml
<!-- Add in pom.xml -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <scope>runtime</scope>
</dependency>
```

- application.properties (example):

```
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

Spring Boot auto-configures an EntityManagerFactory backed by Hibernate when spring-boot-starter-data-jpa is on the classpath.


## 8. Example: Entity, repository, service (Spring Boot + JPA/Hibernate)

```java
// name=src/main/java/com/example/model/User.java
package com.example.model;

import javax.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String email;

    // constructors, getters, setters
    public User() {}
    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }
    // getters and setters ...
}

// name=src/main/java/com/example/repo/UserRepository.java
package com.example.repo;

import com.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data will implement basic CRUD automatically
}

// name=src/main/java/com/example/service/UserService.java
package com.example.service;

import com.example.model.User;
import com.example.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public User createUser(User u) {
        return repo.save(u);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return repo.findById(id);
    }

    @Transactional
    public User updateUser(User u) {
        return repo.save(u);
    }

    @Transactional
    public void deleteById(Long id) {
        repo.deleteById(id);
    }
}
```

Notes: Spring Data JPA and Spring transaction management reduce boilerplate. Under the hood, repo.save(...) delegates to EntityManager.merge/persist and Hibernate turns that into SQL.


## 9. Example: Using Hibernate Session / EntityManager directly

```java
// using EntityManager
@Autowired
private EntityManager em;

public User find(Long id) {
    return em.find(User.class, id);
}

public User save(User u) {
    if (u.getId() == null) {
        em.persist(u);
        return u;
    } else {
        return em.merge(u);
    }
}

// using native Hibernate Session (if you need provider-specific API)
Session session = em.unwrap(Session.class);
session.saveOrUpdate(user);
```


## 10. Mapping associations (examples)

- OneToMany / ManyToOne example:

```java
@Entity
public class Order {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
}

@Entity
public class User {
    @Id @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders = new ArrayList<>();
}
```

Cascading and fetch type choices affect performance and behavior.


## 11. Transactions and flushing

- Flushing: synchronizing the persistence context with the DB (generating SQL). Happens automatically before commit, and may happen during queries that depend on in-memory changes.
- Use @Transactional (Spring) on service methods to define transaction boundaries.


## 12. Caching

- First-level cache: the persistence context (session) — always enabled and scoped to session/EntityManager.
- Second-level cache: optional, across sessions (providers: EHCache, Infinispan) — must be configured explicitly.


## 13. Differences: JDBC vs JPA vs Hibernate

- JDBC:
  - Low-level API to execute SQL and process ResultSet manually.
  - You manage connections, statements, transactions (unless plus a framework).
  - Pros: full control, simple for small queries. Cons: lots of boilerplate, error-prone, harder to maintain.

- JPA:
  - Specification (API) for ORM in Java: entities, entity manager, JPQL, criteria.
  - Provider implementations: Hibernate, EclipseLink, OpenJPA.
  - Pros: standardized, portable, reduces boilerplate, integrates with frameworks.

- Hibernate:
  - Implementation of JPA + provider-specific features (native API like Session, HQL, advanced caching, multi-tenancy, interceptor hooks).
  - Pros: mature, rich ecosystem and tools. Cons: abstraction may hide SQL and cause performance pitfalls if misused.

Summary:
- JDBC = raw SQL + manual mapping.
- JPA = API/spec that abstracts persistence concepts.
- Hibernate = one implementation of JPA + additional features.


## 14. Common interview questions and short answers

Q: What is the difference between persist() and merge()?
A: persist() makes a transient instance persistent and assigns an identifier (if generated) and must be used with new entities; merge() copies the state of the given object onto the persistent instance and returns a managed instance (useful for detached instances).

Q: What's the difference between get() and load()?
A: get() hits the DB immediately and returns null if not found; load() may return a proxy and only hits the DB when necessary; load() throws an exception if the entity does not exist when it is accessed.

Q: Explain lazy loading and how to avoid LazyInitializationException.
A: Lazy loading defers fetching associations until they're accessed. LazyInitializationException occurs when trying to access a lazy association outside a session. Avoid by: use DTO fetch joins, initialize within transaction, use OpenSessionInView (not recommended for many apps), or fetch eagerly where appropriate.

Q: When would you use batch inserts/updates with Hibernate?
A: For bulk operations to improve performance and reduce round trips. Configure hibernate.jdbc.batch_size and use StatelessSession or flush/clear the session regularly to avoid memory bloat.

Q: How to optimize performance using Hibernate?
A: Use projections, limit selected columns, tune fetch strategies, use joins/FetchMode, enable second-level cache where appropriate, use batch operations, avoid N+1 queries by using fetch joins or entity graphs.


## 15. Best practices

- Keep transactional boundaries at service layer.
- Avoid long-lived persistence contexts; clear/flush or use pagination for large result sets.
- Prefer DTOs for API responses to avoid exposing entities and to control fetching.
- Monitor SQL generated (spring.jpa.show-sql or loggers) and profile for N+1 queries.


## 16. Quick cheat-sheet: common methods

- save(entity) / persist(entity): insert new entity.
- update(entity) / merge(entity): update existing entity.
- saveOrUpdate(entity) (Hibernate): insert or update depending on identifier.
- find()/get(): read by id.
- createQuery("JPQL/HQL"): run queries.
- createNativeQuery(): run native SQL.
- setParameter(): bind parameters to queries.


---

This file provides a compact but thorough overview intended for interview prep and quick reference. If you'd like, I can also:
- add examples for relationships (ManyToMany, OneToOne) with join tables,
- include a section on troubleshooting common errors with stack traces,
- add a runnable Spring Boot example project structure inside this repository.

