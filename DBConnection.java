package test;

import java.util.List;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * DashboardFrame — extended UI:
 * - Top cards: Total Spots / Occupied / Available
 * - Left: Add Vehicle form (plate, type, Park button)
 * - Center: Spot grid (A1..A50). Click occupied spot to checkout; click empty to park.
 * - Bottom: History table (Plate, Type, Spot, Entry time, Exit Time, Price)
 *
 * Integrates with occupancy history for AnalyticsFrame.
 */
public class DashboardFrame extends JFrame {
    // Top status labels (cards)
    private final JLabel totalSpotsValue = new JLabel("", SwingConstants.CENTER);
    private final JLabel occupiedValue = new JLabel("", SwingConstants.CENTER);
    private final JLabel availableValue = new JLabel("", SwingConstants.CENTER);

    // Parking spots
    private final String[] spotNames;
    private final JButton[] spotButtons;

    // Occupancy tracking & analytics
    private int currentOccupancy = 0;
    private final List<Integer> occupancyHistory = new ArrayList<>();
    private final List<LocalDateTime> occupancyTimestamps = new ArrayList<>();

    // Parked vehicles: plate -> VehicleRecord
    private final Map<String, VehicleRecord> parkedVehicles = new LinkedHashMap<>();

    // Table
    private final DefaultTableModel historyModel;

    // formatting & fees
    private final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter TIME_ONLY_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private final double HOURLY_RATE = 20.0;

