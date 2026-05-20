package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import java.util.Optional;

public class ArtistController {
    @FXML private TextField searchField;
    @FXML private ComboBox<Discipline> disciplineFilter;
    @FXML private TableView<Artist> artistTable;
    @FXML private TableColumn<Artist, String> nameColumn;
    @FXML private TableColumn<Artist, String> cityColumn;
    @FXML private TableColumn<Artist, String> emailColumn;
    @FXML private TableColumn<Artist, Integer> yearColumn;
    @FXML private Label artistStatusLabel;

    private final ArtistService artistService = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("contactEmail"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("birthYear"));

        disciplineFilter.setItems(FXCollections.observableArrayList(artistService.getAllDisciplines()));
        refreshTable();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        Discipline d = disciplineFilter.getValue();
        String dName = (d != null) ? d.getName() : null;
        artistTable.setItems(FXCollections.observableArrayList(artistService.searchArtists(query, dName, null)));
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        disciplineFilter.setValue(null);
        refreshTable();
    }

    @FXML
    private void handleAdd() {
        Dialog<Artist> dialog = new Dialog<>();
        dialog.setTitle("Add Artist");
        dialog.setHeaderText("Enter artist details");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField  = new TextField(); nameField.setPromptText("Name *");
        TextField emailField = new TextField(); emailField.setPromptText("Email *");
        TextField cityField  = new TextField(); cityField.setPromptText("City");
        TextField yearField  = new TextField(); yearField.setPromptText("Birth Year");
        ComboBox<Discipline> disciplineBox = new ComboBox<>(
                FXCollections.observableArrayList(artistService.getAllDisciplines()));
        disciplineBox.setPromptText("Discipline (optional)");

        grid.add(new Label("Name:"),       0, 0); grid.add(nameField,     1, 0);
        grid.add(new Label("Email:"),      0, 1); grid.add(emailField,    1, 1);
        grid.add(new Label("City:"),       0, 2); grid.add(cityField,     1, 2);
        grid.add(new Label("Birth Year:"), 0, 3); grid.add(yearField,     1, 3);
        grid.add(new Label("Discipline:"), 0, 4); grid.add(disciplineBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == addButtonType) {
                String name  = nameField.getText().trim();
                String email = emailField.getText().trim();
                if (name.isEmpty() || email.isEmpty()) return null;
                Artist a = new Artist();
                a.setName(name);
                a.setContactEmail(email);
                a.setCity(cityField.getText().trim());
                a.setActive(true);
                try {
                    a.setBirthYear(Integer.parseInt(yearField.getText().trim()));
                } catch (NumberFormatException ignored) {}
                if (disciplineBox.getValue() != null)
                    a.getDisciplines().add(disciplineBox.getValue());
                return a;
            }
            return null;
        });

        Optional<Artist> result = dialog.showAndWait();
        result.ifPresent(artist -> {
            try {
                artistService.createArtist(artist);
                refreshTable();
                artistStatusLabel.setStyle("-fx-text-fill: green;");
                artistStatusLabel.setText("Artist \"" + artist.getName() + "\" added successfully.");
            } catch (Exception e) {
                artistStatusLabel.setStyle("-fx-text-fill: red;");
                artistStatusLabel.setText("Error: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleEdit() {
        Artist selected = artistTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            artistStatusLabel.setStyle("-fx-text-fill: red;");
            artistStatusLabel.setText("Please select an artist first.");
            return;
        }

        Dialog<Artist> dialog = new Dialog<>();
        dialog.setTitle("Edit Artist");
        dialog.setHeaderText("Editing: " + selected.getName());

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField  = new TextField(selected.getName());
        TextField emailField = new TextField(selected.getContactEmail() != null ? selected.getContactEmail() : "");
        TextField cityField  = new TextField(selected.getCity() != null ? selected.getCity() : "");
        TextField yearField  = new TextField(selected.getBirthYear() != null ? String.valueOf(selected.getBirthYear()) : "");

        grid.add(new Label("Name:"),       0, 0); grid.add(nameField,  1, 0);
        grid.add(new Label("Email:"),      0, 1); grid.add(emailField, 1, 1);
        grid.add(new Label("City:"),       0, 2); grid.add(cityField,  1, 2);
        grid.add(new Label("Birth Year:"), 0, 3); grid.add(yearField,  1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                selected.setName(nameField.getText().trim());
                selected.setContactEmail(emailField.getText().trim());
                selected.setCity(cityField.getText().trim());
                try {
                    selected.setBirthYear(Integer.parseInt(yearField.getText().trim()));
                } catch (NumberFormatException ignored) {}
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(artist -> {
            try {
                artistService.updateArtist(artist);
                refreshTable();
                artistStatusLabel.setStyle("-fx-text-fill: green;");
                artistStatusLabel.setText("Artist \"" + artist.getName() + "\" updated.");
            } catch (Exception e) {
                artistStatusLabel.setStyle("-fx-text-fill: red;");
                artistStatusLabel.setText("Error: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleDelete() {
        Artist selected = artistTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            artistStatusLabel.setStyle("-fx-text-fill: red;");
            artistStatusLabel.setText("Please select an artist first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Artist");
        confirm.setHeaderText("Delete \"" + selected.getName() + "\"?");
        confirm.setContentText("This will also delete all their artworks (CASCADE).");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    artistService.deleteArtist(selected.getName());
                    refreshTable();
                    artistStatusLabel.setStyle("-fx-text-fill: green;");
                    artistStatusLabel.setText("Artist \"" + selected.getName() + "\" deleted.");
                } catch (Exception e) {
                    artistStatusLabel.setStyle("-fx-text-fill: red;");
                    artistStatusLabel.setText("Error: " + e.getMessage());
                }
            }
        });
    }

    private void refreshTable() {
        artistTable.setItems(FXCollections.observableArrayList(artistService.getAllArtists()));
    }
}
