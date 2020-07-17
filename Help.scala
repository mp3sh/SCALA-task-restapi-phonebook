package task.restapi

import java.nio.file.Path

import akka.actor._
import akka.util.Timeout

import scala.io.Source
import scala.util.matching.Regex

object Help {
  def props(implicit timeout: Timeout) = Props(new Help)
  def name = "help"
  val path = "src/main/resources/phonebook.txt"

  case class IdUser(id: Int)
  case class InfoUser(name: String, phone: String)
  case class User(id: IdUser, info: InfoUser)
  case class Users(phonebook: Vector[User])
  case class GetUser(id: Int)

  case object GetAllUsers
  case class AddUser(name: String, phone: String)
  case class FindUser(id: Int)

}

class Help(implicit timeout: Timeout) extends Actor {
  import Help._
  import context._

  def createReader = actorOf(Reader.props)

  def writeResults(words: List[String]): Path = {
    import java.nio.file.{Paths, Files, StandardOpenOption}
    import scala.collection.JavaConverters._

    val resultPath = Paths.get(path)
    Files.write(resultPath, words.asJava, StandardOpenOption.APPEND)
  }

  def receive: PartialFunction[Any, Unit] = {
    case FindUser(id) =>
      val source = Source.fromFile(path)
      val user = source.getLines.find(_.split(" ")(0) == s"$id")
      source.close()

      user match {
        case None => sender() ! None
        case Some(a) => {
          val res = a.split(" ")
          val message = User(IdUser(res(0).toInt), InfoUser(res(1), res(2)))
          sender() ! Some(message)
        }
      }

    case AddUser(name, phone) =>
      val source = Source.fromFile(path)
      val id = source.getLines.toArray.last.split(" ")(0).toInt + 1
      source.close()

      val validphone: Regex = "([0-9]+)$".r
      phone match {
        case validphone(phone) => {
          writeResults(List(s"$id $name $phone"))
          sender() ! Some(User(IdUser(id), InfoUser(name, phone)))
        }
        case _ => sender() ! None
      }

    case GetAllUsers =>
      createReader forward Reader.GetAll
  }
}
