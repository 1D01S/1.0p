import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Game extends JPanel implements MouseMotionListener, MouseWheelListener {
    private final int WIDTH = 800, HEIGHT = 600;
    private String shapeType = "CUBE"; // Начальная фигура - Куб
    private double angleX = 0, angleY = 0; // Угол поворота
    private double scale = 100; // Масштаб
    private double translateX = 0, translateY = 0; // Положение модели
    private int lastMouseX, lastMouseY; // Для отслеживания позиции мыши

    private List<double[]> vertices = new ArrayList<>(); // Вершины
    private List<int[]> faces = new ArrayList<>(); // Грань

    private JLabel polyCountLabel; // Для отображения количества полигонов
    private JButton closeModelButton; // Кнопка закрытия модели
    private boolean dragging = false; // Переменная для отслеживания перетаскивания модели

    public Game() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setFocusable(true);
        this.addMouseMotionListener(this);
        this.addMouseWheelListener(this);
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) { // Двойной клик для начала перетаскивания
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    dragging = true; // Начинаем перетаскивание
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragging) {
                    dragging = false; // Завершаем перетаскивание
                }
            }
        });

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));

        JButton switchShapeButton = new JButton("Change Shape");
        switchShapeButton.addActionListener(e -> {
            switch (shapeType) {
                case "CUBE":
                    shapeType = "PYRAMID";
                    break;
                case "PYRAMID":
                    shapeType = "TRUNCATED_PYRAMID";
                    break;
                case "TRUNCATED_PYRAMID":
                    shapeType = "SPHERE";
                    break;
                case "SPHERE":
                    shapeType = "CUBE";
                    break;
            }
            updatePolygonCount();
            repaint();
        });

        JButton loadModelButton = new JButton("Load 3D Model (.obj)");
        loadModelButton.addActionListener(e -> {
            loadModel();
            updatePolygonCount();
            repaint();
        });

        closeModelButton = new JButton("Close Model");
        closeModelButton.setEnabled(false);
        closeModelButton.addActionListener(e -> {
            closeModel();
        });

        JButton resetButton = new JButton("Reset Model");
        resetButton.addActionListener(e -> {
            resetModel();
            repaint();
        });

        polyCountLabel = new JLabel("Polygon Count: 0");

        controlPanel.add(switchShapeButton);
        controlPanel.add(loadModelButton);
        controlPanel.add(closeModelButton);
        controlPanel.add(resetButton);
        controlPanel.add(polyCountLabel);

        this.setLayout(new BorderLayout());
        this.add(controlPanel, BorderLayout.EAST);
    }

    private void closeModel() {
        vertices.clear();
        faces.clear();
        resetModel();
        closeModelButton.setEnabled(false); // Делаем кнопку закрытия снова недоступной
        repaint();
    }

    private void loadModel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("OBJ files (*.obj)", "obj"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            loadOBJ(file);
            if (!vertices.isEmpty() && !faces.isEmpty()) {
                closeModelButton.setEnabled(true); // Активируем кнопку после загрузки модели
            }
        }
    }

    private void loadOBJ(File file) {
        vertices.clear();
        faces.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("v ")) {
                    String[] parts = line.split("\\s+"); // Исправлено: использование корректного регулярного выражения
                    double[] vertex = new double[3];
                    vertex[0] = Double.parseDouble(parts[1]);
                    vertex[1] = Double.parseDouble(parts[2]);
                    vertex[2] = Double.parseDouble(parts[3]);
                    vertices.add(vertex);
                } else if (line.startsWith("f ")) {
                    String[] parts = line.split("\\s+");
                    int[] face = new int[parts.length - 1];
                    for (int i = 1; i < parts.length; i++) {
                        String[] vertexParts = parts[i].split("/");
                        face[i - 1] = Integer.parseInt(vertexParts[0]) - 1; // Индексы начинаются с 1 в OBJ
                    }
                    faces.add(face);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading OBJ file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updatePolygonCount() {
        polyCountLabel.setText("Polygon Count: " + faces.size());
    }

    private void resetModel() {
        translateX = 0;
        translateY = 0;
        angleX = 0;
        angleY = 0;
        updatePolygonCount();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (vertices.isEmpty() && faces.isEmpty()) {
            switch (shapeType) {
                case "CUBE":
                    drawCube(g);
                    break;
                case "PYRAMID":
                    drawPyramid(g);
                    break;
                case "TRUNCATED_PYRAMID":
                    drawTruncatedPyramid(g);
                    break;
                case "SPHERE":
                    drawSphere(g);
                    break;
            }
        } else {
            drawShape(g, vertices, faces);
        }
    }

    // Методы для рисования фигур
    public void drawCube(Graphics g) {
        double[][] vertices = {
                {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
                {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}
        };

        int[][] faces = {
                {4, 5, 6, 7},
                {0, 1, 2, 3},
                {0, 1, 5, 4},
                {2, 3, 7, 6},
                {0, 3, 7, 4},
                {1, 2, 6, 5}
        };

        drawShape(g, vertices, faces);
    }

    public void drawPyramid(Graphics g) {
        double[][] vertices = {
                {0, 1, 0}, {-1, -1, 1}, {1, -1, 1}, {1, -1, -1}, {-1, -1, -1}
        };

        int[][] faces = {
                {0, 1, 2}, {0, 2, 3}, {0, 3, 4}, {0, 4, 1}, {1, 2, 3, 4}
        };

        drawShape(g, vertices, faces);
    }

    public void drawTruncatedPyramid(Graphics g) {
        double[][] vertices = {
                {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1},
                {-0.5, -0.5, 0}, {0.5, -0.5, 0}, {0.5, 0.5, 0}, {-0.5, 0.5, 0}
        };

        int[][] faces = {
                {0, 1, 2, 3},
                {4, 5, 6, 7},
                {0, 1, 5, 4},
                {1, 2, 6, 5},
                {2, 3, 7, 6},
                {3, 0, 4, 7}
        };

        drawShape(g, vertices, faces);
    }

    public void drawSphere(Graphics g) {
        int radius = 1;
        int sticks = 20;
        int slices = 20;

        for (int i = 0; i < sticks; i++) {
            double theta1 = (i * Math.PI) / sticks;
            double theta2 = ((i + 1) * Math.PI) / sticks;

            for (int j = 0; j < slices; j++) {
                double phi1 = (j * 2 * Math.PI) / slices;
                double phi2 = ((j + 1) * 2 * Math.PI) / slices;

                double[][] vertices = {
                        {radius * Math.sin(theta1) * Math.cos(phi1), radius * Math.cos(theta1), radius * Math.sin(theta1) * Math.sin(phi1)},
                        {radius * Math.sin(theta1) * Math.cos(phi2), radius * Math.cos(theta1), radius * Math.sin(theta1) * Math.sin(phi2)},
                        {radius * Math.sin(theta2) * Math.cos(phi2), radius * Math.cos(theta2), radius * Math.sin(theta2) * Math.sin(phi2)},
                        {radius * Math.sin(theta2) * Math.cos(phi1), radius * Math.cos(theta2), radius * Math.sin(theta2) * Math.sin(phi1)}
                };

                int[][] faces = {
                        {0, 1, 2, 3}
                };

                drawShape(g, vertices, faces);
            }
        }
    }

    private void drawShape(Graphics g, List<double[]> vertices, List<int[]> faces) {
        double[] depths = new double[faces.size()];
        for (int i = 0; i < faces.size(); i++) {
            depths[i] = Arrays.stream(faces.get(i)).mapToDouble(v -> vertices.get(v)[2]).average().orElse(0);
        }

        Integer[] order = new Integer[depths.length];
        for (int i = 0; i < depths.length; i++) {
            order[i] = i;
        }

        Arrays.sort(order, (a, b) -> Double.compare(depths[b], depths[a]));

        for (int faceIndex : order) {
            drawFace(g, vertices, faces.get(faceIndex));
        }
    }

    private void drawShape(Graphics g, double[][] vertices, int[][] faces) {
        List<double[]> vertexList = new ArrayList<>();
        for (double[] vertex : vertices) {
            vertexList.add(vertex);
        }

        List<int[]> faceList = new ArrayList<>();
        for (int[] face : faces) {
            faceList.add(face);
        }

        drawShape(g, vertexList, faceList);
    }

    private void drawFace(Graphics g, List<double[]> vertices, int[] face) {
        Polygon polygon = new Polygon();
        int[] projX = new int[face.length];
        int[] projY = new int[face.length];

        for (int i = 0; i < face.length; i++) {
            double[] v = vertices.get(face[i]);
            double y = v[1] * Math.cos(angleX) - v[2] * Math.sin(angleX);
            double z = v[1] * Math.sin(angleX) + v[2] * Math.cos(angleX);
            double x = (v[0] + translateX) * Math.cos(angleY) + z * Math.sin(angleY); // Учитываем перемещение по X

            projX[i] = (int) (x * scale + (double) WIDTH / 2);
            projY[i] = (int) (-y * scale + (double) HEIGHT / 2 + translateY); // Учитываем перемещение по Y
            polygon.addPoint(projX[i], projY[i]);
        }

        g.setColor(Color.BLACK);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(2));
        g2d.drawPolygon(polygon);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragging) {
            angleX += (e.getY() - lastMouseY) * 0.01;
            angleY += (e.getX() - lastMouseX) * 0.01;
        } else {
            double mouseX = (e.getX() - (double) WIDTH / 2) / scale;
            double mouseY = (e.getY() - (double) HEIGHT / 2) / scale;

            // Увеличиваем влияние на Y
            double yMovementFactor = 100.0; // Множитель для Y, можно настроить по необходимости

            // Перемещение модели
            translateX += mouseX - (lastMouseX - (double) WIDTH / 2) / scale;
            translateY += (mouseY - (lastMouseY - (double) HEIGHT / 2) / scale) * yMovementFactor; // Применяем множитель к Y
        }

        lastMouseX = e.getX();
        lastMouseY = e.getY();
        repaint();
    }


    @Override
    public void mouseMoved(MouseEvent e) {
        lastMouseX = e.getX();
        lastMouseY = e.getY();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        scale += e.getWheelRotation() * 10;
        scale = Math.max(10, scale);
        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("3D Shapes");
        Game gamePanel = new Game();
        frame.add(gamePanel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

