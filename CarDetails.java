public class CarDetails {
    public final String make;
    public final String model;
    public final int year;
    public final double mileage;

    // The required constructor to fix the previous error
    public CarDetails(String make, String model, int year, double mileage) {
        this.make = make;
        this.model = model;
        this.year = year;
        this.mileage = mileage;
    }

    @Override
    public String toString() {
        return make + " " + model + " (" + year + "), " + (int)mileage + " km";
    }
}