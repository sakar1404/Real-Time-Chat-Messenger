package controller;

import model.Status;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import javafx.scene.control.*;
import javafx.collections.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyEvent;
import model.Client;
import model.client.*;

/**
 * Controller for the Login screen.
 *
 * @author Member(1-2-3-4)
 */
public class ClientController implements Initializable {

    @FXML
    private TableView tvFriends;
    @FXML
    private TableView tvUsers;
    @FXML
    private TableColumn<ClientUser, String> columnUsername;
    @FXML
    private TableColumn<ClientUser, Status> columnUsernameStatus;
    @FXML
    private TableColumn<Conversation, String> columnFriends;
    @FXML
    private Label labelLeftStatus;
    @FXML
    private Label labelTalkingWIth;
    @FXML
    private TextArea txtAreaMessages;
    @FXML
    private TextArea txtAreaNewMessage;
    @FXML
    private TextField textFieldSearch;
    @FXML
    private SplitPane split;
    @FXML
    private ComboBox comboBoxStatus;

    private final ObservableList<Conversation> friendList = FXCollections.observableList(new ArrayList<>());
    private final ObservableList<ClientUser> userList = FXCollections.observableList(new ArrayList<>());

    private Client client;
    private Conversation activeConversation;
    private boolean removing = false;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTabel();
        initComboBox();
    }

    private void initTabel() {
        columnUsername.setCellValueFactory((TableColumn.CellDataFeatures<ClientUser, String> param)
                -> new SimpleObjectProperty<>(param.getValue().getUserName()));
        columnFriends.setCellValueFactory((TableColumn.CellDataFeatures<Conversation, String> param)
                -> new SimpleObjectProperty<>(param.getValue().getTalkingWithUsername()));

        columnUsernameStatus.setCellValueFactory((TableColumn.CellDataFeatures<ClientUser, Status> param)
                -> new SimpleObjectProperty<>(param.getValue().getStatus()));
        columnUsernameStatus.setComparator((o1, o2) -> {
            //If they are the same, return 0
            if (o1 == o2) {
                return 0;
            }
            //o1 should be placed at top if online
            if (o1 == Status.ONLINE) {
                return 1;
            }
            //o1 should be placed at bottom if offline
            if (o1 == Status.OFFLINE) {
                return -1;
            }

            //We now know that o1 is busy. o2 is either online or offline
            //if o2 is offline, o1 should be placed on top
            return o2 == Status.OFFLINE ? 1 : -1;

        });
        tvFriends.setItems(friendList);

        tvFriends.getSelectionModel().getSelectedItems().addListener((ListChangeListener.Change c) -> {
            if (!removing) {
                int idx = tvFriends.getSelectionModel().getFocusedIndex();
                Conversation conv = friendList.get(idx);
                setActiveConversation(conv);
            }
        });

        tvUsers.setOnMouseClicked(e -> sendRequest());

        FilteredList<ClientUser> filteredList = new FilteredList<>(userList, clientUser -> true);
        textFieldSearch.textProperty().addListener((obs, old, newValue) -> {
            filteredList.setPredicate(clientUser -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                return clientUser.getUserName().toLowerCase().contains(newValue.toLowerCase());
            });
        });
        SortedList<ClientUser> sortList = new SortedList<>(filteredList);
        sortList.comparatorProperty().bind(tvUsers.comparatorProperty());
        tvUsers.setItems(sortList);

        tvFriends.prefWidthProperty().bind(split.widthProperty());
        tvUsers.prefWidthProperty().bind(split.widthProperty());
    }

    private void initComboBox() {
        comboBoxStatus.getItems().add(Status.ONLINE);
        comboBoxStatus.getItems().add(Status.BUSY);
        comboBoxStatus.getSelectionModel().select(Status.ONLINE);
        System.setProperty("glass.accessible.force", "false");
    }

    @FXML
    private void changeStatus() {
        try {
            client.sendStatusUpdate((Status) comboBoxStatus.getSelectionModel().getSelectedItem());
        } catch (IOException ex) {
            showAlertIOException(ex);
        }
    }

    private void sendRequest() {
        if (!removing) {
            int idx = tvUsers.getSelectionModel().getFocusedIndex();
            if (idx == -1) {
                return;
            }
            ClientUser user = userList.get(idx);
            if (user.getStatus() == Status.ONLINE) {
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("Confirm Connection");
                alert.setHeaderText("Do you want to connect with " + user.getUserName());
                alert.setContentText("We will alert you when your peer "
                        + "has responded to the request.");

                Optional<ButtonType> answer = alert.showAndWait();
                if (answer.isPresent() && answer.get() == ButtonType.OK) {
                    try {
                        client.connectChat(user.getUserName());
                    } catch (IOException ex) {
                        showAlertIOException(ex);
                    }
                }
            } else {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Connection Request");
                alert.setHeaderText("User " + user.getUserName() + " is " + user.getStatus().toString().toLowerCase() + ".");
                alert.setContentText("This person is not available at the moment.");
                alert.show();
            }
        }
        tvUsers.getSelectionModel().clearSelection();
    }

    private void showAlertIOException(IOException ex) {
        Alert alertErr = new Alert(AlertType.ERROR);
        alertErr.setTitle("Error occurred");
        alertErr.setHeaderText("An IOException occurred");

        TextArea txtArea = new TextArea(ex.toString());
        alertErr.getDialogPane().setExpandableContent(txtArea);
    }

    private void showAlertBoxError(String title, String contentText) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(contentText);
        alert.showAndWait();
    }

    /**
     * Stores message string in conversation.
     *
     * @param userName senders name
     * @param msg      Message
     * @see model.client.Conversation
     */
    public void addMessageToConversation(String userName, Message msg) {
        for (Conversation cnv : friendList) {
            if (cnv.getTalkingWithUsername().equals(userName)) {
                cnv.addMessage(msg);
                if (activeConversation == cnv) {
                    appendMsgToConversation();
                }
                return;
            }
        }

        showAlertBoxError("Error occurred",
                "This should not happen. Server should have control over this.");
    }

    /**
     * Clears the list of user and inserts all registered users.
     *
     * @param restOfArray String list of users separated with ;. Each user must have two fields, the first with uname the second status.
     */
    public void updateUserList(String restOfArray) {
        if (restOfArray.equals("")) {
            return;
        }
        userList.clear();
        String[] users = restOfArray.split(";");
        for (int i = 0; i < users.length; i += 2) {
            userList.add(new ClientUser(users[i], users[i + 1]));
        }
    }

    /**
     * Shows alert box, with input error.
     *
     * @param restOfArray Error message.
     */
    public void showError(String restOfArray) {
        showAlertBoxError("Server error", restOfArray);
    }

    /**
     * Updates user status to offline, busy or online in user and fiend list.
     *
     * @param username Is the username to change the status of.
     * @param status   THe user is changed to this status.
     */
    public void updateStatus(String username, String status) {
        ClientUser user = new ClientUser(username, status);
        Conversation con = new Conversation(user);
        Status newStatus;

        switch (status) {
            case "0":
                newStatus = Status.OFFLINE;
                break;
            case "-":
                newStatus = Status.BUSY;
                break;
            case "+":
                newStatus = Status.ONLINE;
                break;
            default:
                showError("Your client may be outdated.");
                return;
        }

        if (userList.contains(user)) {
            userList.get(userList.indexOf(user)).setStatus(newStatus);
        } else if (friendList.contains(con)) {
            if (newStatus == Status.OFFLINE) {
                moveFromFriendsToUser(username, true);
                userList.get(userList.size() - 1).setStatus(newStatus);
            } else {
                friendList.get(friendList.indexOf(con)).getClientUser().setStatus(newStatus);
            }
        } else {
            userList.add(user);
        }
        tvUsers.refresh();
        tvFriends.refresh();
    }

    /**
     * Reacts to an request from another client.
     *
     * @param username Username of the user that wants to conect.
     */
    public void connectRequest(String username) {

        ButtonType accept = new ButtonType("Accept", ButtonBar.ButtonData.OK_DONE);
        ButtonType reject = new ButtonType("Reject", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(AlertType.CONFIRMATION, "", accept, reject);
        alert.setTitle("Confirm Connection");
        alert.setHeaderText(username + " wants to connect to you.");

        Optional<ButtonType> answer = alert.showAndWait();
        if (answer.isPresent()) {
            try {
                String respons = answer.get() == accept ? "YES" : "NO";
                client.sendRespons(username, respons);
                if (respons.equals("YES")) {
                    moveFromUsersToFriends(username, false);
                }
            } catch (IOException ex) {
                showAlertIOException(ex);
            }
        }
    }

    /**
     * Moves user from user list to conversation list
     *
     * @param username  Username to move.
     * @param showAlert If the user should get an alert or not.
     */
    public void moveFromUsersToFriends(String username, boolean showAlert) {
        removing = true;
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getUserName().equals(username)) {
                friendList.add(new Conversation(userList.get(i)));
                userList.remove(i);

                break;
            }
        }
        if (friendList.size() > 0) {
            setActiveConversation(friendList.get(friendList.size() - 1));
        } else {
            setActiveConversation(null);
        }
        removing = false;

        if (showAlert) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Accepted");
            alert.setContentText(username + " is added to your conversation list. You can now chat.");
            alert.show();
        }
    }

    /**
     * Moves user from conversation list to user list
     *
     * @param username  Username to move.
     * @param showAlert If the user should get an alert or not.
     */
    public void moveFromFriendsToUser(String username, boolean showAlert) {
        removing = true;
        for (int i = 0; i < friendList.size(); i++) {
            if (friendList.get(i).getTalkingWithUsername().equals(username)) {
                userList.add(friendList.remove(i).getClientUser());
                break;
            }
        }
        if (friendList.size() > 0) {
            setActiveConversation(friendList.get(0));
        } else {
            setActiveConversation(null);
        }
        removing = false;
        if (showAlert) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Disconnected");
            alert.setContentText(username + " disconnected, and has been moved to the user list.");
            alert.show();
        }

    }

    /**
     * Displays alert box if a user rejects clients connection request
     *
     * @param username Username of the user that rejected the raquest.
     */
    public void negativeResponse(String username) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Rejected");
        alert.setContentText(username + " doesn't want to talk with you...");

        alert.show();
    }

    /**
     * Sets client to be stored in this object.
     *
     * @param client the client to store in the Client field.
     * @see Client
     */
    void setClient(Client client) {
        this.client = client;
        try {
            client.getUserList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActiveConversation(Conversation conv) {
        if (conv != null) {
            activeConversation = conv;
            labelTalkingWIth.setText("Talking with: " + conv.getTalkingWithUsername());
            appendMsgToConversation();
        } else {
            activeConversation = null;
            labelTalkingWIth.setText("Talking with:");
            txtAreaMessages.clear();

        }
    }

    private void appendMsgToConversation() {

        txtAreaMessages.clear();
        for (Message msg : activeConversation.getMessages()) {
            txtAreaMessages.appendText(msg.toString().replace("&#92", "\n").replace("&#59", ";"));
            txtAreaMessages.appendText("\n");
        }
    }

    /**
     * Writes text to GUI label, located to the left.
     * Writes your username.
     *
     * @param username The username bound to this client.
     */
    public void setYourUnameLabel(String username) {
        labelLeftStatus.setText("Your username: " + username);
    }

    @FXML
    private void handleSendMsg() {
        try {
            if (activeConversation != null) {
                String msg = txtAreaNewMessage.getText();
                activeConversation.addMessage(new Message("Me", msg));
                appendMsgToConversation();
                client.sendMsg(activeConversation.getTalkingWithUsername(), msg.replace("\n", "&#92").replace(";", "&#59"));
                txtAreaNewMessage.clear();
            }
        } catch (IOException ex) {
            showAlertIOException(ex);
        }
    }

    @FXML
    private void handleDisconnectFromUser() {
        try {
            if (activeConversation != null) {
                String username = activeConversation.getTalkingWithUsername();
                moveFromFriendsToUser(username, false);
                client.disconnectChat(username);
            }
        } catch (IOException ex) {
            showAlertIOException(ex);
        }
    }

    @FXML
    private void buttonPressed(KeyEvent e) {
        if (e.isShiftDown() && e.getCode().toString().equals("ENTER")) {
            txtAreaNewMessage.appendText("\n");
        } else if (e.getCode().toString().equals("ENTER")) {
            handleSendMsg();
            txtAreaNewMessage.clear();
            e.consume();
        }
    }
}
