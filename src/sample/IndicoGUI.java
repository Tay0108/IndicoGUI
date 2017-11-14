package sample;

import io.indico.Indico;
import io.indico.api.results.BatchIndicoResult;
import io.indico.api.results.IndicoResult;
import io.indico.api.utils.IndicoException;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class IndicoGUI extends Application {

    final private DirectoryChooser directoryChooser = new DirectoryChooser();
    final private String[] EXTENSIONS = new String[]{"png", "jpg", "gif", "jpeg", "bmp"};

    private File directory;
    private Stage stage;
    //private HashMap<String, HashMap<String, Double>> adjustments = new HashMap<String, HashMap<String, Double>>();
    private Indico indico = null; // access to API
    private ObservableList<String> photosList = FXCollections.observableArrayList();
    private ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();

    private Map<String, String> namePath = new HashMap<>();
    private Map<String, Object> indicoParams = new HashMap<>();

    @FXML
    private TextField adjustmentsCount;

    @FXML
    private TextField adjustmentThreshold; // percents

    @FXML
    private ImageView imageDisplay = new ImageView();

    @FXML
    private ListView<String> photosListView;

    @FXML
    private PieChart imageChart;

    @FXML
    private Label imageLabel;

    @FXML
    private void browse() {
        directory = directoryChooser.showDialog(stage);
        photosList.clear(); // clearing list
        if (directory != null && directory.listFiles() != null) {
            for (File image : directory.listFiles()) { // TODO
                for (final String extension : EXTENSIONS) {
                    if (image.getName().endsWith("." + extension)) {
                        photosList.add(image.getName());
                        namePath.put(image.getName(), image.getAbsolutePath());
                        //adjustments.put(image.getAbsolutePath(), null);
                    }
                }
            }
        }
        photosListView.setItems(photosList);
    }

    @FXML
    private void group() throws NoImagesException, CannotCreateDirectoryException, FileNotMovedException {
        try {
            if (indico == null) {
                indico = new Indico("f1cd06f9d3b9857bbecb831f7e4f9592");
            }

            File[] photosFiles = directory.listFiles();

            if (photosFiles == null) {
                throw new FileNotFoundException();
            }

            List<String> photosPatches = new ArrayList<String>();
            Boolean noImages = true;

            for (File photosFile : photosFiles) { // TODO
                if (!photosFile.isDirectory()) {
                    noImages = false;
                    photosPatches.add(photosFile.getPath());
                }
            }

            if (noImages) {
                throw new NoImagesException();
            }

            List<Map<String, Double>> results;
            BatchIndicoResult multiple = indico.imageRecognition.predict(photosPatches);
            results = multiple.getImageRecognition();

            String maxKey;
            Double maxValue;

            File destinationFolder;
            File sourceFile;
            File destinationFile;

            for (int i = 0; i < results.size(); i++) {
                maxKey = "";
                maxValue = 0.0;
                for (Map.Entry<String, Double> iteratorMap : results.get(i).entrySet()) {

                    if (iteratorMap.getValue() > maxValue) {
                        maxKey = iteratorMap.getKey();
                        maxValue = iteratorMap.getValue();
                    }
                }
                sourceFile = new File(photosPatches.get(i));
                destinationFolder = new File(directory.getPath() + "/" + maxKey + "/");

                if (!destinationFolder.exists()) {
                    if (!destinationFolder.mkdir()) {
                        throw new CannotCreateDirectoryException();
                    }
                }
                destinationFile = new File(destinationFolder + "/" + sourceFile.getName());
                if (!sourceFile.renameTo(destinationFile)) {
                    throw new FileNotMovedException();
                }
            }
        } catch (IndicoException e) {
            System.out.println("IndicoException");
        } catch (IOException e) {
            System.out.println("IOException");
        }
    }

    @FXML
    private void clear() {
        // null fields
        directory = null;
        adjustmentsCount.setText("8"); // default value, 8 adjustments
        adjustmentThreshold.setText("0.01"); // default value, 1%
        //adjustments.clear();
        photosList.clear();
        // remove data from chart or mb whole chart
        chartData.clear();
        imageChart.setData(null);
        // clear displayed image
        imageDisplay.setImage(null);
        // clear list of images
        photosListView.setItems(null);
    }

    @FXML
    protected void displayData() throws IndicoException, IOException, NullPointerException {
        String currentElement = photosListView.getSelectionModel().getSelectedItem();
        imageLabel.setText(currentElement);
        currentElement = namePath.get(currentElement); // getting image path from its name
        displayImage(currentElement);
        // getting data from api:
        if (indico == null) {
            indico = new Indico("f1cd06f9d3b9857bbecb831f7e4f9592");
        }
        indicoParams.put("top_n", Integer.parseInt(adjustmentsCount.getText()));
        indicoParams.put("threshold", Double.parseDouble(adjustmentThreshold.getText()));

        IndicoResult result = indico.imageRecognition.predict(currentElement, indicoParams);
        Map<String, Double> singleAdjustment = result.getImageRecognition();
        // display chart:
        displayChart(singleAdjustment);
    }

    @FXML
    private void displayImage(String currentElement) {
        Image toDisplay = new Image("file:" + currentElement);
        imageDisplay.setImage(toDisplay);
    }

    private void displayChart(Map<String, Double> data) {
        // display map as a chart
        imageChart.setTitle("Adjustments");
        imageChart.setLabelsVisible(false);
        chartData.clear();
        Double others = 0.0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            chartData.add(new PieChart.Data(entry.getKey() + " " + (Math.round(entry.getValue() * 100) + "%"), entry.getValue()));
            others += entry.getValue();
        }
        others = 1.0 - others;

        chartData.add(new PieChart.Data("Other " + Math.round(others * 100) + "%", others));
        imageChart.setData(chartData);
    }

    @FXML
    public void initialize() {
        adjustmentsCount.setText("8");
        adjustmentThreshold.setText("0.01");
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        /* config */
        stage = primaryStage;
        stage.setResizable(false); // preventing window resizing
        Parent root = FXMLLoader.load(getClass().getResource("layout.fxml"));
        root.getStylesheets().add(IndicoGUI.class.getResource("main.css").toExternalForm());
        stage.setTitle("IndicoGUI");
        stage.setScene(new Scene(root, 1000, 600));
        stage.getIcons().add(new Image("file:icon.png"));
        /* eof config */

        /* browse */
        directoryChooser.setTitle("Select directory containing images");
        directoryChooser.setInitialDirectory(new File("./"));
        /* eof browse */

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
