module com.car.damageanalyzerbot {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.car.damageanalyzerbot to javafx.fxml;
    exports com.car.damageanalyzerbot;
}