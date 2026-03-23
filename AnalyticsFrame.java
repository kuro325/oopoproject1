package test;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AnalyticsFrame — shows full day/month/year (date) and time on the x-axis.
 * Date is drawn on the first line (dd MMM yyyy) and time below (HH:mm).
 * Labels are spaced automatically to avoid overlap.
 */
public class AnalyticsFrame extends JFrame {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FMT_SECONDS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TOOLTIP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AnalyticsFrame(List<Integer> history, List<LocalDateTime> timestamps, int capacity) {
        setTitle("Occupancy Analytics");
        setSize(820, 560); // taller to make room for two-line labels
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        ChartPanel chart = new ChartPanel(history, timestamps, capacity);
        add(chart, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JCheckBox smoothBox = new JCheckBox("Smooth line");
        smoothBox.setSelected(true);
        JButton exportBtn = new JButton("Export PNG");

        smoothBox.addActionListener(e -> {
            chart.setSmoothingEnabled(smoothBox.isSelected());
            chart.repaint();
        });

        exportBtn.addActionListener(e -> {
            // render chart to image and save
            BufferedImage img = new BufferedImage(chart.getWidth(), chart.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            chart.paint(g2);
            g2.dispose();

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Chart As");
            chooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
            chooser.setSelectedFile(new File("occupancy_chart.png"));
            int option = chooser.showSaveDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                try {
                    File out = chooser.getSelectedFile();
                    String path = out.getAbsolutePath();
                    if (!path.toLowerCase().endsWith(".png")) {
                        out = new File(path + ".png");
                    }
                    ImageIO.write(img, "png", out);
                    JOptionPane.showMessageDialog(this, "Chart saved to:\n" + out.getAbsolutePath());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Failed to save image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        controls.add(new JLabel("Options:"));
        controls.add(smoothBox);
        controls.add(exportBtn);
        add(controls, BorderLayout.SOUTH);
    }

    private static class ChartPanel extends JPanel {
        private List<Integer> data;
        private List<LocalDateTime> times;
        private boolean smoothingEnabled = true;
        private final int capacity;
        private final int padding = 50;
        private final int labelPadding = 70; // increased to fit two-line labels
        private final Color gridColor = new Color(220, 220, 220);
        private final Color lineColor = new Color(0, 120, 215);
        private final Color smoothLineColor = new Color(0, 160, 120);
        private final Color pointColor = new Color(200, 50, 50);
        private int hoverIndex = -1;

        ChartPanel(List<Integer> history, List<LocalDateTime> timestamps, int capacity) {
            this.data = history != null ? history : new ArrayList<>();
            this.times = timestamps != null ? timestamps : new ArrayList<>();
            this.capacity = Math.max(1, capacity);

            setBackground(Color.WHITE);
            ToolTipManager.sharedInstance().setInitialDelay(0);

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int idx = getClosestPointIndex(e.getX(), e.getY());
                    if (idx != -1 && idx < data.size()) {
                        hoverIndex = idx;
                        String timeStr = times.size() > idx ? times.get(idx).format(TOOLTIP_FMT) : String.valueOf(idx);
                        setToolTipText("Time: " + timeStr + "  Value: " + data.get(idx));
                    } else {
                        hoverIndex = -1;
                        setToolTipText(null);
                    }
                    repaint(); // highlight point
                }
            });

            addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    repaint();
                }
            });
        }

