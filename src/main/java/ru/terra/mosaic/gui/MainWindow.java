package ru.terra.mosaic.gui;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import lombok.SneakyThrows;
import lombok.val;
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static javafx.application.Platform.runLater;
import static javafx.embed.swing.SwingFXUtils.fromFXImage;
import static javafx.embed.swing.SwingFXUtils.toFXImage;
import static javax.imageio.ImageIO.read;
import static javax.imageio.ImageIO.write;

/**
 * Date: 21.07.15
 * Time: 16:26
 */
public class MainWindow extends AbstractWindow {
    private static final Integer APP_ID = 5005860;
    private static final String CLIENT_SECRET = "";
    private static final String REDIRECT_URI = "";
    @FXML
    public ImageView ivResult;
    @FXML
    public Label lblDir;
    @FXML
    public ListView<String> lvPics;
    @FXML
    public Label lblStatus;
    @FXML
    public ImageView ivSource;
    @FXML
    public Slider slTileSize;
    @FXML
    private BufferedImage sourceImage;
    private final Map<String, AvgColor> tilesCache = new HashMap<>();
    private final ThreadingManager threadingManager = new ThreadingManager(20);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void loadDb(ActionEvent actionEvent) {
        val directoryChooser = new DirectoryChooser();
        val selectedDirectory = directoryChooser.showDialog(currStage);
        if (selectedDirectory != null) {
            lblDir.setText(selectedDirectory.getAbsolutePath());
            new Service<Void>() {
                @Override
                protected Task<Void> createTask() {
                    return new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            try {
                                val files = Files.walk(Paths.get(selectedDirectory.toURI()));
                                final int[] c = {0};
                                java.util.List<File> pics = new ArrayList<>();
                                files.filter(p -> p.toFile().isFile()).forEach(path -> pics.add(path.toFile()));
                                pics.parallelStream().forEach(f -> threadingManager.execute(() -> {
                                    try {
                                        val bimg = read(f);
                                        synchronized (tilesCache) {
                                            tilesCache.put(f.getAbsolutePath(), averageColor(bimg));
                                        }
                                        runLater(() -> {
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

    private AvgColor averageColor(final BufferedImage image) {
        val avgColor = new AvgColor();
        val img = toFXImage(image, null);
        val width = (int) img.getWidth();
        val height = (int) img.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                val color = img.getPixelReader().getColor(x, y);
                avgColor.r += new Float(color.getRed());
                avgColor.g += new Float(color.getGreen());
                avgColor.b += new Float(color.getBlue());
            }
        }

        val totalPixels = image.getWidth() * image.getHeight();
        avgColor.r /= totalPixels;
        avgColor.g /= totalPixels;
        avgColor.b /= totalPixels;
        return avgColor;
    }

    public void saveResult(ActionEvent actionEvent) {
        val fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image");
        val file = fileChooser.showSaveDialog(currStage);
        if (file != null) {
            try {
                write(fromFXImage(ivResult.getImage(), null), "png", file);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void openSrc(ActionEvent actionEvent) throws IOException {
        val fileChooser = new FileChooser();
        fileChooser.setTitle("Open source file");
        //Set extension filter
        val extFilterJPG = new ExtensionFilter("JPG files (*.jpg)", "*.jpg");
        val extFilterPNG = new ExtensionFilter("PNG files (*.png)", "*.png");
        fileChooser.getExtensionFilters().addAll(extFilterJPG, extFilterPNG);
        val src = fileChooser.showOpenDialog(currStage);
        if (src != null) {
            sourceImage = ImageIO.read(src);
            WritableImage image = toFXImage(sourceImage, null);
            ivSource.setImage(image);
        }
    }

    public static BufferedImage resize(BufferedImage img, int newW, int newH) {
        val w = img.getWidth();
        val h = img.getHeight();
        val dimg = new BufferedImage(newW, newH, img.getType());
        val g = dimg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, newW, newH, 0, 0, w, h, null);
        g.dispose();
        return dimg;
    }

    // возвращает Евклидово расстояние между двумя точками
    private Float distance(AvgColor p1, AvgColor p2) {
        return (float) Math.sqrt(sq(p2.r - p1.r) + sq(p2.g - p1.g) + sq(p2.b - p1.b));
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
        val timg = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), sourceImage.getType());

        val img = toFXImage(sourceImage, null);
        val tileSize = (int) slTileSize.getValue();
        for (int y = 0; y < img.getHeight(); y += tileSize) {
            for (int x = 0; x < img.getWidth(); x += tileSize) {
                val color = img.getPixelReader().getColor(x, y);
                val avgColor = new AvgColor((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue(), (float) 0);
                val near = nearest(avgColor, tiles);
//                System.out.println("Nearest: " + near);
                val finalX = x;
                val finalY = y;
                threadingManager.execute(() -> {
                    if (near != null) {
                        BufferedImage newTile = null;
                        try {
                            newTile = resize(read(new File(near)), tileSize, tileSize);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        timg.getGraphics().drawImage(newTile, finalX, finalY, (img1, infoflags, x1, y1, width, height) -> true);
                        runLater(() -> ivResult.setImage(toFXImage(timg, null)));
                    }
                });
            }
        }
    }

    @SneakyThrows
    public void loginVK(final ActionEvent actionEvent) {
        val transportClient = new HttpTransportClient();
        val vk = new VkApiClient(transportClient);
        final String code = "";
        val authResponse = vk.oAuth()
                .userAuthorizationCodeFlow(APP_ID, CLIENT_SECRET, REDIRECT_URI, code)
                .execute();

        val actor = new UserActor((long) authResponse.getUserId(), authResponse.getAccessToken());

    }
}
