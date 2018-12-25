package com.guizmaii.aecor.study.core.action
import aecor.MonadActionReject
import cats.data.NonEmptyList
import com.guizmaii.aecor.study.core.entity.Booking
import com.guizmaii.aecor.study.core.event._
import com.guizmaii.aecor.study.core.state._

class EventsourcedBooking[F[_]](
    implicit F: MonadActionReject[F, Option[BookingState], BookingEvent, BookingCommandRejection]
) extends Booking[F] {

  import F._
  import cats.syntax.all._

  override def place(client: ClientId, concert: ConcertId, seats: NonEmptyList[Seat]): F[Unit] =
    read.flatMap {
      case Some(_) => reject(BookingAlreadyExists)
      case None =>
        if (seats.distinct =!= seats) reject(DuplicateSeats)
        else if (seats.size > 10) reject(TooManySeats)
        else append(BookingPlaced(client, concert, seats))
    }

  override def deny(reason: String): F[Unit]                   = ???
  override def cancel(reason: String): F[Unit]                 = ???
  override def receivePayment(paymentId: PaymentId): F[Unit]   = ???
  override def status: F[BookingStatus]                        = ???
  override def tickets: F[Option[NonEmptyList[Ticket]]]        = ???
  override def confirm(tickets: NonEmptyList[Ticket]): F[Unit] = ???

}
