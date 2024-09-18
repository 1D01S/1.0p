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
    private boolean isRotating = true; // Флаг для вращения куба
    private double angleX = 0, angleY = 0; // Угол поворота
    private double scale = 100; // Масштаб
    private double translateX = 0, translateY = 0; // Положение модели
    private int lastMouseX, lastMouseY; // Для отслеживания позиции мыши

    private List<double[]> vertices = new ArrayList<>(); // Вершины
    private List<int[]> faces = new ArrayList<>(); // Грань

    private JLabel polyCountLabel; // Для отображения количества полигонов
    private JLabel modelSizeLabel; // Для отображения размера модели
    private JButton closeModelButton; // Кнопка закрытия модели
    private boolean dragging = false; // Переменная для отслеживания перетаскивания модели

    public Game() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setFocusable(true);
        this.addMouseMotionListener(this);
        this.addMouseWheelListener(this);
        initializeMouseListener();
        initializeControlPanel();
        initializeCube(); // Инициализация куба

        // Запуск анимации вращения
        Timer timer = new Timer(16, e -> {
            if (isRotating) {
                angleX += 0.01; // Увеличение угла для вращения
                angleY += 0.01;
                repaint();
            }
        });
        timer.start();
    }

    private void initializeMouseListener() {
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    dragging = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
            }
        });
    }

    private void initializeControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));

        JButton loadModelButton = createLoadModelButton();
        closeModelButton = createCloseModelButton();
        JButton resetButton = createResetButton();

        polyCountLabel = new JLabel("Polygon Count: 0");
        modelSizeLabel = new JLabel("Model Size: 0.0");

        // Удалены кнопки для смены формы
        controlPanel.add(loadModelButton);
        controlPanel.add(closeModelButton);
        controlPanel.add(resetButton);
        controlPanel.add(polyCountLabel);
        controlPanel.add(modelSizeLabel);

        this.setLayout(new BorderLayout());
        this.add(controlPanel, BorderLayout.EAST);
    }

    private JButton createLoadModelButton() {
        JButton loadModelButton = new JButton("Load 3D Model (.obj)");
        loadModelButton.addActionListener(e -> {
            loadModel();
            repaint();
        });
        return loadModelButton;
    }

    private JButton createCloseModelButton() {
        JButton closeModelButton = new JButton("Close Model");
        closeModelButton.setEnabled(false);
        closeModelButton.addActionListener(e -> closeModel());
        return closeModelButton;
    }

    private JButton createResetButton() {
        JButton resetButton = new JButton("Reset Model");
        resetButton.addActionListener(e -> {
            resetModel();
            repaint();
        });
        return resetButton;
    }

    private void closeModel() {
        vertices.clear();
        faces.clear();
        initializeCube(); // Возврат к кубу
        closeModelButton.setEnabled(false);
        updatePolygonCount();
        updateModelSizeLabel(); // Обновляем размер модели
        repaint();
    }

    private void initializeCube() {
        vertices.add(new double[]{-1, -1, -1});
        vertices.add(new double[]{1, -1, -1});
        vertices.add(new double[]{1, 1, -1});
        vertices.add(new double[]{-1, 1, -1});
        vertices.add(new double[]{-1, -1, 1});
        vertices.add(new double[]{1, -1, 1});
        vertices.add(new double[]{1, 1, 1});
        vertices.add(new double[]{-1, 1, 1});

        faces.add(new int[]{0, 1, 2, 3});
        faces.add(new int[]{4, 5, 6, 7});
        faces.add(new int[]{0, 1, 5, 4});
        faces.add(new int[]{2, 3, 7, 6});
        faces.add(new int[]{0, 3, 7, 4});
        faces.add(new int[]{1, 2, 6, 5});

        updatePolygonCount(); // Обновляем количество полигонов
        updateModelSizeLabel(); // Обновляем размер модели
    }

    private void loadModel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("OBJ files (*.obj)", "obj"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            loadOBJ(file);
            closeModelButton.setEnabled(!vertices.isEmpty() && !faces.isEmpty());
        }
    }

    private void loadOBJ(File file) {
        vertices.clear();
        faces.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;
            while ((line = br.readLine()) != null) {
                processOBJLine(line);
            }
            // После загрузки модели определяем её размеры
            for (double[] v : vertices) {
                minX = Math.min(minX, v[0]);
                minY = Math.min(minY, v[1]);
                minZ = Math.min(minZ, v[2]);
                maxX = Math.max(maxX, v[0]);
                maxY = Math.max(maxY, v[1]);
                maxZ = Math.max(maxZ, v[2]);
            }
            double sizeX = maxX - minX;
            double sizeY = maxY - minY;
            double sizeZ = maxZ - minZ;
            double modelSize = Math.max(sizeX, Math.max(sizeY, sizeZ));

            // Увеличиваем модель, если она слишком мала
            if (modelSize < 3.0) { // 3.0 - минимум для удобного просмотра
                double scaleFactor = 3.0 / modelSize; // Увеличиваем до размера 3.0
                for (int i = 0; i < vertices.size(); i++) {
                    vertices.set(i, new double[]{
                            vertices.get(i)[0] * scaleFactor,
                            vertices.get(i)[1] * scaleFactor,
                            vertices.get(i)[2] * scaleFactor
                    });
                }
            }
            updatePolygonCount();
            updateModelSizeLabel(modelSize); // Обновляем текст размера модели

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading OBJ file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void processOBJLine(String line) {
        if (line.startsWith("v ")) {
            String[] parts = line.trim().split("\\s+");
            vertices.add(new double[]{
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3])
            });
        } else if (line.startsWith("f ")) {
            String[] parts = line.trim().split("\\s+");
            int[] face = Arrays.stream(parts, 1, parts.length)
                    .mapToInt(part -> Integer.parseInt(part.split("/")[0]) - 1)
                    .toArray();
            faces.add(face);
        }
    }

    private void updatePolygonCount() {
        polyCountLabel.setText("Polygon Count: " + faces.size());
    }

    private void updateModelSizeLabel() {
        modelSizeLabel.setText("Model Size: 0.0");
    }

    private void updateModelSizeLabel(double modelSize) {
        modelSizeLabel.setText(String.format("Model Size: %.2f", modelSize));
    }

    private void resetModel() {
        translateX = 0;
        translateY = 0;
        angleX = 0;
        angleY = 0;
        updatePolygonCount();
        updateModelSizeLabel();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawShape(g, vertices, faces);
    }

    private void drawShape(Graphics g, List<double[]> vertices, List<int[]> faces) {
        double[] depths = new double[faces.size()];
        Integer[] order = new Integer[faces.size()];

        // Расчет центра модели
        double centerX = 0, centerY = 0, centerZ = 0;

        for (double[] vertex : vertices) {
            centerX += vertex[0];
            centerY += vertex[1];
            centerZ += vertex[2];
        }
        int vertexCount = vertices.size();

        if (vertexCount > 0) {
            centerX /= vertexCount;
            centerY /= vertexCount;
            centerZ /= vertexCount;

            // Рисуем центр в виде красной точки, фиксированной по центру окна
            g.setColor(Color.RED);
            int centerXScreen = WIDTH / 2;
            int centerYScreen = HEIGHT / 2;
            g.fillOval(centerXScreen - 5, centerYScreen - 5, 10, 10); // Рисуем точку
        }

        for (int i = 0; i < faces.size(); i++) {
            depths[i] = Arrays.stream(faces.get(i)).mapToDouble(v -> vertices.get(v)[2]).average().orElse(0);
            order[i] = i;
        }

        Arrays.sort(order, (a, b) -> Double.compare(depths[b], depths[a]));
        Arrays.stream(order).forEach(faceIndex -> drawFace(g, vertices, faces.get(faceIndex)));
    }

    private void drawFace(Graphics g, List<double[]> vertices, int[] face) {
        Polygon polygon = new Polygon();
        for (int index : face) {
            double[] v = vertices.get(index);

            // Применяем смещение для центровки вращения
            double centeredX = v[0] - translateX; // Центрируем по X
            double centeredY = v[1];
            double centeredZ = v[2];

            // Вращение вокруг X и Y
            double y = centeredY * Math.cos(angleX) - centeredZ * Math.sin(angleX);
            double z = centeredY * Math.sin(angleX) + centeredZ * Math.cos(angleX);
            double x = (centeredX) * Math.cos(angleY) + z * Math.sin(angleY);

            // Переводим координаты для отображения
            polygon.addPoint((int) (x * scale + WIDTH / 2), (int) (-y * scale + HEIGHT / 2 + translateY));
        }
        g.setColor(Color.BLACK);
        g.drawPolygon(polygon);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragging) {
            return;
        }
        translateModel(e);
        lastMouseX = e.getX();
        lastMouseY = e.getY();
        repaint();
    }

    private void translateModel(MouseEvent e) {
        double mouseX = (e.getX() - (double) WIDTH / 2) / scale;
        double mouseY = (e.getY() - (double) HEIGHT / 2) / scale;
        double yMovementFactor = 100.0; // Множитель для Y
        translateX += mouseX - (lastMouseX - (double) WIDTH / 2) / scale;
        translateY += (mouseY - (lastMouseY - (double) HEIGHT / 2) / scale) * yMovementFactor;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        lastMouseX = e.getX();
        lastMouseY = e.getY();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        scale = Math.max(10, scale + e.getWheelRotation() * 10);
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



