# SansORM

[![][Build Status img]][Build Status]
[![][license img]][license]
[![][Maven Central img]][Maven Central]
[![][Javadocs img]][Javadocs]

## Preface

SansOrm is a "No-ORM" sane Java-to-SQL/SQL-to-Java object mapping library. It was created out of the same conviction as expressed in articles like these:

[OrmHate](https://martinfowler.com/bliki/OrmHate.html) (by Martin Fowler)<br>
[ORM Is an Offensive Anti-Pattern](https://dzone.com/articles/orm-offensive-anti-pattern)<br>
[ORM is an anti-pattern](http://seldo.com/weblog/2011/08/11/orm_is_an_antipattern)<br>
[Object-Relational Mapping is the Vietnam of Computer Science](https://blog.codinghorror.com/object-relational-mapping-is-the-vietnam-of-computer-science/)

## Download

<pre>
&lt;dependency>
    &lt;groupId>com.github.h-thurow&lt;/groupId>
    &lt;artifactId>sansorm&lt;/artifactId>
    &lt;version>3.8&lt;/version>
&lt;/dependency>
</pre>
or <a href=http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.github.h-thurow%22%20AND%20a%3A%22sansorm%22>download from here</a>.

## Intention of this fork

Support not only field access but property access to. With property access the class's getters and setters are called to read or write values. With field access the fields are read and written to directly. So if you need more control over the process of reading or writing set access type explicitely with `@Access` annotation or annotate getters, not fields (do not mix the style within one class). If there is no @Access annotation found the place of the annotations decide upon the access type.

Fully JPA annotated classes, you already have, should be processed as-is, without throwing exceptions due to unsupported annotations and not forcing you to change them just to make them usable with SansOrm. Remember SansOrm is not an ORM frame work so only a small subset of JPA annotations are really supported (see below).

The anyway limited support for self joins was broken.

Numerous tests added to stabilize further development.

The name of the fork will propably change in the near future.

## SansOrm

SansOrm is not an ORM.  SansOrm library will...

* Massively decrease the boilerplate code you write even if you use pure SQL (and no Java objects)
* Persist and retrieve simple annotated Java objects, and lists thereof, _without you writing SQL_
* Persist and retrieve complex annotated Java objects, and lists thereof, _where you provide the SQL_

SansOrm will _never_...

* Perform a JOIN for you
* Persist a graph of objects for you
* Lazily retrieve anything for you
* Page data for you

These things that SansOrm will _never_ do are better and more efficiently performed by _you_.  SansOrm will _help_ you
do them simply, but there isn't much magic under the covers.

You could consider the philosophy of SansOrm to be SQL-first.  That is, think about a correct SQL relational schema *first*, and then once that is correct, consider how to use SansOrm to make your life easier.  In order to scale, your SQL schema design and the queries that run against it need to be efficient.  There is no way to go from an "object model" to SQL with any kind of real efficiency, due to an inherent mis-match between the "object world" and the "relational world".  As others have noted, if you truly need to develop in the currency of pure objects, then what you need is not a relational database but instead an object database.

**Note:** *SansOrm does not currently support MySQL because the MySQL JDBC driver does not return proper metadata
which is required by SansOrm for mapping.  In the future, SansOrm may support a purely 100% annotation-based type
mapping but this would merely be a concession to MySQL and in no way desirable.*

----------------------------------------------------------------

<img src="https://github.com/brettwooldridge/SansOrm/wiki/quote1.png"/>

----------------------------------------------------------------

### Initialization

First of all we need a datasource. Once you get it, call one of ```SansOrm.initializeXXX``` methods:
```Java
DataSource ds = ...;
SansOrm.initializeTxNone(ds);

// or if you want to use embedded TransactionManager implementation
SansOrm.initializeTxSimple(ds);

// or if you have your own TransactionManager and UserTransaction
TransactionManager tm = ...;
UserTransaction ut = ...;
SansOrm.initializeTxCustom(ds, tm, ut);
```
We strongly recommend using the embedded ``TransactionManager`` via the the second initializer above.  If you have an existing external ``TransactionManager``, of course you can use that.

The embedded ``TransactionManager`` conserves database Connections when nested methods are called, alleviating the need to pass ``Connection`` instances around manually. The ``TransactionManager`` uses a ``ThreadLocal`` variable to "flow" the transaction across nested calls, allowing all work to be committed as a single unit of work.

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
Let's imagine a Java class that reflects the table in a straight-forward way, and contains some JPA (javax.persistence) annotations:

Customer:
```Java
@Table(name = "customer")
public class Customer {
   @Id @GeneratedValue
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
Here we introduce the most important SansOrm class, ```OrmElf```.  What is ```OrmElf```?  Well, an 'Elf' is a 'Helper'
but with fewer letters to type.  Besides, who doesn't like Elves?  Let's look at how the ```OrmElf``` can help us:
```Java
public List<Customer> getAllCustomers() {
   return OrmElf.listFromClause(Customer.class, null);
}
```
As a second argument to ```OrmElf.listFromClause()``` you can provide a where clause, to restrict the found objects:
```
OrmElf.listFromClause(Customer.class, "id BETWEEN ? AND ?", minId, maxId)
```

Now lets store a new customer
```
Customer customer = new Customer();
customer.setFirstName = "...";
customer.setLastName = "...";
OrmElf.insertObject(customer);
```
The very useful thing that happens here is that after storing the object you can immediately access its id:
```
assertTrue(customer.getId() != 0);
```
While you are working with the customer object "offline" the object might change in the database. How can you update your object so it reflects the current state?
```
customer = OrmElf.refresh(customer)
```
Note that the returned customer object is identical with the one you supplied as argument or null in case it was deleted in the meantime.

There are much more useful methods like:

* ```OrmElf.objectById(Class<T> type, Object... ids)```
* ```OrmElf.updateObject(customer)```
* ```OrmElf.deleteObject(customer)```
* ```OrmElf.resultSetToObject(ResultSet resultSet, T target)```
* ```OrmElf.statementToObject(PreparedStatement stmt, Class<T> clazz, Object... args)```
* ```OrmElf.countObjectsFromClause(Class<T> clazz, String clause, Object... args)```

Many of these methods can also work with lists of objects.

### Supported Annotations
Except for the ``@Table`` and ``@MappedSuperclass`` annotations, which must annotate a *class*, and ``@Access`` annotation, which can annotate classes as well as fields/getters, all other annotations must appear on *member variables*.

The following annotations are supported:

| Annotation            | Supported Attributes                                 |
|:--------------------- |:---------------------------------------------------- |
| ``@Access``           | ``AccessType.PROPERTY``, ``AccessType.FIELD``        |
| ``@Column``           | ``name``, ``insertable``, ``updatable``, ``table``   |
| ``@Convert``          | ``converter`` (``AttributeConverter`` _classes only_)|
| ``@Enumerated``       | ``value`` (=``EnumType.ORDINAL``, ``EnumType.STRING``) |
| ``@GeneratedValue``   | ``strategy`` (``GenerationType.IDENTITY`` _only_)    |
| ``@Id``               | n/a                                                  |
| ``@JoinColumn``       | ``name (supports self-join only and only with @OneToOne and @ManyToOne)``             |
| ``@MappedSuperclass`` | n/a                                                  |
| ``@Table``            | ``name``                                             |
| ``@Transient``        | n/a                                                  |


### Automatic Data Type Conversions

#### Writing
When *writing* data to JDBC, SansOrm relies on the *driver* to perform most conversions.  SansOrm only calls ``Statement.setObject()`` internally, and expects that the driver will properly perform conversions.  For example, convert an ``int`` or ``java.lang.Integer`` into an ``INTEGER`` column type.

If the ``@Convert`` annotation is present on the field in question, the appropriate user-specified ``javax.persistence.AttributeConverter`` will be called. 

For fields where the ``@Enumerated`` annotation is present, SansOrm will obtain the value to persist by calling ``ordinal()`` on the ``enum`` instance in the case of ``EnumType.ORDINAL``, and ``name()`` on the ``enum`` instance in the case of ``EnumType.STRING``.

#### Reading
When *reading* data from JDBC, SansOrm relies on the *driver* to perform most conversions.  SansOrm only calls ``ResultSet.getObject()`` internally, and expects that the driver will properly perform conversions to Java types.  For example , for an ``INTEGER`` column type, return a ``java.lang.Integer`` from ``ResultSet.getObject()``.

However, if the Java object type returned by the driver *does not match* the type of the mapped member field, SansOrm permits the following automatic conversions:

| Driver ``getObject()`` Java Type | Mapped Member Java type                 |
|:-------------------------------- |:--------------------------------------- |
| ``java.lang.Integer``            | ``boolean`` (0 == ``false``, everything else ``true``)|
| ``java.math.BigDecimal``         | ``java.math.BigInteger``  |
| ``java.math.BigDecimal``         | ``int`` or ``java.lang.Integer`` (via cast)  |
| ``java.math.BigDecimal``         | ``long`` or ``java.lang.Long`` (via cast) |
| ``java.util.UUID``               | ``String``                                |
| ``java.sql.Clob``                | ``String``                                |

If the ``@Convert`` annotation is present on the field in question, the appropriate user-specified ``javax.persistence.AttributeConverter`` will be called. 

For fields where the ``@Enumerated`` annotation is present, SansOrm will map ``java.lang.Integer`` values from the driver to the correct ``Enum`` value in the case of ``EnumType.ORDINAL``, and will map ``java.lang.String`` values from the driver to the correct ``Enum`` value in the case of ``EnumType.STRING``.

Finally, SansOrm has specific support for the PostgreSQL ``PGobject`` and ``CITEXT`` data types.  ``CITEXT`` column values are converted to ``java.lang.String``.  ``PGobject`` "unknown type" column values have their ``getValue()`` method called, and the result is attempted to be set via reflection onto the mapped member field.

### More Advanced

[Performing Joins](https://github.com/h-thurow/SansOrm/wiki/Performing-Joins)<br>
[Help with raw JDBC](https://github.com/h-thurow/SansOrm/wiki/SqlClosure)


[Build Status]:https://travis-ci.org/brettwooldridge/SansOrm
[Build Status img]:https://travis-ci.org/brettwooldridge/SansOrm.svg?branch=master

[license]:LICENSE
[license img]:https://img.shields.io/badge/license-Apache%202-blue.svg
   
[Maven Central]:https://maven-badges.herokuapp.com/maven-central/com.github.h-thurow/sansorm
[Maven Central img]:https://maven-badges.herokuapp.com/maven-central/com.github.h-thurow/sansorm/badge.svg
   
[Javadocs]:http://javadoc.io/doc/com.github.h-thurow/sansorm/3.8
[Javadocs img]:http://javadoc.io/badge/com.github.h-thurow/sansorm.svg
