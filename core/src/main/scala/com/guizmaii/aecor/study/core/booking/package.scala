package com.guizmaii.aecor.study.core
import aecor.runtime.Eventsourced.Entities
import com.guizmaii.aecor.study.core.booking.entity.Booking
import com.guizmaii.aecor.study.core.booking.event.BookingCommandRejection
import com.guizmaii.aecor.study.core.booking.state.BookingKey

package object booking {
  type Bookings[F[_]] = Entities.Rejectable[BookingKey, Booking, F, BookingCommandRejection]
}
