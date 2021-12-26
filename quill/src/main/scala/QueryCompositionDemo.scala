import QuillContext.dataSourceLayer
import zio.IO
import zio._
import zio.macros.accessible
import zio.magic._

import java.sql.SQLException
import javax.sql.DataSource

case class Person(id: Int, firstName: String, lastName: String, age: Int)
case class Troll(id: Int, firstName: String, lastName: String, age: Int)
case class Robot(id: Int, name: String, age: Int)
case class Address(ownerFk: Int, street: String, zip: Int, state: String)

case class UserRole(id: Int, userFk: Int, roleFk: Int)
case class Role(id: Int, name: String)
case class RolePermission(id: Int, roleFk: Int, permissionFk: Int)
case class Permission(id: Int, name: String)

@accessible
trait DataService {
  def getPerson: IO[SQLException, List[Person]]
  def personNamed(name: String): IO[SQLException, List[Person]]
}

object DataService {
  val live = (DataServiceLive.apply _).toLayer[DataService]
}

final case class DataServiceLive(dataSource: DataSource) extends DataService {
  val env = Has(dataSource)
  import QuillContext._
  def somePerson = quote {
    query[Person].filter(p => p.firstName == "Joe")
  }
  def getPerson: IO[SQLException, List[Person]] =
    run(somePerson).provide(env)
  def personNamed(name: String): IO[SQLException, List[Person]] =
    run(quote { query[Person].filter(p => p.firstName == lift(name)) }).provide(env)
}

object QueryCompositionDemo extends zio.App {

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    DataService.personNamed("Joe")
      .inject(dataSourceLayer, DataService.live)
      .debug("Result")
      .exitCode

}