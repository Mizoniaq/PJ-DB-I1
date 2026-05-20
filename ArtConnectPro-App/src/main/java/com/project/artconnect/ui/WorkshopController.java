package com.project.artconnect.ui;

import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.CommunityService;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDateTime;
import java.util.List;

public class WorkshopController {
    @FXML private TableView<Workshop> workshopTable;
    @FXML private TableColumn<Workshop, String> titleColumn;
    @FXML private TableColumn<Workshop, LocalDateTime> dateColumn;
    @FXML private TableColumn<Workshop, String> instructorColumn;
    @FXML private TableColumn<Workshop, Double> priceColumn;
    @FXML private TableColumn<Workshop, String> levelColumn;
    @FXML private TableColumn<Workshop, String> spotsColumn;
    @FXML private Label bookingStatusLabel;

    private final WorkshopService workshopService = ServiceProvider.getWorkshopService();
    private final CommunityService communityService = ServiceProvider.getCommunityService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        levelColumn.setCellValueFactory(new PropertyValueFactory<>("level"));
        instructorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getInstructor() != null
                        ? cellData.getValue().getInstructor().getName() : "Unknown"));
        spotsColumn.setCellValueFactory(cellData -> {
            Workshop w = cellData.getValue();
            int spots = w.getAvailableSpots();
            String label = spots <= 0 ? "FULL" : spots + " / " + w.getMaxParticipants();
            return new SimpleStringProperty(label);
        });

        refreshTable();
    }

    public void refreshTable() {
        workshopTable.setItems(FXCollections.observableArrayList(workshopService.getAllWorkshops()));
    }

    private CommunityMember getFirstMember() {
        List<CommunityMember> members = communityService.getAllMembers();
        return members.isEmpty() ? null : members.get(0);
    }

    @FXML
    private void handleBook() {
        Workshop selected = workshopTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            bookingStatusLabel.setStyle("-fx-text-fill: red;");
            bookingStatusLabel.setText("Please select a workshop first.");
            return;
        }

        CommunityMember member = getFirstMember();
        if (member == null) {
            bookingStatusLabel.setStyle("-fx-text-fill: red;");
            bookingStatusLabel.setText("No community members found.");
            return;
        }
        try {
            workshopService.bookWorkshop(selected, member);
            refreshTable();
            bookingStatusLabel.setStyle("-fx-text-fill: green;");
            bookingStatusLabel.setText("Booked \"" + selected.getTitle() + "\" for " + member.getName() + ".");
        } catch (Exception e) {
            bookingStatusLabel.setStyle("-fx-text-fill: red;");
            bookingStatusLabel.setText("Booking failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        Workshop selected = workshopTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            bookingStatusLabel.setStyle("-fx-text-fill: red;");
            bookingStatusLabel.setText("Please select a workshop first.");
            return;
        }
        CommunityMember member = getFirstMember();
        if (member == null) {
            bookingStatusLabel.setStyle("-fx-text-fill: red;");
            bookingStatusLabel.setText("No community members found.");
            return;
        }
        try {
            workshopService.cancelBooking(selected, member);
            refreshTable();
            bookingStatusLabel.setStyle("-fx-text-fill: green;");
            bookingStatusLabel.setText("Booking cancelled for \"" + selected.getTitle() + "\" (" + member.getName() + ").");
        } catch (Exception e) {
            bookingStatusLabel.setStyle("-fx-text-fill: red;");
            bookingStatusLabel.setText("Cancel failed: " + e.getMessage());
        }
    }
}
