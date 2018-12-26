package com.guizmaii.aecor.study.core.booking.runtime
import java.time.Instant

import com.guizmaii.aecor.study.core.booking.event._
import scodec.Codec

object BookingWireCodecs {

  import boopickle.Default._

  implicit final val instantPickler: boopickle.Pickler[Instant] =
    boopickle.DefaultBasic.longPickler.xmap(Instant.ofEpochMilli)(_.toEpochMilli)

  implicit final val rejectionPickler: boopickle.Pickler[BookingCommandRejection] =
    boopickle.Default
      .compositePickler[BookingCommandRejection]
      .addConcreteType[BookingAlreadyExists.type]
      .addConcreteType[BookingNotFound.type]
      .addConcreteType[TooManySeats.type]
      .addConcreteType[DuplicateSeats.type]
      .addConcreteType[BookingIsNotConfirmed.type]
      .addConcreteType[BookingIsAlreadyCanceled.type]
      .addConcreteType[BookingIsAlreadyConfirmed.type]
      .addConcreteType[BookingIsAlreadySettled.type]
      .addConcreteType[BookingIsDenied.type]

  implicit final val rejectionCodec: Codec[BookingCommandRejection] =
    aecor.macros.boopickle.BoopickleCodec.codec[BookingCommandRejection]

}
