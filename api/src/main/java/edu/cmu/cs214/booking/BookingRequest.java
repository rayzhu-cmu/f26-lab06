package edu.cmu.cs214.booking;

/**
 * Immutable arguments for creating a booking.
 *
 * @param roomId the room to book, non-null
 * @param startMinute first minute of the booking, inclusive, from midnight
 * @param endMinute first minute after the booking, exclusive; must be greater
 *                  than {@code startMinute}
 * @param waitlistKey opaque caller key, or null to decline waitlisting on conflict
 * @param notes optional caller notes, or null; stored unchanged
 */
public record BookingRequest(String roomId, long startMinute, long endMinute,
                             String waitlistKey, String notes) {
}
