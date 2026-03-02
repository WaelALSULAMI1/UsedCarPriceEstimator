import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.time.Year;

public class PriceCalculator {

    private static final String DB_URL = "jdbc:sqlite:carprices_sar.db";
    private static final String LOG_FILE = "estimation_history_sar.txt";
    private static final int NETWORKING_PORT = 8888;
    private static final int MIN_YEAR = 1980;
    private static final double MAX_MILEAGE_KM = 500_000.0;
    private static final double MIN_PRICE_FRACTION = 0.20;  // price never below 20% of base
    private static final double YEARLY_DEPRECIATION_RATE = 0.05;  // 5% per year
    private static final double MILEAGE_DEPRECIATION_PER_KM = 1.0 / 1_000_000.0;  // 1% per 10,000 km

    public double calculate(CarDetails car) throws Exception {
        validateCarDetails(car);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<Double> basePriceFuture = executor.submit(() -> getBaselinePriceFromDB(car));
        Future<Double> marketAdjustmentFuture = executor.submit(() -> getMarketAdjustmentFactor());

        double basePrice = 0.0;
        double adjustmentFactor = 0.0;

        try {
            basePrice = basePriceFuture.get();
            adjustmentFactor = marketAdjustmentFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("Multi-threading interrupted.", e);
        } catch (ExecutionException e) {
            throw new Exception("A concurrent task failed: " + e.getCause().getMessage(), e.getCause());
        } finally {
            executor.shutdown();
        }

        double priceAfterDepreciation = applyDepreciation(basePrice, car.year, car.mileage);
        double finalPrice = applyMarketAdjustment(priceAfterDepreciation, adjustmentFactor);

        logEstimationHistory(car, finalPrice);

        return finalPrice;
    }

    /**
     * Validates car year and mileage. Year must be in [1980, currentYear]; mileage in [0, 500000].
     */
    private void validateCarDetails(CarDetails car) {
        int currentYear = Year.now().getValue();
        if (car.year < MIN_YEAR || car.year > currentYear) {
            throw new IllegalArgumentException(
                String.format("Invalid year: %d. Year must be between %d and %d.", car.year, MIN_YEAR, currentYear));
        }
        if (car.mileage < 0 || car.mileage > MAX_MILEAGE_KM) {
            throw new IllegalArgumentException(
                String.format("Invalid mileage: %.0f km. Mileage must be between 0 and %.0f km.", car.mileage, MAX_MILEAGE_KM));
        }
    }

    /**
     * Applies yearly depreciation (5% per year, compound 0.95^age) and mileage depreciation
     * (1% per 10,000 km). Ensures price never goes below 20% of base price.
     */
    private double applyDepreciation(double basePrice, int year, double mileage) {
        int currentYear = Year.now().getValue();
        int age = Math.max(0, currentYear - year);
        double yearFactor = Math.pow(1.0 - YEARLY_DEPRECIATION_RATE, age);

        double mileageRate = mileage * MILEAGE_DEPRECIATION_PER_KM;  // 1% per 10,000 km
        double mileageFactor = 1.0 - mileageRate;

        double priceAfterYear = basePrice * yearFactor;
        double priceAfterMileage = priceAfterYear * mileageFactor;
        double floor = basePrice * MIN_PRICE_FRACTION;

        return Math.max(priceAfterMileage, floor);
    }

    private double applyMarketAdjustment(double price, double adjustmentFactor) {
        return price * (1.0 + adjustmentFactor);
    }

    private double getBaselinePriceFromDB(CarDetails car) throws SQLException {
        double baselinePrice = 0.0;
        String sql = "SELECT base_price FROM prices WHERE LOWER(make) = LOWER(?) AND LOWER(model) = LOWER(?)";
        System.out.println("DB: Starting database query...");

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, car.make);
            pstmt.setString(2, car.model);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    baselinePrice = rs.getDouble("base_price");
                    System.out.printf("DB: Baseline Price found: SAR %,.2f\n", baselinePrice);
                } else {
                    throw new SQLException("Base price data not found for " + car.make + " " + car.model + " (SAR)");
                }
            }
        }
        return baselinePrice;
    }

    private double getMarketAdjustmentFactor() throws IOException {
        double adjustment = 0.0;
        String host = "127.0.0.1";
        int port = NETWORKING_PORT;

        System.out.println("NETWORKING: Connecting to market data server on port " + port + "...");

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String response = in.readLine();
            if (response != null) {
                adjustment = Double.parseDouble(response);
                System.out.printf("NETWORKING: Market Adjustment Factor received: %.2f%%\n", adjustment * 100);
            } else {
                throw new IOException("Server returned an empty response.");
            }
        }
        return adjustment;
    }

    public void logEstimationHistory(CarDetails car, double price) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            String logEntry = String.format("%s - Estimated Price: SAR %,.2f", car.toString(), price);
            out.println(logEntry);
            System.out.println("\nIOSTREAMS: Log entry saved to " + LOG_FILE);
        }
    }

    public static void readEstimationLog() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("  - " + line);
            }
        }
    }

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             java.sql.Statement stmt = conn.createStatement()) {

            String createTableSQL = "CREATE TABLE IF NOT EXISTS prices (" +
                    "make TEXT NOT NULL," +
                    "model TEXT NOT NULL," +
                    "base_price REAL NOT NULL," +
                    "PRIMARY KEY (make, model)" +
                    ");";
            stmt.execute(createTableSQL);

            String insertDataSQL = "INSERT OR REPLACE INTO prices (make, model, base_price) VALUES (?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(insertDataSQL)) {

                insertCarData(pstmt, "Toyota", "Camry", 105000.0);
                insertCarData(pstmt, "Toyota", "Corolla", 88000.0);
                insertCarData(pstmt, "Toyota", "Land Cruiser", 280000.0);
                insertCarData(pstmt, "Toyota", "Yaris", 65000.0);
                insertCarData(pstmt, "Toyota", "Hilux", 115000.0);

                insertCarData(pstmt, "Honda", "Civic", 93750.0);
                insertCarData(pstmt, "Honda", "Accord", 110000.0);
                insertCarData(pstmt, "Honda", "CRV", 125000.0);
                insertCarData(pstmt, "Honda", "Pilot", 145000.0);

                insertCarData(pstmt, "Hyundai", "Elantra", 75000.0);
                insertCarData(pstmt, "Hyundai", "Sonata", 90000.0);
                insertCarData(pstmt, "Hyundai", "Tucson", 105000.0);
                insertCarData(pstmt, "Hyundai", "Accent", 55000.0);

                insertCarData(pstmt, "Nissan", "Sentra", 80000.0);
                insertCarData(pstmt, "Nissan", "Altima", 100000.0);
                insertCarData(pstmt, "Nissan", "Patrol", 250000.0);
                insertCarData(pstmt, "Nissan", "Kicks", 72000.0);

                insertCarData(pstmt, "Ford", "Taurus", 120000.0);
                insertCarData(pstmt, "Ford", "Explorer", 155000.0);
                insertCarData(pstmt, "Ford", "Mustang", 185000.0);

            }
            System.out.println("DB: Database initialized and 20 sample cars loaded (SAR).");

        } catch (SQLException e) {
            System.err.println("DB INITIALIZATION ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void insertCarData(PreparedStatement pstmt, String make, String model, double price) throws SQLException {
        pstmt.setString(1, make);
        pstmt.setString(2, model);
        pstmt.setDouble(3, price);
        pstmt.executeUpdate();
    }
}