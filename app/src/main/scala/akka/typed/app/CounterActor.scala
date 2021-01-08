package akka.typed.app

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object CounterActor {

  trait CounterRequests
  case class Increment(value: Int, replyTo: ActorRef[CounterActor.IncrementResponse]) extends CounterRequests
  case class Query(replyTo: ActorRef[CounterActor.QueryResponse]) extends CounterRequests

  sealed trait CounterResponses
  case class QueryResponse(count: Int) extends CounterResponses
  case class IncrementResponse(message: String) extends CounterResponses

  def apply(): Behavior[CounterRequests] =
    countservice(0)

  def countservice(counter: Int): Behavior[CounterRequests] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessage {

        case Increment(value, from) =>
          val n = counter + value
          val response_message = s"-- Data Response -- ${value} records acknowledged from ${context.self}"
          context.log.info(s"Increment Message from ${from} with ${value} records")
          from ! IncrementResponse(response_message)
          countservice(n)

        case Query(replyTo) =>
          context.log.info(s"Servicing Query Message Request From ${replyTo}")
          replyTo ! QueryResponse(counter)
          Behaviors.same
      }
    }
  }
}