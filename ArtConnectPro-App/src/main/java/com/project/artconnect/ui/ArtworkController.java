package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.ArtworkService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import java.util.List;
import java.util.Optional;

public class ArtworkController {
    @FXML private TableView<Artwork> artworkTable;
    @FXML private TableColumn<Artwork, String> titleColumn;
    @FXML private TableColumn<Artwork, String> typeColumn;
    @FXML private TableColumn<Artwork, Double> priceColumn;
    @FXML private TableColumn<Artwork, String> statusColumn;
    @FXML private TableColumn<Artwork, String> artistColumn;
    @FXML private Label artworkStatusLabel;

    private final ArtworkService artworkService = ServiceProvider.getArtworkService();
    private final ArtistService artistService   = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        artistColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getArtist() != null ? cellData.getValue().getArtist().getName() : "Unknown"));

        refreshTable();
    }

    public void refreshTable() {
        artworkTable.setItems(FXCollections.observableArrayList(artworkService.getAllArtworks()));
    }

    @FXML
    private void handleAdd() {
        List<Artist> artists = artistService.getAllArtists();
        if (artists.isEmpty()) {
            artworkStatusLabel.setStyle("-fx-text-fill: red;");
            artworkStatusLabel.setText("No artists found. Add an artist first.");
            return;
        }

        Dialog<Artwork> dialog = new Dialog<>();
        dialog.setTitle("Add Artwork");
        dialog.setHeaderText("Enter artwork details");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField  = new TextField(); titleField.setPromptText("Title *");
        TextField typeField   = new TextField(); typeField.setPromptText("Type (painting, sculpture...)");
        TextField mediumField = new TextField(); mediumField.setPromptText("Medium (oil, watercolor...)");
        TextField priceField  = new TextField(); priceField.setPromptText("Price *");
        ComboBox<Artist> artistBox = new ComboBox<>(FXCollections.observableArrayList(artists));
        artistBox.setPromptText("Select Artist *");

        grid.add(new Label("Title:"),   0, 0); grid.add(titleField,  1, 0);
        grid.add(new Label("Type:"),    0, 1); grid.add(typeField,   1, 1);
        grid.add(new Label("Medium:"),  0, 2); grid.add(mediumField, 1, 2);
        grid.add(new Label("Price:"),   0, 3); grid.add(priceField,  1, 3);
        grid.add(new Label("Artist:"),  0, 4); grid.add(artistBox,   1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == addButtonType) {
                String title = titleField.getText().trim();
                Artist artist = artistBox.getValue();
                if (title.isEmpty() || artist == null) return null;
                double price = 0.0;
                try { price = Double.parseDouble(priceField.getText().trim()); } catch (NumberFormatException ignored) {}
                Artwork a = new Artwork();
                a.setTitle(title);
                a.setType(typeField.getText().trim());
                a.setMedium(mediumField.getText().trim());
                a.setPrice(price);
                a.setArtist(artist);
                a.setStatus(Artwork.Status.EXHIBITED);
                return a;
            }
            return null;
        });

        Optional<Artwork> result = dialog.showAndWait();
        result.ifPresent(artwork -> {
            try {
                artworkService.createArtwork(artwork);
                artworkTable.setItems(FXCollections.observableArrayList(artworkService.getAllArtworks()));
                artworkStatusLabel.setStyle("-fx-text-fill: green;");
                artworkStatusLabel.setText("Artwork \"" + artwork.getTitle() + "\" added.");
            } catch (Exception e) {
                artworkStatusLabel.setStyle("-fx-text-fill: red;");
                artworkStatusLabel.setText("Error: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleDelete() {
        Artwork selected = artworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            artworkStatusLabel.setStyle("-fx-text-fill: red;");
            artworkStatusLabel.setText("Please select an artwork first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Artwork");
        confirm.setHeaderText("Delete \"" + selected.getTitle() + "\"?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    artworkService.deleteArtwork(selected.getTitle());
                    refreshTable();
                    artworkStatusLabel.setStyle("-fx-text-fill: green;");
                    artworkStatusLabel.setText("Artwork \"" + selected.getTitle() + "\" deleted.");
                } catch (Exception e) {
                    artworkStatusLabel.setStyle("-fx-text-fill: red;");
                    artworkStatusLabel.setText("Error: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleMarkSold() {
        Artwork selected = artworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            artworkStatusLabel.setStyle("-fx-text-fill: red;");
            artworkStatusLabel.setText("Please select an artwork first.");
            return;
        }
        if (selected.getStatus() == Artwork.Status.SOLD) {
            artworkStatusLabel.setStyle("-fx-text-fill: orange;");
            artworkStatusLabel.setText("\"" + selected.getTitle() + "\" is already sold.");
            return;
        }
        try {
            selected.setStatus(Artwork.Status.SOLD);
            artworkService.updateArtwork(selected);
            artworkTable.refresh();
            artworkStatusLabel.setStyle("-fx-text-fill: green;");
            artworkStatusLabel.setText("\"" + selected.getTitle() + "\" marked as SOLD. Audit log updated.");
        } catch (Exception e) {
            artworkStatusLabel.setStyle("-fx-text-fill: red;");
            artworkStatusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleMarkForSale() {
        Artwork selected = artworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            artworkStatusLabel.setStyle("-fx-text-fill: red;");
            artworkStatusLabel.setText("Please select an artwork first.");
            return;
        }
        if (selected.getStatus() == Artwork.Status.FOR_SALE) {
            artworkStatusLabel.setStyle("-fx-text-fill: orange;");
            artworkStatusLabel.setText("\"" + selected.getTitle() + "\" is already for sale.");
            return;
        }
        try {
            selected.setStatus(Artwork.Status.FOR_SALE);
            artworkService.updateArtwork(selected);
            artworkTable.refresh();
            artworkStatusLabel.setStyle("-fx-text-fill: green;");
            artworkStatusLabel.setText("\"" + selected.getTitle() + "\" marked as FOR SALE. Audit log updated.");
        } catch (Exception e) {
            artworkStatusLabel.setStyle("-fx-text-fill: red;");
            artworkStatusLabel.setText("Error: " + e.getMessage());
        }
    }
}
