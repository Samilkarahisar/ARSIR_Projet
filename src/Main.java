import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class Main extends Application {

    private TextField serverIp;
    private TextField serverPort;
    private TextField filePathSend;
    private TextField fileNameReceive;
    private TextField directoryReceive;

    public static void main(String[] args) {
        // launch javafx
        launch(args);
    }

    // build the window
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Serveur MultiPort TFTP");
        Group root = new Group();
        Scene scene = new Scene(root, 400, 250, Color.WHITE);

        // server
        VBox serverPane = new VBox();
        serverPane.getChildren().add(new Label("Port du serveur :"));
        serverPort = new TextField ();
        serverPane.getChildren().add(serverPort);

        // create the tabs
        TabPane tabPane = new TabPane();

        String PathName;

        // receive file tab
        Tab tabReceive = new Tab();
        tabReceive.setText("Choix du répertoire de Téléchargement");
        VBox receiveFileBox = new VBox();

        // directory
        receiveFileBox.getChildren().add(new Label("Répertoire de téléchargement : "));
        HBox selectDirectory = new HBox();
        directoryReceive = new TextField();
        selectDirectory.getChildren().add(directoryReceive);
        Button btnSelectDir = new Button("Selectionner");
        btnSelectDir.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                DirectoryChooser chooser = new DirectoryChooser();
                File selectedDirectory = chooser.showDialog(primaryStage);

                if (selectedDirectory != null)
                    directoryReceive.setText(selectedDirectory.getAbsolutePath());
            }
        });
        selectDirectory.getChildren().add(btnSelectDir);
        receiveFileBox.getChildren().add(selectDirectory);
        tabReceive.setContent(receiveFileBox);
        // receive
        Button receive = new Button("Démarrer le serveur");
        receive.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {

                try {

                    server.TFTPServer server= new server.TFTPServer(Integer.parseInt(serverPort.getText()),directoryReceive.getText());
                    server.start();

                }catch (SocketException e) {
                    e.printStackTrace();
                }
                catch (NumberFormatException e) {
                    final Stage dialog = new Stage();
                    dialog.initModality(Modality.APPLICATION_MODAL);
                    dialog.initOwner(primaryStage);
                    VBox dialogVbox = new VBox(20);
                    dialogVbox.getChildren().add(new Label("Numéro de port invalide"));
                    dialogVbox.setAlignment(Pos.CENTER);
                    Scene dialogScene = new Scene(dialogVbox, 250, 50);
                    dialog.setScene(dialogScene);
                    dialog.show();
                }
            }
        });
        receiveFileBox.getChildren().add(receive);
        tabReceive.setContent(receiveFileBox);
        tabPane.getTabs().add(tabReceive);


        BorderPane borderPane = new BorderPane();
        borderPane.prefHeightProperty().bind(scene.heightProperty());
        borderPane.prefWidthProperty().bind(scene.widthProperty());

        borderPane.setTop(serverPane);
        borderPane.setCenter(tabPane);
        root.getChildren().add(borderPane);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
