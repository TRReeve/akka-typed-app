package akka.typed.app

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration.DurationInt

object QueryActor {

  trait Query
  private case class Tick() extends Query
  // Wraps a message that could contain a query response. Or we could change it to contain anything
  // that extends the trait of Counter.Responses and map a whole range of responses.
  private case class QueryCounterResponse(resp: CounterActor.QueryResponse) extends Query

  def apply(counter_ref: ActorRef[CounterActor.Query]): Behavior[Query] = {
    Behaviors.setup { context =>

      /*
       * Adapter for wrapping Counter messages back to a type we can handle within this actor
       *  Notice that we haven't had to expose this actors protocol outside of the actor and all
       *  messages to do with the Counter are defined inside Counter
       **/
      val response_adapter: ActorRef[CounterActor.QueryResponse] =
        context.messageAdapter(rsp => QueryCounterResponse(rsp))

      Behaviors.withTimers { timer =>
        timer.startTimerWithFixedDelay(Tick(), 5.second)

        Behaviors.receiveMessage {
          case Tick() =>
            context.log.info(s"Sending Query Request from ${context.self}")
            counter_ref ! CounterActor.Query(response_adapter)
            Behaviors.same

          case QueryCounterResponse(resp) =>
            resp match {
              case CounterActor.QueryResponse(count) =>
                context.log.info(s"-- Query Response -- Current count is ${count}")
                Behaviors.same
              case _ => Behaviors.same
            }
          case _ =>
            Behaviors.same
        }
      }
    }
  }
}