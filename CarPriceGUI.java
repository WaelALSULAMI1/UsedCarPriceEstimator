import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.util.Locale;

public class CarPriceGUI extends JFrame {

    private final JTextField makeField;
    private final JTextField modelField;
    private final JTextField yearField;
    private final JTextField mileageField;
    private final JLabel resultLabel;
    private final PriceCalculator calculator;

    public CarPriceGUI() {

        calculator = new PriceCalculator();

        setTitle("Car Price Estimator (SAR)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        inputPanel.add(new JLabel("Car Make (e.g., Toyota):"));
        makeField = new JTextField(15);
        inputPanel.add(makeField);

        inputPanel.add(new JLabel("Model (e.g., Camry):"));
        modelField = new JTextField(15);
        inputPanel.add(modelField);

        inputPanel.add(new JLabel("Year (e.g., 2020):"));
        yearField = new JTextField(15);
        inputPanel.add(yearField);

        inputPanel.add(new JLabel("Mileage (in KM):"));
        mileageField = new JTextField(15);
        inputPanel.add(mileageField);

        add(inputPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton calculateButton = new JButton("Calculate Estimated Price");
        calculateButton.setFont(new Font("Arial", Font.BOLD, 14));
        calculateButton.addActionListener(this::calculateAction);
        controlPanel.add(calculateButton);

        resultLabel = new JLabel("Estimated Price: SAR 0.00");
        resultLabel.setFont(new Font("Arial", Font.BOLD, 18));
        resultLabel.setForeground(new Color(0, 102, 204));
        controlPanel.add(resultLabel);

        add(controlPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private void calculateAction(ActionEvent e) {

        String make = makeField.getText().trim();
        String model = modelField.getText().trim();
        String yearText = yearField.getText().trim();
        String mileageText = mileageField.getText().trim();

        try {
            int year = Integer.parseInt(yearText);
            double mileage = Double.parseDouble(mileageText);

            CarDetails car = new CarDetails(make, model, year, mileage);

            double estimatedPrice = calculator.calculate(car);

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "SA"));
            String formattedPrice = formatter.format(estimatedPrice);

            resultLabel.setText("Estimated Price: " + formattedPrice);

            JOptionPane.showMessageDialog(this,
                    "Estimated Price: " + formattedPrice,
                    "Price Result",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Please enter valid numbers for Year and Mileage.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Calculation Failed: " + ex.getMessage(),
                    "Process Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}