package controller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Starts the client.
 * @author Member(1-2-3-4)
 */
public class MainClient extends Application {

    @Override
    public void start(Stage stage) throws Exception {
	Parent root = FXMLLoader.load(getClass().getResource("/view/LoginClient.fxml"));

        stage.setTitle("sMessage - Login");
	Scene scene = new Scene(root);

	stage.setScene(scene);
	stage.show();
    }

    /**
     * Launches JavaFX. 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
	launch(args);
    }

}
