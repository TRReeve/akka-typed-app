package akka.typed.app

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration.DurationInt
import scala.util.Random

object ProducerActor {

  trait Producer
  private case class Tick() extends Producer
  // CounterActor doesn't need to be aware of this protocol. This actor can now handle a generic
  // of responses from the Counter Actor Without needing to know the details of every single
  // message counter can return.
  private case class IncrementCounterWrapper(resp: CounterActor.IncrementResponse) extends Producer

  private def random_number: Int = Random.nextInt(100)

  def apply(counter_ref: ActorRef[CounterActor.Increment]): Behavior[Producer] = {
    Behaviors.setup { context =>

      /**
       * The message in the "Counter" Service looks like
       * case class Increment(value: Int, replyTo: ActorRef[Counter.IncrementResponse]) extends CounterRequests
       *
       * Using Passing in the adapter we can change the local ActorRef of Producer to the correct type
       * for the message.
       *
       * Then in this obhect we have the IncrementCounterWrapper which wraps the expected response
       * from the server and we can match on the type. We could also use the parent Trait of Counter.IncrementResponse
       * if we wanted to handle both IncrementResponses and Query Responses. >?
       * */
      val response_adapter: ActorRef[CounterActor.IncrementResponse] =
        context.messageAdapter(rsp => IncrementCounterWrapper(rsp))

      Behaviors.withTimers { timer =>
        timer.startTimerWithFixedDelay(Tick(), 2.second)

        Behaviors.receiveMessage {
          case Tick() =>
            counter_ref ! CounterActor.Increment(random_number, response_adapter)
            Behaviors.same
          case IncrementCounterWrapper(resp) =>
            context.log.debug(resp.message)
            Behaviors.same
          case _ =>
            Behaviors.ignore
        }
      }
    }
  }
}