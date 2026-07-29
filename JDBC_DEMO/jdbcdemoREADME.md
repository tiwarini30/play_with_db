# JDBC Demo — Deep Dive (Statement vs PreparedStatement, Transactions, Under the Hood)

This file extends the JDBC_DEMO README with focused, interview-oriented material you provided: why learn JDBC before Spring Boot, how Spring Boot uses JDBC, what happens under the hood, debugging tips, concrete Statement vs PreparedStatement implementations, SQL injection example, transaction internals, and ASCII diagrams that explain runtime structure.

---

## Why learn JDBC before Spring Boot?
- JDBC is the low-level API that directly interacts with relational databases. Higher-level frameworks (JPA, Spring Data) are built on top of JDBC or use JDBC drivers under the hood.
- Understanding JDBC helps you debug database issues, reason about performance (connection pooling, batching), and answer interview questions explaining what the frameworks abstract away.

## Does Spring Boot use JDBC internally?
- Yes. Spring Boot depends on Spring Framework abstractions (DataSource, JdbcTemplate, Spring Data) which use JDBC drivers to talk to the database. When you add `spring-boot-starter-data-jpa` or `spring-boot-starter-jdbc`, the runtime still obtains Connections and executes SQL through the database's JDBC driver.

## What happens under the hood (high-level flow)
1. Driver registration: when JVM loads a JDBC driver, the driver registers with `DriverManager` (older) or a `DataSource`/ConnectionPool is configured (modern apps).
2. Connection acquisition: application asks DriverManager or a DataSource for a Connection. The driver opens a socket / protocol session with the DB server.
3. Statement preparation: for `Statement` the driver sends SQL as-is to server for parsing/execution; for `PreparedStatement` the driver (or server) can precompile the SQL with placeholders and reuse the execution plan.
4. Parameter binding: `PreparedStatement.setX()` binds parameters safely (driver escapes/binds values) to avoid injection.
5. Execution: driver sends request, DB executes, generates result set or update count.
6. Result mapping: driver converts DB protocol rows into a `ResultSet` that the app iterates.
7. Close/return: resources closed and connections returned to pool (if using pooling).

Under-the-hood notes:
- Modern DB drivers and servers often implement server-side prepared statements: the server stores a parsed execution plan for the SQL with placeholders. The client sends only parameters on subsequent executions.
- Some drivers emulate prepared statements client-side by escaping/formatting values before sending SQL; behavior depends on the driver and configuration.

## Debugging tips (what to check when things go wrong)
- ClassNotFoundException: make sure JDBC driver dependency (e.g., `mysql-connector-java`, `org.postgresql`, `com.h2database:h2`) is on classpath.
- Connection refused: verify JDBC URL, host, port and that the DB server is reachable.
- Slow queries: capture the final SQL (for PreparedStatement, enable driver logging or use connection pool logging) and run directly in DB console with EXPLAIN.
- Resource leaks: ensure ResultSet/Statement/Connection are closed (try-with-resources). In pools, leaks often show as stale/leaked connections.
- Transaction issues: check autocommit state and isolation level; uncommitted changes will not be visible to other connections depending on isolation.

---

## Working structure diagram (ASCII)
Application-level flow (simplified):

Application
  |
  +--> DataSource / DriverManager
         |
         +--> Connection (socket/session)
                |
                +--> Statement / PreparedStatement
                       |
                       +--> DB Server (Parser -> Optimizer -> Executor)
                               |
                               +--> Result rows / update count

PreparedStatement vs Statement flow (simplified):

PreparedStatement:
App -> prepare(sql with ? placeholders) -> driver/server parses & compiles -> server stores plan
App -> set parameters -> execute -> server executes plan with bound parameters
(benefit: cheaper parsing, safer parameter handling, re-usable plan)

Statement:
App -> build full SQL string (with values inlined) -> send to server -> server parses + executes
(benefit: simple, but vulnerable to injection and extra parsing)

---

## Statement implementation (vulnerable to SQL injection)
(Using your example, slightly cleaned)

