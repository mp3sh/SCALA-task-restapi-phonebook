package task.restapi

import akka.actor._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import akka.pattern.ask
import spray.json._
import task.restapi.Help.InfoUser

import scala.concurrent.ExecutionContext

class RestApi(system: ActorSystem, timeout: Timeout) extends RestRoutes {
  implicit val requestTimeout = timeout
  implicit def executionContext =  system.dispatcher

  def createHelp = system.actorOf(Help.props, Help.name)
}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  import Help._
  case class Error(message: String)

  implicit val idFormat = jsonFormat1(IdUser)
  implicit val infoFormat = jsonFormat2(InfoUser)
  implicit val userFormat = jsonFormat2(User)
  implicit val usersFormat = jsonFormat1(Users)
  implicit  val errorFormat = jsonFormat1(Error)
}

trait RestRoutes extends HelpApi with JsonSupport {
  import StatusCodes._

  def routes: Route = phonebookRoute

  def phonebookRoute =
    pathPrefix("AddUser") {
      pathEndOrSingleSlash{
        post {
          entity(as[InfoUser]) { i =>
            onSuccess(addUser(i.name, i.phone)) {
              _.fold(complete(Error("Неверный формат номера телефона")))(result => complete(OK, result))}}
        }
      }
    } ~
    path("GetAllUsers") {
      get {
        onSuccess(getAllUsers) {result => complete(OK, result)}
      }
    } ~
    path("GetUserById" / IntNumber) { i =>
      get {
        onSuccess(findUser(i)) {
          _.fold(complete(Error("Пользователь не найден")))(result => complete(OK, result))}
      }
    }
}

trait HelpApi {
  import Help._

  def createHelp(): ActorRef
  val help = createHelp()

  implicit def executionContext: ExecutionContext
  implicit def requestTimeout: Timeout

  def findUser(id: Int) =
    help.ask(FindUser(id))
      .mapTo[Option[User]]

  def getAllUsers =
    help.ask(GetAllUsers)
      .mapTo[Users]

  def addUser(name: String, phone: String) =
    help.ask(AddUser(name, phone))
      .mapTo[Option[User]]
}