import javax.swing.SwingUtilities;

public class CarPriceEstimator {

    public static void main(String[] args) {
        PriceCalculator.initializeDatabase();
        NetworkingServer.start();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        SwingUtilities.invokeLater(() -> {
            CarPriceGUI gui = new CarPriceGUI();
            gui.setVisible(true);
        });
    }
}