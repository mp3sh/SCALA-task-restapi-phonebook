package task.restapi

import akka.actor.{Actor, PoisonPill, Props}

import scala.annotation.tailrec
import scala.io.Source

object Reader {
  def props = Props(new Reader)

  case object GetAll
}

class Reader extends Actor {
  import Help._
  import Reader._

  def receive = {
    case GetAll =>
      @tailrec
      def toUsersType(vec: Vector[String], inc: Int = 0, users: Vector[User] = Vector.empty[User]): Vector[User] = {
        if (inc == vec.size) users
        else {
          val u = vec(inc).split(" ")
          val user = User(IdUser(u(0).toInt), InfoUser(u(1), u(2)))
          toUsersType(vec, inc + 1, users :+ user)
        }
      }

      val source = Source.fromFile(path)
      val users = source.getLines.toVector
      source.close()

      sender() ! Users(toUsersType(users))
      self ! PoisonPill
  }

}
