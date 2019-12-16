[![][license img]][license]
[![][Maven Central img]][Maven Central]
[![][Javadocs img]][Javadocs]

# q2o

q2o is a JPA based Java object mapper which helps you with many of the tedious SQL and JDBC ResultSet related tasks, but without all the complexity an ORM framework comes with. It offers limited support for reading object relations on demand, but it does not intend to be an ORM.

### Intention of this fork

Support not only field access but property access to. With property access the class's getters and setters are called to read or write values. With field access the fields are read and written to directly. So if you need more control over the process of reading or writing set access type explicitely with `@Access` annotation or annotate getters, not fields (do not mix the style within one class). If there is no @Access annotation found the place of the annotations decide upon the access type. With support for property access you can also let IntelliJ generate your entity classes (IntelliJ defaults to annotating getters, not fields) and use them without reworking.

Fully JPA annotated classes, you already have, should be processed as-is, without throwing exceptions due to unsupported annotations and not forcing you to change them just to make them usable with q2o. Remember q2o is not an ORM framework so only a small subset of JPA annotations are really supported (see below).

Support for reading `@OneToOne` and `@ManyToOne` relations on demand.

Spring Transaction Support.

MySQL Support (New in 3.13)

More convenient methods.

API clean-up **(still subject of change!)**. There is a SansOrm 3.7 compatibility layer.

Numerous tests added to stabilize further development.

### Initialization

First of all we need a datasource. Once you get it, call one of ```q2o.initializeXXX``` methods:
```Java
DataSource ds = ...;
q2o.initializeTxNone(ds);

// or if you want to use embedded TransactionManager implementation
q2o.initializeTxSimple(ds);

// or if you have your own TransactionManager and UserTransaction
TransactionManager tm = ...;
UserTransaction ut = ...;
q2o.initializeTxCustom(ds, tm, ut);

// Starting with V 3.12 you can make q2o Spring transaction aware
q2o.initializeWithSpringTxSupport(ds);

// From V 3.13 on you can enable MySQL support. Configure MySQL with generateSimpleParameterMetadata=true, call 
// one of the q2o.initialize* methods and activate MySQL mode:
q2o.setMySqlMode(true);
```
Even without initialization there is support for some q2o methods. This means all methods taking a Connection, PreparedStatement or ResultSet as an argument can be called without q2o having been initialized.

### Object Mapping

Take this database table:
```SQL
CREATE TABLE customer (
   customer_id INTEGER NOT NULL GENERATED BY DEFAULT AS IDENTITY,
   last_name VARCHAR(255),
   first_name VARCHAR(255),
   email VARCHAR(255)
);
```
Let's imagine a Java class that reflects the table in a straight-forward way, and contains some JPA annotations. These annotations will instruct q2o how to map objects to SQL and ResultSets to Objects:
```Java
@Entity
@Table(name = "customer")
public class Customer {
   @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
   private int id;

   @Column(name = "last_name")
   private String lastName;

   @Column(name = "first_name")
   private String firstName;

   @Column(name = "email")
   private String emailAddress;

   public Customer() {
      // no arg constuctor declaration is necessary only when other constructors are declared
   }
}
```
Here we introduce the most important q2o classes, ```Q2Obj``` and ```Q2ObjList```. Let's look at how they can help:
```Java
public List<Customer> getAllCustomers() {
   return Q2ObjList.fromClause(Customer.class, null);
}
```
As a second argument to ```Q2ObjList.fromClause()``` you can provide a where clause, to restrict the found objects:
```
Q2ObjList.fromClause(Customer.class, "id BETWEEN ? AND ?", minId, maxId)
```

Now lets store a new customer
```
Customer customer = new Customer();
customer.setFirstName = "...";
customer.setLastName = "...";
Q2Obj.insert(customer);
```
The very useful thing that happens here is that after storing the object you can immediately access its id:
```
assertTrue(customer.getId() != 0);
```
While you are working with the customer object "offline" the object might change in the database. How can you update your object so it reflects the current state?
```
customer = Q2Obj.refresh(customer)
```
Note that the returned customer object is identical with the one you supplied as argument or null in case it was deleted in the meantime.