    public DashboardFrame() {
        
        setTitle("Parking Control Panel");
        setSize(1200, 820);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        // configure spot count and names (50 spots)
        int spotCount = 50;
        spotNames = new String[spotCount];
        for (int i = 0; i < spotCount; i++) spotNames[i] = "A" + (i + 1);
        spotButtons = new JButton[spotCount];

        // initialize occupancy history with zero
        occupancyHistory.add(0);
        occupancyTimestamps.add(LocalDateTime.now());

        // TOP: status cards + small action buttons (analytics/logout)
        add(createTopPanel(), BorderLayout.NORTH);

        // CENTER: left add-vehicle card + center spot grid
        add(createCenterPanel(), BorderLayout.CENTER);

        // BOTTOM: history table
        historyModel = new DefaultTableModel(new String[]{"Plate", "Type", "Spot", "Entry time", "Exit Time", "Price"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        add(createHistoryTablePanel(), BorderLayout.SOUTH);

        // initialize counts displayed
        updateCounts();

        // show
        setVisible(true);
    }

    private JPanel createTopPanel() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(8, 12, 0, 12));

        // cards panel
        JPanel cards = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 6));
        cards.setOpaque(false);

        cards.add(makeStatCard("Total Spots", String.valueOf(spotNames.length), totalSpotsValue));
        cards.add(makeStatCard("Occupied", "0", occupiedValue));
        cards.add(makeStatCard("Available", String.valueOf(spotNames.length), availableValue));

        top.add(cards, BorderLayout.WEST);

        // right-side small actions: Analytics, Logout
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        rightActions.setOpaque(false);
        JButton analyticsBtn = new JButton("View Analytics");
        JButton logoutBtn = new JButton("Logout");
        analyticsBtn.addActionListener(e -> new AnalyticsFrame(new ArrayList<>(occupancyHistory), new ArrayList<>(occupancyTimestamps), spotNames.length).setVisible(true));
        logoutBtn.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            this.dispose();
        });
        rightActions.add(analyticsBtn);
        rightActions.add(logoutBtn);
        top.add(rightActions, BorderLayout.EAST);
        
        

        return top;
    }

    private JPanel makeStatCard(String title, String initialValue, JLabel valueLabel) {
        JPanel card = new JPanel();
        card.setPreferredSize(new Dimension(140, 64));
        card.setLayout(new BorderLayout());
        card.setBackground(new Color(220, 220, 255));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 190, 230)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(new Font("SansSerif", Font.PLAIN, 12));
        valueLabel.setText(initialValue);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setForeground(new Color(40, 40, 120));
        card.add(t, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Left: Add Vehicle card
        center.add(createAddVehicleCard(), BorderLayout.WEST);

        // Center: Spots panel
        center.add(createSpotsPanel(), BorderLayout.CENTER);

        return center;
    }

    private JPanel createAddVehicleCard() {
        JPanel card = new JPanel();
        card.setPreferredSize(new Dimension(320, 280));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 190, 230)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setBackground(new Color(240, 240, 255));

        JLabel title = new JLabel("Add Vehicle");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField plateField = new JTextField();
        plateField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        plateField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        plateField.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JLabel plateLabel = new JLabel("Plate:");
        plateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel typeLabel = new JLabel("Type:");
        typeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        String[] types = {"Car", "Motorbike", "Truck", "Other"};
        JComboBox<String> typeCombo = new JComboBox<>(types);
        typeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JButton parkBtn = new JButton("Park Vehicle");
        parkBtn.setBackground(new Color(60, 120, 230));
        parkBtn.setForeground(Color.WHITE);
        parkBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        parkBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        // action: park into first available spot
        parkBtn.addActionListener(e -> {
            String plate = plateField.getText();
            if (plate == null) plate = "";
            plate = plate.trim().toUpperCase();
            if (plate.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter plate number", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (parkedVehicles.containsKey(plate)) {
                JOptionPane.showMessageDialog(this, "This plate is already parked.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // find first available spot
            int idx = firstAvailableSpotIndex();
            if (idx < 0) {
                JOptionPane.showMessageDialog(this, "No available spots.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String type = (String) typeCombo.getSelectedItem();
            LocalDateTime entry = LocalDateTime.now();
            String spot = spotNames[idx];
            // assign
            parkedVehicles.put(plate, new VehicleRecord(plate, type, spot, entry));
            markSpotOccupied(idx, plate);
            addHistoryRow(plate, type, spot, entry.format(TIMESTAMP_FMT), "", "");
            updateCounts();
            updateOccupancy(1);
            plateField.setText("");
        });

        // small note area
        JLabel note = new JLabel("<html><i>Enter plate and select type, then Park Vehicle.<br/>You can also click a spot to park or checkout.</i></html>");
        note.setFont(new Font("SansSerif", Font.PLAIN, 11));
        note.setForeground(Color.DARK_GRAY);

        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(plateLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(plateField);
        card.add(Box.createVerticalStrut(10));
        card.add(typeLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(typeCombo);
        card.add(Box.createVerticalStrut(12));
        card.add(parkBtn);
        card.add(Box.createVerticalStrut(12));
        card.add(note);

        return card;
    }

    private JPanel createSpotsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JLabel areaTitle = new JLabel("Parking Spots");
        areaTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        panel.add(areaTitle, BorderLayout.NORTH);

        // Create grid with configurable columns and computed rows
        int cols = 10; // 10 columns x 5 rows => 50 spots
        int rows = (int) Math.ceil((double) spotNames.length / cols);
        JPanel grid = new JPanel(new GridLayout(rows, cols, 10, 10));
        grid.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        for (int i = 0; i < spotNames.length; i++) {
            JButton b = new JButton(spotNames[i]);
            b.setBackground(new Color(220, 230, 255));
            b.setFont(new Font("SansSerif", Font.BOLD, 12));
            b.setPreferredSize(new Dimension(90, 48));
            final int idx = i;
            b.addActionListener(e -> onSpotClicked(idx));
            spotButtons[i] = b;
            grid.add(b);
        }

        // wrap grid in a scroll pane so 50 buttons fit nicely
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane createHistoryTablePanel() {
        JTable table = new JTable(historyModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(1160, 220));
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 220)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        return scroll;
    }

    private void onSpotClicked(int idx) {
        JButton b = spotButtons[idx];
        String spot = spotNames[idx];
        // check if occupied: if the button's client property "plate" exists
        Object plateObj = b.getClientProperty("plate");
        if (plateObj != null) {
            String plate = (String) plateObj;
            // prompt to checkout
            int opt = JOptionPane.showConfirmDialog(this, "Spot " + spot + " occupied by " + plate + ".\nCheckout?", "Checkout", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                checkoutPlate(plate);
            }
        } else {
            // empty: prompt for plate to park into this spot
            String plate = JOptionPane.showInputDialog(this, "Enter plate to park into " + spot + ":");
            if (plate != null) {
                plate = plate.trim().toUpperCase();
                if (plate.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Plate cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (parkedVehicles.containsKey(plate)) {
                    JOptionPane.showMessageDialog(this, "This plate is already parked.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String type = (String) JOptionPane.showInputDialog(this, "Select type", "Vehicle Type",
                        JOptionPane.PLAIN_MESSAGE, null, new String[]{"Car", "Motorbike", "Truck", "Other"}, "Car");
                if (type == null) return;
                LocalDateTime entry = LocalDateTime.now();
                parkedVehicles.put(plate, new VehicleRecord(plate, type, spot, entry));
                markSpotOccupied(idx, plate);
                addHistoryRow(plate, type, spot, entry.format(TIMESTAMP_FMT), "", "");
                updateCounts();
                updateOccupancy(1);
            }
        }
    }

    private void markSpotOccupied(int idx, String plate) {
        JButton b = spotButtons[idx];
        b.setBackground(new Color(200, 50, 50));
        b.setForeground(Color.WHITE);
        b.putClientProperty("plate", plate);
        b.setText(spotNames[idx] + " (" + plate + ")");
    }

    private void markSpotFree(int idx) {
        JButton b = spotButtons[idx];
        b.setBackground(new Color(220, 230, 255));
        b.setForeground(Color.BLACK);
        b.putClientProperty("plate", null);
        b.setText(spotNames[idx]);
    }

    private int firstAvailableSpotIndex() {
        for (int i = 0; i < spotButtons.length; i++) {
            if (spotButtons[i].getClientProperty("plate") == null) return i;
        }
        return -1;
    }

    private void addHistoryRow(String plate, String type, String spot, String entry, String exit, String price) {
        historyModel.addRow(new Object[]{plate, type, spot, entry, exit, price});
    }

    private void checkoutPlate(String plate) {
        VehicleRecord rec = parkedVehicles.get(plate);
        if (rec == null) {
            JOptionPane.showMessageDialog(this, "Plate not found in parked records.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        LocalDateTime exitTime = LocalDateTime.now();
        Duration duration = Duration.between(rec.entryTime, exitTime);
        long minutes = Math.max(0, duration.toMinutes());
        double hoursCharged = Math.ceil(minutes / 60.0);
        double fee = hoursCharged * HOURLY_RATE;

        // Remove from parked map
        parkedVehicles.remove(plate);

        // free the spot button
        int idx = Arrays.asList(spotNames).indexOf(rec.spot);
        if (idx >= 0) markSpotFree(idx);

        // update history table: find the row with same plate & spot & empty exit, update exit & price
        for (int r = historyModel.getRowCount() - 1; r >= 0; r--) {
            String p = Objects.toString(historyModel.getValueAt(r, 0), "");
            String s = Objects.toString(historyModel.getValueAt(r, 2), "");
            String exit = Objects.toString(historyModel.getValueAt(r, 4), "");
            if (p.equals(plate) && s.equals(rec.spot) && (exit == null || exit.isEmpty())) {
                historyModel.setValueAt(exitTime.format(TIMESTAMP_FMT), r, 4);
                historyModel.setValueAt(String.format("$%.2f", fee), r, 5);
                break;
            }
        }

        updateCounts();
        updateOccupancy(-1);

        // show a checkout receipt dialog
        StringBuilder sb = new StringBuilder();
        sb.append("=== CHECK OUT ===\n");
        sb.append("Plate: ").append(plate).append("\n");
        sb.append("Spot: ").append(rec.spot).append("\n");
        sb.append("Entry: ").append(rec.entryTime.format(TIMESTAMP_FMT)).append("\n");
        sb.append("Exit:  ").append(exitTime.format(TIMESTAMP_FMT)).append("\n");
        sb.append(String.format("Duration: %d hr %d min\n", minutes / 60, minutes % 60));
        sb.append(String.format("TOTAL FEE: $%.2f\n", fee));
        JOptionPane.showMessageDialog(this, sb.toString(), "Checkout", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateCounts() {
        int total = spotNames.length;
        int occ = parkedVehicles.size();
        int avail = total - occ;
        totalSpotsValue.setText(String.valueOf(total));
        occupiedValue.setText(String.valueOf(occ));
        availableValue.setText(String.valueOf(avail));
    }

    private void updateOccupancy(int change) {
        // change is +1 or -1; keep currentOccupancy consistent with parkedVehicles size
        currentOccupancy = Math.max(0, parkedVehicles.size());
        // update label already handled in updateCounts
        updateCounts();

        // record to history and timestamp
        occupancyHistory.add(currentOccupancy);
        occupancyTimestamps.add(LocalDateTime.now());
        if (occupancyHistory.size() > 192) {
            occupancyHistory.remove(0);
            occupancyTimestamps.remove(0);
        }
    }

    // simple helper record
    private static class VehicleRecord {
        final String plate;
        final String type;
        final String spot;
        final LocalDateTime entryTime;

        VehicleRecord(String plate, String type, String spot, LocalDateTime entryTime) {
            this.plate = plate;
            this.type = type;
            this.spot = spot;
            this.entryTime = entryTime;
        }
    }
}