package edu.cmu.cs214.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The producer's own suite. It never touches the consumer module. */
class InMemoryBookingServiceTest {

    private final BookingApi api = new InMemoryBookingService();

    @Test
    @SuppressWarnings("deprecation")
    void legacyOverloadsDelegateWithTheSameArgumentsAndValidation() {
        Booking held = api.createBooking("R1", 540, 600, null);
        assertNull(held.getNotes());
        assertNull(api.createBooking("R1", 570, 630, null, "No waitlist"));
        Booking queued = api.createBooking("R1", 570, 630, "guest", "Call on arrival");
        assertEquals(BookingStatus.WAITLISTED, queued.getStatus());
        assertEquals("guest", queued.getWaitlistKey());
        assertEquals("Call on arrival", queued.getNotes());
        assertThrows(IllegalArgumentException.class,
                () -> api.createBooking(null, 540, 600, null));
        assertThrows(IllegalArgumentException.class,
                () -> api.createBooking("R1", 600, 540, null, "Invalid range"));
        assertTrue(api.cancelBooking(held.getId(), true));
        assertEquals(BookingStatus.CONFIRMED, queued.getStatus());
        assertEquals("Call on arrival", queued.getNotes());
    }

    @Test
    void invalidRequestsAreRejectedWithoutChangingTheSchedule() {
        assertThrows(IllegalArgumentException.class, () -> api.createBooking(null));
        assertThrows(IllegalArgumentException.class,
                () -> api.createBooking(new BookingRequest(null, 540, 600, null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> api.createBooking(new BookingRequest("R1", 600, 600, null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> api.createBooking(new BookingRequest("R1", 600, 540, null, null)));
        assertTrue(api.listBookings("R1").isEmpty());
    }

    @Test
    void notesAreRetainedWithoutChangingConflictOrWaitlistBehavior() {
        Booking held = api.createBooking(new BookingRequest("R1", 540, 600, null, "Projector needed"));
        assertEquals(BookingStatus.CONFIRMED, held.getStatus());
        assertEquals("Projector needed", held.getNotes());
        assertNull(held.getWaitlistKey());
        assertNull(api.createBooking(new BookingRequest("R1", 570, 630, null, "Must not waitlist")));
        Booking queued = api.createBooking(new BookingRequest("R1", 570, 630, "guest", "Call on arrival"));
        assertEquals(BookingStatus.WAITLISTED, queued.getStatus());
        assertEquals("guest", queued.getWaitlistKey());
        assertEquals("Call on arrival", queued.getNotes());
        assertTrue(api.cancelBooking(held.getId(), true));
        assertEquals(BookingStatus.CONFIRMED, queued.getStatus());
        assertEquals("Call on arrival", api.listBookings("R1").get(0).getNotes());
    }

    @Test
    void nullNotesAreSupported() {
        assertNull(api.createBooking(new BookingRequest("R1", 540, 600, null, null)).getNotes());
        assertNull(api.createBooking(new BookingRequest("R1", 600, 660, null, null)).getNotes());
    }

    @Test
    void freeRoomGivesConfirmedBooking() {
        Booking booking = api.createBooking(new BookingRequest("R1", 540, 600, null, null));

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(540, booking.getStartMinute());
    }

    @Test
    void conflictWithoutKeyReturnsNull() {
        api.createBooking(new BookingRequest("R1", 540, 600, null, null));

        assertNull(api.createBooking(new BookingRequest("R1", 570, 630, null, null)));
        assertEquals(1, api.listBookings("R1").size());
    }

    @Test
    void conflictWithKeyGoesOnWaitlist() {
        api.createBooking(new BookingRequest("R1", 540, 600, null, null));

        Booking queued = api.createBooking(new BookingRequest("R1", 570, 630, "party-of-four", null));

        assertEquals(BookingStatus.WAITLISTED, queued.getStatus());
        assertEquals("party-of-four", queued.getWaitlistKey());
    }

    @Test
    void touchingRangesDoNotConflict() {
        api.createBooking(new BookingRequest("R1", 540, 600, null, null));

        Booking next = api.createBooking(new BookingRequest("R1", 600, 660, null, null));

        assertEquals(BookingStatus.CONFIRMED, next.getStatus());
    }

    @Test
    void cancelWithNotifyPromotesTheWaitlistedBooking() {
        Booking held = api.createBooking(new BookingRequest("R1", 540, 600, null, null));
        Booking queued = api.createBooking(new BookingRequest("R1", 570, 630, "party-of-four", null));

        assertTrue(api.cancelBooking(held.getId(), true));

        assertEquals(BookingStatus.CONFIRMED, queued.getStatus());
        List<Booking> schedule = api.listBookings("R1");
        assertEquals(1, schedule.size());
        assertEquals(queued.getId(), schedule.get(0).getId());
    }
}
