/*
 Copyright 2012, Brett Wooldridge

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.zaxxer.q2o;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
* Encapsulates object, not SQL centric database operations. For SQL centric operations use {@link Q2Sql}.
*/
//CHECKSTYLE:OFF
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Q2Obj
{
   /**
    * Private constructor.
    */
   private Q2Obj() {
      // private constructor
   }

   // ------------------------------------------------------------------------
   //                               Read Methods
   // ------------------------------------------------------------------------

   /**
    * Load an object by it's ID.  The @Id annotated field(s) of the object is used to
    * set query parameters.
    *
    * @param connection a SQL Connection object
    * @param clazz the class of the object to load
    * @param args the query parameter used to find the object by it's ID. In case of compound keys the ids must be in the same order as they appear in the entity.
    * @param <T> the type of the object to load
    * @return the populated object (a new instance of clazz)
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T byId(Connection connection, Class<T> clazz, Object... args) throws SQLException
   {
      return OrmReader.objectById(connection, clazz, args);
   }

   /**
    * To set fields on an already existing entity.
    *
    * @param target The id field(s) must be set. The returned object is the same as was provided, but with all fields loaded from database.
    *
    */
   public static <T> T byId(Connection connection, T target) throws SQLException
   {
      return OrmReader.objectById(connection, target);
   }

   /**
    * Load an object using the specified clause.  If the specified clause contains the text
    * "WHERE" or "JOIN", the clause is appended directly to the generated "SELECT .. FROM" SQL.
    * However, if the clause contains neither "WHERE" nor "JOIN", it is assumed to be
    * just be the conditional portion that would normally appear after the "WHERE", and therefore
    * the clause "WHERE" is automatically appended to the generated "SELECT .. FROM" SQL, followed
    * by the specified clause.  For example:<p>
    * {@code User user = Q2Obj.objectFromClause(connection, User.class, "username=?", userName);}
    *
    * @param connection a SQL Connection object
    * @param clazz the class of the object to load
    * @param clause the conditional part of a SQL where clause
    * @param args the query parameters used to find the object
    * @param <T> the type of the object to load
    * @return the populated object
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T fromClause(Connection connection, Class<T> clazz, String clause, Object... args) throws SQLException
   {
      return OrmReader.objectFromClause(connection, clazz, clause, args);
   }

   /**
    * Counts the number of rows for the given query.
    *
    * @param connection a SQL connection object.
    * @param clazz the class of the object to query.
    * @param clause The conditional part of a SQL where clause.
    * @param args The query parameters used to find the list of objects.
    * @param <T> the type of object to query.
    * @return The result count.
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> int countFromClause(Connection connection, Class<T> clazz, String clause, Object... args) throws SQLException
   {
      return OrmReader.countObjectsFromClause(connection, clazz, clause, args);
   }

   /**
    * This method takes a PreparedStatement, a target class, and optional arguments to set
    * as query parameters. It sets the parameters automatically, executes the query, and
    * constructs and populates an instance of the target class. <b>The PreparedStatement will be closed.</b>
    *
    * @param stmt the PreparedStatement to execute to construct an object
    * @param clazz the class of the object to instantiate and populate with state
    * @param args optional arguments to set as query parameters in the PreparedStatement
    * @param <T> the class template
    * @return the populated object
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T fromStatement(PreparedStatement stmt, Class<T> clazz, Object... args) throws SQLException
   {
      return OrmReader.statementToObject(stmt, clazz, args);
   }

   /**
    * @see #fromResultSet(ResultSet, Object, Set)
    */
   public static <T> T fromResultSet(ResultSet resultSet, T target) throws SQLException
   {
      return OrmReader.resultSetToObject(resultSet, target);
   }

   /**
    * Get an object from the specified ResultSet. For compatibility with Spring RowMapper ResultSet.next() must been called. <b>The ResultSet is not closed as a result of this
    * method.</b>
    *
    * @param resultSet a {@link ResultSet}
    * @param target the target object to set values on
    * @param ignoredColumns the columns in the result set to ignore. Case as in name element or property name.
    * @param <T> the class template
    * @return the populated object
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T fromResultSet(ResultSet resultSet, T target, Set<String> ignoredColumns) throws SQLException
   {
      return OrmReader.resultSetToObject(resultSet, target, ignoredColumns);
   }

   // ------------------------------------------------------------------------
   //                               Write Methods
   // ------------------------------------------------------------------------

   /**
    * Insert an annotated object into the database.
    *
    * @param connection a SQL connection
    * @param target the annotated object to insert
    * @param <T> the class template
    * @return the same object that was passed in, but with possibly updated @Id field due to auto-generated keys
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T insert(Connection connection, T target) throws SQLException
   {
      return OrmWriter.insertObject(connection, target);
   }

   /**
    * Update a database row using the specified annotated object, the @Id field(s) is used in the WHERE
    * clause of the generated UPDATE statement.
    *
    * @param connection a SQL connection
    * @param target the annotated object to use to update a row in the database
    * @param <T> the class template
    * @return the same object passed in
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T update(Connection connection, T target) throws SQLException
   {
      return OrmWriter.updateObject(connection, target);
   }

   public static <T> T updateExcludeColumns(Connection connection, T target, String... excludedColumns) throws SQLException
   {
      HashSet<String> excludedCols = new HashSet<>(excludedColumns.length);
      excludedCols.addAll(Arrays.asList(excludedColumns));
      return OrmWriter.updateObject(connection, target, excludedCols);
   }

   public static <T> T updateExcludeColumns(T target, String... excludedColumns) {
      HashSet<String> excludedCols = new HashSet<>(excludedColumns.length);
      excludedCols.addAll(Arrays.asList(excludedColumns));
      return SqlClosure.sqlExecute(c -> OrmWriter.updateObject(c, target, excludedCols));
   }

   /**
    * Will only update the named column(s), ignoring all other fields.
    *
    * @param includedColumns case insensitive
    * @see #update(Connection, Object)
    */
   public static <T> T updateIncludeColumns(Connection connection, T target, String... includedColumns) throws SQLException
   {
      Introspected introspected = Introspector.getIntrospected(target.getClass());
      String[] updatableColumns = introspected.getUpdatableColumns();
      HashSet<String> excludedCols = new HashSet<>();
      for (int i = 0; i < updatableColumns.length; i++) {
         for (int j = 0; j < includedColumns.length; j++) {
            if (!updatableColumns[i].equalsIgnoreCase(includedColumns[j])) {
               excludedCols.add(updatableColumns[i]);
            }
         }
      }
      return OrmWriter.updateObject(connection, target, excludedCols);
   }

   /**
    * @see #updateIncludeColumns(Connection, Object, String...)
    */
   public static <T> T updateIncludeColumns(T target, String... includedColumns) {
      return SqlClosure.sqlExecute(c -> Q2Obj.updateIncludeColumns(c, target, includedColumns));
   }

   /**
    * Delete a database row using the specified annotated object, the @Id field(s) is used in the WHERE
    * clause of the generated DELETE statement.
    *
    * @param connection a SQL connection
    * @param target the annotated object to use to delete a row in the database
    * @param <T> the class template
    * @return 0 if no row was deleted, 1 if the row was deleted
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> int delete(Connection connection, T target) throws SQLException
   {
      return OrmWriter.deleteObject(connection, target);
   }

   /**
    * @see OrmWriter#deleteObjectById(Connection, Class, Object...)
    */
   public static <T> int deleteById(Connection connection, Class<T> clazz, Object... args) throws SQLException
   {
      return OrmWriter.deleteObjectById(connection, clazz, args);
   }

   // ------------------------------------------------------------------------
   //                             Utility Methods
   // ------------------------------------------------------------------------


   /**
    * To refresh all fields in case they have changed in database.
    *
    * @param connection a SQL connection
    * @param target an annotated object with at least all @Id fields set.
    * @return the target object with all values updated or null if the object was not found anymore.
    * @throws SQLException if a {@link SQLException} occurs
    * @param <T> the type of the target object
    */
   public static <T> T refresh(Connection connection, T target) throws SQLException {
      return OrmReader.refresh(connection, target);
   }

   public static <T> T refresh(T target) throws SQLException {
      return SqlClosure.sqlExecute(c -> Q2Obj.refresh(c, target));
   }

   /**
    * @see #byId(Connection, Class, Object...)
    */
   public static <T> T byId(Class<T> type, Object... ids)
   {
      return SqlClosure.sqlExecute(c -> Q2Obj.byId(c, type, ids));
   }

   /**
    * @see #byId(Connection, Object)
    */
   public static <T> T byId(T target)
   {
      return SqlClosure.sqlExecute(c -> OrmReader.objectById(c, target));
   }

   /**
    * Gets an object using a where clause.
    * @param type The type of the desired object.
    * @param clause The WHERE clause.
    * @param args The arguments for the WHERE clause.
    * @param <T> The type of the object.
    * @return The object or {@code null}
    * @see #fromClause(Connection, Class, String, Object...)
    */
   public static <T> T fromClause(Class<T> type, String clause, Object... args)
   {
      return SqlClosure.sqlExecute(c -> Q2Obj.fromClause(c, type, clause, args));
   }

   /**
    * Inserts the given object into the database.
    * @param object The object to insert.
    * @param <T> The type of the object.
    * @return The inserted object populated with any generated IDs.
    */
   public static <T> T insert(T object)
   {
      return SqlClosure.sqlExecute(c -> Q2Obj.insert(c, object));
   }

   /**
    * Updates the given object in the database.
    * @param object The object to update.
    * @param <T> The type of the object.
    * @return The updated object.
    */
   public static <T> T update(T object)
   {
      return SqlClosure.sqlExecute(c -> Q2Obj.update(c, object));
   }

   /**
    * @see #delete(Connection, Object)
    */
   public static <T> int delete(T object)
   {
      return SqlClosure.sqlExecute(c ->  Q2Obj.delete(c, object));
   }

   /**
    * Delete an object from the database by ID.
    * @param clazz the class of the object to delete.
    * @param args the IDs of the object, in order of appearance of declaration in the target object class.
    * @param <T> The type of the object.
    * @return the number of rows affected.
    */
   public static <T> int deleteById(Class<T> clazz, Object... args)
   {
      return SqlClosure.sqlExecute(c -> Q2Obj.deleteById(c, clazz, args));
   }

   /**
    * Counts the number of rows for the given query.
    *
    * @param clazz the class of the object to query.
    * @param clause The conditional part of a SQL where clause.
    * @param args The query parameters used to find the list of objects.
    * @param <T> the type of object to query.
    * @return The result count.
    */
   public static <T> int countFromClause(Class<T> clazz, String clause, Object... args)
   {
      return SqlClosure.sqlExecute(c -> Q2Obj.countFromClause(c, clazz, clause, args));
   }

   /**
    * To select only specified fields of an object or to perform OneToOne or ManyToOne joins.
    *
    * @param clazz the class of the object to query.
    * @param select full select ... from ... where ... statement. OneToOne and ManyToOne joins achievable.
    * @param args SQL placeholder arguments
    * @param <T> The type of object to query
    * @return The object or object graph
    */
   public static <T> T fromSelect(Class<T> clazz, String select, Object... args) {
      return SqlClosure.sqlExecute(connection -> {
         PreparedStatement stmnt = connection.prepareStatement(select);
         return Q2Obj.fromStatement(stmnt, clazz, args);
      });
   }

   /**
    * @see #fromSelect(Class, String, Object...)
    */
   public static <T> T fromSelect(Connection connection, Class<T> clazz, String select, Object... args) throws SQLException {
      PreparedStatement stmnt = connection.prepareStatement(select);
      return Q2Obj.fromStatement(stmnt, clazz, args);
   }

   public static int deleteByWhereClause(Class<?> clazz, String whereClause, Object... args) {
      return SqlClosure.sqlExecute(connection -> {
         return OrmWriter.deleteByWhereClause(connection, clazz, whereClause, args);
      });
   }

   /**
    * Deletes all objects from the corresponding table that matches the where clause.
    *
    * @param whereClause withouth "where"
    */
   public static int deleteByWhereClause(final Connection connection, Class<?> clazz, String whereClause, Object... args) throws SQLException {
      return OrmWriter.deleteByWhereClause(connection, clazz, whereClause, args);
   }

}
