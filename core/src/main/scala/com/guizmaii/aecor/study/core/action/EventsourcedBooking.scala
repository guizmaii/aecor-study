package com.guizmaii.aecor.study.core.action

import aecor.MonadActionReject
import aecor.data.{EitherK, EventsourcedBehavior}
import cats.Monad
import cats.data.NonEmptyList
import com.guizmaii.aecor.study.core.entity.Booking
import com.guizmaii.aecor.study.core.event._
import com.guizmaii.aecor.study.core.state.BookingStatus.{AwaitingConfirmation, Canceled, Confirmed, Denied, Settled}
import com.guizmaii.aecor.study.core.state._

final class EventsourcedBooking[F[_]](
    implicit F: MonadActionReject[F, Option[BookingState], BookingEvent, BookingCommandRejection]
) extends Booking[F] {

  import cats.syntax.all._

  private val ignore: F[Unit] = F.unit

  override def place(client: ClientId, concert: ConcertId, seats: NonEmptyList[Seat]): F[Unit] =
    F.read.flatMap {
      case Some(_) => F.reject(BookingAlreadyExists)
      case None =>
        if (seats.distinct =!= seats) F.reject(DuplicateSeats)
        else if (seats.size > 10) F.reject(TooManySeats)
        else F.append(BookingPlaced(client, concert, seats))
    }

  override def confirm(tickets: NonEmptyList[Ticket]): F[Unit] =
    status.flatMap {
      case AwaitingConfirmation =>
        F.append(BookingConfirmed(tickets)) >>
          F.whenA(tickets.foldMap(_.price).amount <= 0)(F.append(BookingSettled))

      case Confirmed | Settled => ignore
      case Denied              => F.reject(BookingIsDenied)
      case Canceled            => F.reject(BookingIsAlreadyCanceled)
    }

  override def deny(reason: String): F[Unit] =
    status.flatMap {
      case AwaitingConfirmation => F.append(BookingDenied(reason))
      case Denied               => ignore
      case Confirmed | Settled  => F.reject(BookingIsAlreadyConfirmed)
      case Canceled             => F.reject(BookingIsAlreadyCanceled)
    }

  override def cancel(reason: String): F[Unit] =
    status.flatMap {
      case AwaitingConfirmation | Confirmed => F.append(BookingCancelled(reason))
      case Canceled | Denied                => ignore
      case Settled                          => F.reject(BookingIsAlreadySettled)
    }

  override def receivePayment(paymentId: PaymentId): F[Unit] =
    status.flatMap {
      case AwaitingConfirmation        => F.reject(BookingIsNotConfirmed)
      case Canceled | Denied | Settled => F.reject(BookingIsAlreadySettled)
      case Confirmed                   => F.append(BookingPaid(paymentId)) >> F.append(BookingSettled)
    }

  override def status: F[BookingStatus] = F.read.flatMap {
    case Some(s) => F.pure(s.status)
    case _       => F.reject(BookingNotFound)
  }

  override def tickets: F[Option[NonEmptyList[Ticket]]] = F.read.map(_.flatMap(_.tickets))

}

object EventsourcedBooking {
  def behavior[F[_]: Monad]
    : EventsourcedBehavior[EitherK[Booking, BookingCommandRejection, ?[_]], F, Option[BookingState], BookingEvent] =
    EventsourcedBehavior
      .optionalRejectable(new EventsourcedBooking(), BookingState.init, _.handleEvent(_))
}
