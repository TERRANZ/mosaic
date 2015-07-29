package ru.terra.mosaic.gui;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import ru.terra.mosaic.threading.ThreadingManager;
import ru.terra.mosaic.util.AbstractWindow;
import ru.terra.mosaic.util.AvgColor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Stream;

/**
 * Date: 21.07.15
 * Time: 16:26
 */
public class MainWindow extends AbstractWindow {
    @FXML
    public ImageView ivResult;
    @FXML
    public Label lblDir;
    @FXML
    public ListView lvPics;
    @FXML
    public Label lblStatus;
    @FXML
    public ImageView ivSource;
    @FXML
    public Slider slTileSize;
    @FXML
    private BufferedImage sourceImage;
    private Map<String, AvgColor> tilesCache = new HashMap<>();
    private ThreadingManager threadingManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        threadingManager = new ThreadingManager(20);
    }

    public void loadDb(ActionEvent actionEvent) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        final File selectedDirectory = directoryChooser.showDialog(currStage);
        if (selectedDirectory != null) {
            lblDir.setText(selectedDirectory.getAbsolutePath());
            new Service<Void>() {
                @Override
                protected Task<Void> createTask() {
                    return new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            try {
                                Stream<Path> files = Files.walk(Paths.get(selectedDirectory.toURI()));
                                final int[] c = {0};
                                java.util.List<File> pics = new ArrayList<>();
                                files.filter(p -> p.toFile().isFile()).forEach(path -> pics.add(path.toFile()));
                                pics.parallelStream().forEach(f -> threadingManager.execute(() -> {
                                    try {
                                        BufferedImage bimg = ImageIO.read(f);
                                        synchronized (tilesCache) {
                                            tilesCache.put(f.getAbsolutePath(), averageColor(bimg));
                                        }
                                        Platform.runLater(() -> {
                                            lvPics.getItems().add(f.getAbsolutePath());
                                            c[0]++;
                                            lblStatus.setText("Обработано " + c[0] + " картинок");

                                        });
                                    } catch (Exception e) {
                                        System.out.println("Unable to process file " + f.getAbsolutePath());
                                        e.printStackTrace();
                                    }
                                }));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    };
                }
            }.start();

        }
    }

    private AvgColor averageColor(BufferedImage image) {
        AvgColor avgColor = new AvgColor();
        Image img = SwingFXUtils.toFXImage(image, null);
        int width = (int) img.getWidth();
        int height = (int) img.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = img.getPixelReader().getColor(x, y);
                avgColor.r += new Float(color.getRed());
                avgColor.g += new Float(color.getGreen());
                avgColor.b += new Float(color.getBlue());
            }
        }

        long totalPixels = image.getWidth() * image.getHeight();
        avgColor.r /= totalPixels;
        avgColor.g /= totalPixels;
        avgColor.b /= totalPixels;
        return avgColor;
    }

    public void saveResult(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image");
        File file = fileChooser.showSaveDialog(currStage);
        if (file != null) {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(ivResult.getImage(), null), "png", file);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void openSrc(ActionEvent actionEvent) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open source file");
        //Set extension filter
        FileChooser.ExtensionFilter extFilterJPG = new FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.jpg");
        FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.png");
        fileChooser.getExtensionFilters().addAll(extFilterJPG, extFilterPNG);
        File src = fileChooser.showOpenDialog(currStage);
        if (src != null) {
            sourceImage = ImageIO.read(src);
            Image image = SwingFXUtils.toFXImage(sourceImage, null);
            ivSource.setImage(image);
        }
    }

    public static BufferedImage resize(BufferedImage img, int newW, int newH) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage dimg = new BufferedImage(newW, newH, img.getType());
        Graphics2D g = dimg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, newW, newH, 0, 0, w, h, null);
        g.dispose();
        return dimg;
    }

    // возвращает Евклидово расстояние между двумя точками
    private Float distance(AvgColor p1, AvgColor p2) {
        return new Float(Math.sqrt(sq(p2.r - p1.r) + sq(p2.g - p1.g) + sq(p2.b - p1.b)));
    }

    // возвращает квадрат
    private Float sq(Float n) {
        return n * n;
    }

    private String nearest(AvgColor target, Map<String, AvgColor> tiles) {
        String fileName = null;
        Float smallest = 1000000000.0f;
        for (String fn : tiles.keySet()) {
            float dist = distance(tiles.get(fn), target);
            if (dist < smallest) {
                fileName = fn;
                smallest = dist;
            }
        }
//        if (fileName != null)
//            tiles.remove(fileName);
        return fileName;
    }

    public void start(ActionEvent actionEvent) throws IOException {
        System.gc();
        System.out.println("awd");
        Map<String, AvgColor> tiles = new HashMap<>();
        tilesCache.forEach((fn, ac) -> tiles.put(fn, ac.cl()));
        BufferedImage timg = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), sourceImage.getType());

        Image img = SwingFXUtils.toFXImage(sourceImage, null);
        int tileSize = (int) slTileSize.getValue();
        for (int y = 0; y < img.getHeight(); y += tileSize) {
            for (int x = 0; x < img.getWidth(); x += tileSize) {
                Color color = img.getPixelReader().getColor(x, y);
                AvgColor avgColor = new AvgColor(color.getRed(), color.getGreen(), color.getBlue(), 0);
                String near = nearest(avgColor, tiles);
//                System.out.println("Nearest: " + near);
                final int finalX = x;
                final int finalY = y;
                threadingManager.execute(() -> {
                    if (near != null) {
                        BufferedImage newTile = null;
                        try {
                            newTile = resize(ImageIO.read(new File(near)), tileSize, tileSize);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        timg.getGraphics().drawImage(newTile, finalX, finalY, (img1, infoflags, x1, y1, width, height) -> true);
                        Platform.runLater(() -> ivResult.setImage(SwingFXUtils.toFXImage(timg, null)));
                    }
                });
            }
        }
    }
}
