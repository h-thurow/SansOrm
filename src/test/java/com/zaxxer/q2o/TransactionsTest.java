package com.zaxxer.q2o;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.persistence.Entity;
import javax.sql.DataSource;
import javax.transaction.Transaction;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sansorm.DataSources.getH2DataSource;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 08.01.20
 */
@RunWith(Parameterized.class)
public class TransactionsTest {

   @Parameterized.Parameters(name = "autocommit={0}, userTx={1}")
   public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][] {
//         {true, true}, {false, true}
         {true, true}
      });
   }
   private DataSource dataSource;

   @Parameterized.Parameter(0)
   public boolean withAutoCommit;

   @Parameterized.Parameter(1)
   public boolean withUserTx;

   @Before // not @BeforeClass to have fresh table in each test, also sde
   public void setUp() throws IOException
   {
      dataSource = getH2DataSource(/*autoCommit=*/withAutoCommit);
      if (withUserTx) {
         dataSource = q2o.initializeTxSimple(dataSource);
      }
      else {
         dataSource = q2o.initializeTxNone(dataSource);
      }
      Q2Sql.executeUpdate(
         "CREATE TABLE MyObj (string VARCHAR(128))");
   }

   @After // not @AfterClass to have fresh table in each test
   public void tearDown() {
      Q2Sql.executeUpdate("DROP TABLE MyObj");
      q2o.deinitialize();
   }

   @Entity
   static class MyObj {
      String string;
   }

   @Test
   public void notEnclosedInTransaction()
   {
      try {
         MyObj o = new MyObj();
         o.string = "1";
         MyObj oInserted = Q2Obj.insert(o);

         // throws exception
         Q2Sql.executeUpdate("insert into MyObj (string, noSuchColumn) values (2, 2)");
      }
      catch (Exception ignored) { }

      List<MyObj> objs = Q2ObjList.fromSelect(MyObj.class, "select * from MyObj");
      assertThat(objs).hasSize(1);
   }

   @Test
   public void notEnclosedInTransaction2()
   {
      try {
         MyObj o = new MyObj();
         o.string = "1";
         MyObj oInserted = Q2Obj.insert(o);

         MyObj o2 = new MyObj();
         o2.string = "2";
         // Its a transactional connection. If you retrieve connections from datasource you have to manage them on your own, e. g. commit, close or rollback it.
         Connection connection = dataSource.getConnection();
         try {
            Q2Obj.insert(connection, o2);
            connection.commit();
         }
         catch (Exception e) {
            connection.rollback();
         }
         finally {
            connection.close();
         }

         // throws exception
         Q2Sql.executeUpdate("insert into MyObj (string, noSuchColumn) values (2, 2)");
      }
      catch (Exception ignored) {
      }

      List<MyObj> objs = Q2ObjList.fromSelect(MyObj.class, "select * from MyObj");
      assertThat(objs).hasSize(2);
   }

   @Test
   public void enclosedInTransaction()
   {
      try {
         TransactionHelper.beginOrJoinTransaction();

         MyObj o = new MyObj();
         o.string = "1";
         MyObj oInserted = Q2Obj.insert(o);

         MyObj o2 = new MyObj();
         o2.string = "2";
         // dataSource returns transactional connections
         Q2Obj.insert(dataSource.getConnection(), o2);

         // throws exception
         Q2Sql.executeUpdate("insert into MyObj (string, noSuchColumn) values (2, 2)");

         TransactionHelper.commit();

      }
      catch (Exception ignored) {
         TransactionHelper.rollback();
      }

      List<MyObj> objs = Q2ObjList.fromSelect(MyObj.class, "select * from MyObj");
      assertThat(objs).isEmpty();
   }

   @Test
   public void enclosedInTransaction2()
   {
      try {
         SqlClosure.sqlExecute(new SqlFunction<Void>() {
            @Override
            public Void execute(final Connection transactionalConnection) throws SQLException
            {
               MyObj o = new MyObj();
               o.string = "1";
               MyObj oInserted = Q2Obj.insert(o);

               MyObj o2 = new MyObj();
               o2.string = "2";
               // This connection is managed by q2o. Compare with notEnclosedInTransaction2().
               Q2Obj.insert(transactionalConnection, o2);

               // throws exception
               Q2Sql.executeUpdate("insert into MyObj (string, noSuchColumn) values (2, 2)");
               return null;
            }
         });
      }
      catch (Exception ignored) { }

      List<MyObj> objs = Q2ObjList.fromSelect(MyObj.class, "select * from MyObj");
      assertThat(objs).isEmpty();
   }

   @Test
   public void suspendTransaction()
   {
      MyObj o2 = null;
      try {
         TransactionHelper.beginOrJoinTransaction();

         MyObj o = new MyObj();
         o.string = "1";
         MyObj oInserted = Q2Obj.insert(o);

         Transaction tx = TransactionHelper.suspend();

         // This object will be stored.
         o2 = new MyObj();
         o2.string = "2";
//         Q2Obj.insert(dataSource.getConnection(), o2);
         Q2Obj.insert(o2);

         TransactionHelper.resume(tx);

         // throws exception
         Q2Sql.executeUpdate("insert into MyObj (string, noSuchColumn) values (2, 2)");

         TransactionHelper.commit();

      }
      catch (Exception ignored) {
         ignored.printStackTrace();
         TransactionHelper.rollback();
      }

      List<MyObj> objs = Q2ObjList.fromSelect(MyObj.class, "select * from MyObj");
      assertThat(objs).hasSize(1).first().isEqualToComparingFieldByField(o2);
   }

//   @Test
//   public void suspendTransaction()
//   {
//      try {
//         TransactionHelper.beginOrJoinTransaction();
//         Transaction tx = TransactionHelper.suspend();
//         TransactionHelper.resume(tx);
//         TransactionHelper.commit();
//      }
//      catch (Exception e) {
//         e.printStackTrace();
//         TransactionHelper.rollback();
//      }
//
//   }
}