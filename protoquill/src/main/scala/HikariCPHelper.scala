import com.typesafe.config.ConfigFactory
import zio.ZLayer
import zio.Task
import zio.ULayer
import zio.Has

import java.util.Properties
import javax.sql.DataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

/*
dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
dataSource.user=test
dataSource.password=test
dataSource.databaseName=mydb
dataSource.portNumber=5432
dataSource.serverName=localhost
*/

object HikariCPHelper {

//   val dataSourceLayer: ULayer[Has[DataSource]] =
//     DataSourceLayer.fromPrefix("quill").orDie

  val hikariDataSourceLayer: ULayer[Has[DataSource]] = ZLayer.fromEffectMany(
    Task.effect {
      val source = ConfigFactory.load
      val props = new Properties()
      props.setProperty("dataSourceClassName", source.getString("hikari.dataSourceClassName"));
      props.setProperty("dataSource.serverName", source.getString("hikari.dataSource.serverName"));
      props.setProperty("dataSource.portNumber", source.getString("hikari.dataSource.portNumber"));
      props.setProperty("dataSource.user", source.getString("hikari.dataSource.user"));
      props.setProperty("dataSource.password", source.getString("hikari.dataSource.password"));
      props.setProperty("dataSource.databaseName", source.getString("hikari.dataSource.databaseName"));
      val config = new HikariConfig(props)
      new HikariDataSource(config).asInstanceOf[DataSource]
    }.map(ds => Has(ds))
  ).orDie

}
