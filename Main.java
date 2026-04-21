package efc;

import efc.data.DataSeeder;
import efc.service.BookingManager;
import efc.ui.FitnessUI;

/**
 * Entry point for the Elite Fitness Club Booking System.
 *
 * Initialises the booking manager, seeds sample data,
 * then launches the interactive CLI.
 */
public class Main {

    public static void main(String[] args) {
        BookingManager bm = new BookingManager();
        DataSeeder.seed(bm);

        FitnessUI ui = new FitnessUI(bm);
        ui.launch();
    }
}