What if your object has many fields and you only want to retrieve some of them?
```
Q2Obj.fromSelect(Customer.class, "select id, last_name from customer where id = ?", id)
```
As long as your object has the id set, you can refresh its values with ```refresh(customer)``` or change its values and update it with ```updateObject(customer)```.

There are much more useful methods like:

* ```Q2Obj.byId(Class<T> type, Object... ids)```
* ```Q2Obj.update(customer)```
* ```Q2Obj.delete(customer)```
* ```Q2Obj.fromStatement(PreparedStatement stmt, Class<T> clazz, Object... args)```
* ```Q2Obj.countFromClause(Class<T> clazz, String clause, Object... args)```

Many of these methods can also work with lists of objects. [See Javadoc.](http://javadoc.io/page/com.github.h-thurow/q2o/latest/com/zaxxer/q2o/Q2ObjList.html)

### q2o and Spring

q2o is helpful even when you depend on Spring JDBC:
```
List<Customer> customers = jdbcTemplate.query("...", new RowMapper<Customer>() {
    @Override
    public Customer mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        return Q2Obj.fromResultSet(rs, Customer.class);
    }
});
```
All q2o methods taking a Connection, PreparedStatement or ResultSet are throwing SQLExceptions, so Spring can translate them into some subtype of its DataAccessException. These methods can even be called without initialization of q2o.

Starting with V 3.12 you can initialize q2o with Spring Transaction support too. See [Initialization](#initialization) above.



### Supported Annotations

| Annotation            | Supported elements                                     | Position               |
|:--------------------- |:-------------------------------------------------------|:-----------------------|
| ``@Access``           | ``value`` (=``AccessType.PROPERTY``,``AccessType.FIELD``)           | Classes, Getters, Fields |
| ``@Column``           | ``name``, ``insertable``, ``updatable``, ``table``     | Getters, Fields |
| ``@Convert``          | ``converter``                                          | Getters, Fields |
| ``@Entity``          | ``name``                                                | Classes           |
| ``@Enumerated``       | ``value`` (=``EnumType.ORDINAL``, ``EnumType.STRING``) | Getters, Fields |
| ``@GeneratedValue``   | ``strategy`` (``GenerationType.IDENTITY`` _only_)      | Getters, Fields |
| ``@Id``               | n/a                                                    | Getters, Fields |
| ``@JoinColumn``       | ``name (supports only @OneToOne and @ManyToOne)``      | Getters, Fields |
| ``@MappedSuperclass`` | n/a                                                    | Classes           |
| ``@Table``            | ``name``                                               | Classes           |
| ``@Transient``        | n/a                                                    | Getters, Fields |


### More Advanced

[Performing Joins](https://github.com/h-thurow/q2o/wiki/Performing-Joins)<br>
[Help with raw JDBC](https://github.com/h-thurow/q2o/wiki/SqlClosure)<br>
[Automatic Data Type Conversions](https://github.com/h-thurow/q2o/wiki/Automatic-Data-Type-Conversions)<br>
[Change log](https://github.com/h-thurow/q2o/wiki/Change-log)

## Download

<pre>
&lt;dependency>
    &lt;groupId>com.github.h-thurow&lt;/groupId>
    &lt;artifactId>q2o&lt;/artifactId>
    &lt;version>3.13&lt;/version>
&lt;/dependency>
</pre>
or <a href=http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.github.h-thurow%22%20AND%20a%3A%22q2o%22>download from here</a>.


[license]:LICENSE
[license img]:https://img.shields.io/badge/license-Apache%202-blue.svg
   
[Maven Central]:https://maven-badges.herokuapp.com/maven-central/com.github.h-thurow/q2o
[Maven Central img]:https://maven-badges.herokuapp.com/maven-central/com.github.h-thurow/q2o/badge.svg
   
[Javadocs]:http://javadoc.io/doc/com.github.h-thurow/q2o
[Javadocs img]:http://javadoc.io/badge/com.github.h-thurow/q2o.svg
