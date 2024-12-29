
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.HashMap;

public class DiagnosisApp extends JFrame {
    private HashMap<String, Double> results;
    private JTextArea resultArea;
    private JTextField diseaseField, probabilityField;

  
    private static final String DB_URL = "jdbc:sqlite:database/diagnosis.db";
    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS diagnoses (id INTEGER PRIMARY KEY AUTOINCREMENT, disease TEXT NOT NULL, probability REAL NOT NULL)";

    public DiagnosisApp() {
      
        setTitle("Diagnosis Application");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

     
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int width = getWidth();
                int height = getHeight();
           
                Color color1 = new Color(70, 130, 180); 
                Color color2 = Color.WHITE;
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, height, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, width, height);
            }
        };
        mainPanel.setLayout(new BorderLayout());
        add(mainPanel);

        results = new HashMap<>();

       
        resultArea = new JTextArea();
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        resultArea.setEditable(false);
        resultArea.setBorder(BorderFactory.createTitledBorder("Diagnosis Results"));
        resultArea.setBackground(new Color(245, 245, 245)); 
        JScrollPane scrollPane = new JScrollPane(resultArea);

       
        JPanel inputPanel = new JPanel();
        inputPanel.setOpaque(false);
        inputPanel.setLayout(new GridLayout(2, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel diseaseLabel = new JLabel("Disease Name:");
        diseaseLabel.setFont(new Font("Arial", Font.BOLD, 14));
        diseaseField = createStyledTextField();

        JLabel probabilityLabel = new JLabel("Probability:");
        probabilityLabel.setFont(new Font("Arial", Font.BOLD, 14));
        probabilityField = createStyledTextField();

        inputPanel.add(diseaseLabel);
        inputPanel.add(diseaseField);
        inputPanel.add(probabilityLabel);
        inputPanel.add(probabilityField);


        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false); 
        buttonPanel.setLayout(new FlowLayout());

        JButton addButton = createStyledButton("Add Diagnosis");
        JButton loadButton = createStyledButton("Load Results");

        buttonPanel.add(addButton);
        buttonPanel.add(loadButton);

        
        addButton.addActionListener(e -> addDiagnosisToDatabase());
        loadButton.addActionListener(e -> loadDiagnosisFromDatabase());

 
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JTextField createStyledTextField() {
        JTextField textField = new JTextField();
        textField.setFont(new Font("Arial", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createLineBorder(new Color(70, 130, 180), 2));
        return textField;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(70, 130, 180)); 
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(25, 25, 112), 2),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        return button;
    }

 
    private Connection connectToDatabase() {
        try {
            return DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

 
    private void addDiagnosisToDatabase() {
        String disease = diseaseField.getText();
        String probabilityText = probabilityField.getText();

        if (disease.isEmpty() || probabilityText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both disease and probability.");
            return;
        }

        try {
            double probability = Double.parseDouble(probabilityText);
            if (probability < 0 || probability > 1) {
                JOptionPane.showMessageDialog(this, "Probability must be between 0 and 1.");
                return;
            }

            Connection conn = connectToDatabase();
            if (conn != null) {
                String insertSQL = "INSERT INTO diagnoses (disease, probability) VALUES (?, ?)";
                try (PreparedStatement preparedStatement = conn.prepareStatement(insertSQL)) {
                    preparedStatement.setString(1, disease);
                    preparedStatement.setDouble(2, probability);
                    preparedStatement.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Diagnosis added successfully.");
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error adding diagnosis.");
                } finally {
                    conn.close();
                }
            }
        } catch (NumberFormatException | SQLException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input. Please try again.");
        }
    }

    // Load diagnosis results from the database
    private void loadDiagnosisFromDatabase() {
        Connection conn = connectToDatabase();
        if (conn != null) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM diagnoses")) {

                results.clear();
                resultArea.setText("");

                while (rs.next()) {
                    String disease = rs.getString("disease");
                    double probability = rs.getDouble("probability");
                    results.put(disease, probability);
                    resultArea.append(String.format("Disease: %s, Probability: %.2f%%\n", disease, probability * 100));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();
            stmt.execute(CREATE_TABLE_SQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new DiagnosisApp().setVisible(true));
    }
}
