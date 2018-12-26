package com.guizmaii.aecor.study.core.booking.event

import java.time.Instant

import cats.data.{NonEmptyList => NEL}
import com.guizmaii.aecor.study.core.booking.state._

sealed trait BookingEvent extends Product with Serializable

final case class BookingPlaced(clientId: ClientId, concertId: ConcertId, seats: NEL[Seat]) extends BookingEvent
final case class BookingConfirmed(tickets: NEL[Ticket], expiresAt: Option[Instant])        extends BookingEvent
final case class BookingDenied(reason: String)                                             extends BookingEvent
final case class BookingCancelled(reason: String)                                          extends BookingEvent
final case object BookingExpired                                                           extends BookingEvent
final case class BookingPaid(paymentId: PaymentId)                                         extends BookingEvent
final case object BookingSettled                                                           extends BookingEvent

sealed trait BookingCommandRejection

final case object BookingAlreadyExists      extends BookingCommandRejection
final case object BookingNotFound           extends BookingCommandRejection
final case object TooManySeats              extends BookingCommandRejection
final case object DuplicateSeats            extends BookingCommandRejection
final case object BookingIsNotConfirmed     extends BookingCommandRejection
final case object BookingIsAlreadyCanceled  extends BookingCommandRejection
final case object BookingIsAlreadyConfirmed extends BookingCommandRejection
final case object BookingIsAlreadySettled   extends BookingCommandRejection
final case object BookingIsDenied           extends BookingCommandRejection
final case object TooEarlyToExpire          extends BookingCommandRejection