```java
// open connection
try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
    System.out.println("Connected to the database");
    insertStudent(conn, "alice", "alice@gmail.com");
} catch (SQLException e) {
    e.printStackTrace();
}

private static void insertStudent(Connection conn, String name, String email) {
    // Vulnerable: string concatenation embeds values directly into SQL
    String sql = "INSERT INTO students (name, email) VALUES('" + name + "', '" + email + "')";
    try (Statement stmt = conn.createStatement()) {
        int rows = stmt.executeUpdate(sql);
        System.out.println("Inserted: " + rows);
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
```

Example of SQL injection using Statement:
- If user passes name = "java'); DROP TABLE students; --" then the constructed SQL becomes:
  INSERT INTO students (name, email) VALUES('java'); DROP TABLE students; --', 'hack@exgmail.com')
- The DB executes the DROP TABLE statement and your table is lost.

Select example using Statement:

```java
public static void selectStudent(Connection conn) {
    String sql = "SELECT * FROM students";
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        System.out.println("Students list:");
        while (rs.next()) {
            int id = rs.getInt("id");
            String name = rs.getString("name");
            String email = rs.getString("email");
            System.out.println(id + " : " + name + " : " + email);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
```

Update and delete using Statement (same pattern) are demonstrated in your snippets but suffer same injection risk.

---

## PreparedStatement implementation (safe)
Use parameter placeholders and set methods to avoid injection and allow driver/server optimizations.

```java
private static void updateStudent(Connection conn, int id, String name, String email) {
    String sql = "UPDATE students SET name = ?, email = ? WHERE id = ?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, name);
        pstmt.setString(2, email);
        pstmt.setInt(3, id);
        int rows = pstmt.executeUpdate();
        System.out.println("Updated: " + rows);
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

private static void insertStudentSafe(Connection conn, String name, String email) {
    String sql = "INSERT INTO students (name, email) VALUES (?, ?)";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, name);
        pstmt.setString(2, email);
        int rows = pstmt.executeUpdate();
        System.out.println("Inserted (safe): " + rows);
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
```

Why PreparedStatement is safer and faster:
- Parameters are bound separately from SQL, so values cannot change the SQL structure — prevents SQL injection.
- The DB can cache the prepared execution plan and reuse it for subsequent executions with different parameters.
- Some drivers support client-side or server-side caching of prepared statements.

---

## Transactions (in detail)
A transaction is a logical unit of work composed of multiple SQL operations that must succeed or fail together (ACID properties).

Example (your code, with explanation):

```java
try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
    conn.setAutoCommit(false); // begin transaction
    try {
        int orderId = insertOrder(conn, 101, "alice01", 2000.0); // insert into orders
        insertOrderItems(conn, orderId, "laptop", 1, 2000.0);   // insert into order_items
        conn.commit(); // commit all changes atomically
        System.out.println("transaction committed successfully");
    } catch (Exception e) {
        e.printStackTrace();
        conn.rollback(); // undo partial work
        System.out.println("operation rolled back");
    } finally {
        conn.setAutoCommit(true);
    }
} catch (SQLException e) {
    e.printStackTrace();
}
```

ACID properties:
- Atomicity: either all operations in the transaction succeed or none do (commit/rollback).
- Consistency: the database moves from one valid state to another, maintaining constraints.
- Isolation: concurrent transactions do not see each other's intermediate states (isolation levels: READ_UNCOMMITTED, READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE).
- Durability: once committed, changes persist even in case of a crash.

Transaction internals / under-the-hood:
- The database uses locks, MVCC, or other concurrency control to provide isolation.
- Writes are often recorded in a transaction log (WAL) before commit for durability.
- On commit, the DB makes the changes durable and makes them visible according to the isolation semantics.

---

## Example: PreparedStatement vs Statement - side-by-side summary
- Security: PreparedStatement wins (prevents injection). Statement loses.
- Performance: PreparedStatement usually wins (precompilation & plan reuse), especially for repeated executions.
- Simplicity: Statement is slightly simpler for ad-hoc queries, but unsafe with user input.
- Flexibility: PreparedStatement supports parameter binding, batch execution, and streams for large objects.

---