        public void setSmoothingEnabled(boolean enabled) {
            this.smoothingEnabled = enabled;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Background gradient
            Paint oldPaint = g2.getPaint();
            g2.setPaint(new GradientPaint(0, 0, Color.WHITE, 0, height, new Color(245, 248, 250)));
            g2.fillRect(0, 0, width, height);
            g2.setPaint(oldPaint);

            if (data == null || data.isEmpty()) {
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("No analytics data available", width / 2 - 60, height / 2);
                g2.dispose();
                return;
            }

            int left = padding + labelPadding;
            int right = width - padding;
            int top = padding;
            int bottom = height - padding - labelPadding;

            // determine min/max (use capacity for upper bound for nicer scaling)
            int maxVal = Math.max(capacity, data.stream().mapToInt(Integer::intValue).max().orElse(capacity));
            int minVal = 0;

            // horizontal and vertical scales
            double xScale = data.size() > 1 ? (double) (right - left) / (data.size() - 1) : 1.0;
            double yScale = maxVal - minVal > 0 ? (double) (bottom - top) / (maxVal - minVal) : 1.0;

            // draw grid lines and y labels
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            int yDivisions = Math.min(maxVal, 5);
            for (int i = 0; i <= yDivisions; i++) {
                int y = bottom - (int) (i * ((double) (bottom - top) / yDivisions));
                g2.setColor(gridColor);
                g2.drawLine(left, y, right, y);
                g2.setColor(Color.DARK_GRAY);
                String yLabel = String.valueOf((int) (minVal + i * (double) (maxVal - minVal) / yDivisions));
                int labelWidth = g2.getFontMetrics().stringWidth(yLabel);
                g2.drawString(yLabel, left - labelWidth - 8, y + g2.getFontMetrics().getHeight() / 2 - 3);
            }

            // title
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            String title = "Occupancy Over Time";
            g2.drawString(title, left, top - 18);

            // decide whether to include seconds in time labels (based on span)
            boolean useSeconds = false;
            if (times.size() >= 2) {
                try {
                    Duration span = Duration.between(times.get(0), times.get(times.size() - 1));
                    if (span.toHours() < 1) {
                        useSeconds = true;
                    } else {
                        useSeconds = false;
                    }
                } catch (Exception ex) {
                    useSeconds = false;
                }
            }

            // determine label step to avoid overlap: measure the widest label (date or time) and compute spacing
            Font labelFont = new Font("SansSerif", Font.PLAIN, 11);
            g2.setFont(labelFont);
            FontMetrics fm = g2.getFontMetrics();

            // prepare a sample widest label for measurement: use first timestamp if available, otherwise generic
            String sampleDate = times.size() > 0 ? times.get(0).format(DATE_FMT) : "00 Jan 0000";
            String sampleTime = times.size() > 0 ? (useSeconds ? times.get(0).format(TIME_FMT_SECONDS) : times.get(0).format(TIME_FMT)) : "00:00";
            int maxLabelWidth = Math.max(fm.stringWidth(sampleDate), fm.stringWidth(sampleTime));
            int desiredPixelSpacing = maxLabelWidth + 10; // padding between labels

            int labelStep;
            if (data.size() <= 1) {
                labelStep = 1;
            } else {
                // number of data points that correspond to desiredPixelSpacing
                labelStep = (int) Math.max(1, Math.ceil(desiredPixelSpacing / xScale));
            }

            // x-axis labels: show date (top) and time (bottom) for the selected ticks
            int step = labelStep;
            int lineHeight = fm.getHeight();
            for (int i = 0; i < data.size(); i += step) {
                int x = left + (int) (i * xScale);
                g2.setColor(Color.DARK_GRAY);
                if (times.size() > i) {
                    LocalDateTime t = times.get(i);
                    String dateStr = t.format(DATE_FMT);
                    String timeStr = useSeconds ? t.format(TIME_FMT_SECONDS) : t.format(TIME_FMT);
                    int dateW = fm.stringWidth(dateStr);
                    int timeW = fm.stringWidth(timeStr);
                    g2.drawString(dateStr, x - dateW / 2, bottom + lineHeight);                 // top line: date
                    g2.drawString(timeStr, x - timeW / 2, bottom + lineHeight + lineHeight);   // bottom line: time
                } else {
                    String label = String.valueOf(i);
                    int w = fm.stringWidth(label);
                    g2.drawString(label, x - w / 2, bottom + lineHeight);
                }
            }

            // prepare points (smoothed if enabled)
            double[] xs = new double[data.size()];
            double[] ys = new double[data.size()];
            List<Double> valuesToPlot = new ArrayList<>(data.size());
            if (smoothingEnabled && data.size() >= 3) {
                valuesToPlot = movingAverage(data, Math.max(3, Math.min(7, data.size() / 6 + 1)));
            } else {
                for (Integer v : data) valuesToPlot.add((double) v);
            }

            for (int i = 0; i < valuesToPlot.size(); i++) {
                xs[i] = left + i * xScale;
                ys[i] = bottom - (valuesToPlot.get(i) - minVal) * yScale;
            }

            // draw line (smoothed color if smoothing, else base)
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(smoothingEnabled ? smoothLineColor : lineColor);
            for (int i = 0; i < xs.length - 1; i++) {
                g2.drawLine((int) xs[i], (int) ys[i], (int) xs[i + 1], (int) ys[i + 1]);
            }

            // draw points (original data points positions)
            g2.setColor(pointColor);
            for (int i = 0; i < data.size(); i++) {
                int x = (int) (left + i * xScale);
                int y = (int) (bottom - (data.get(i) - minVal) * yScale);
                g2.fillOval(x - 4, y - 4, 8, 8);
            }

            // highlight hovered point (if any)
            if (hoverIndex >= 0 && hoverIndex < data.size()) {
                int x = (int) (left + hoverIndex * xScale);
                int y = (int) (bottom - (data.get(hoverIndex) - minVal) * yScale);
                g2.setColor(new Color(255, 215, 0));
                g2.fillOval(x - 6, y - 6, 12, 12);
                g2.setColor(Color.BLACK);
                g2.drawOval(x - 6, y - 6, 12, 12);
            }

            // axis lines
            g2.setColor(Color.DARK_GRAY);
            g2.drawLine(left, bottom, right, bottom); // x
            g2.drawLine(left, bottom, left, top);     // y

            // final touches
            g2.dispose();
        }

        private int getClosestPointIndex(int mouseX, int mouseY) {
            if (data == null || data.isEmpty()) return -1;
            int width = getWidth();
            int left = padding + labelPadding;
            int right = width - padding;
            int top = padding;
            int bottom = getHeight() - padding - labelPadding;
            double xScale = data.size() > 1 ? (double) (right - left) / (data.size() - 1) : 1.0;
            int minDist = 12;
            int best = -1;
            for (int i = 0; i < data.size(); i++) {
                int x = (int) (left + i * xScale);
                int y = (int) (bottom - (data.get(i) * (double) (bottom - top) / Math.max(1, capacity)));
                int dx = Math.abs(mouseX - x);
                int dy = Math.abs(mouseY - y);
                if (dx <= minDist && dy <= minDist) {
                    best = i;
                    minDist = Math.max(1, Math.min(minDist, dx + dy));
                }
            }
            return best;
        }

        private List<Double> movingAverage(List<Integer> src, int window) {
            List<Double> out = new ArrayList<>();
            int w = Math.max(1, window);
            for (int i = 0; i < src.size(); i++) {
                int start = Math.max(0, i - w / 2);
                int end = Math.min(src.size() - 1, i + w / 2);
                double sum = 0;
                for (int j = start; j <= end; j++) sum += src.get(j);
                out.add(sum / (end - start + 1));
            }
            return out;
        }
    }
}